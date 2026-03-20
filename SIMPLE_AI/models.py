from datetime import timedelta
import torch
import torch.nn as nn

from ultralytics import YOLO

# =====   ONNX로 변경한 YOLO 파일 =====
import onnxruntime as ort
import numpy as np
import cv2

# LSTM 모델 정의
class LSTMClassifier(nn.Module):
    def __init__(self, input_dim, hidden_dim1, hidden_dim2, dropout_p, num_classes):
        super().__init__()
        self.lstm1 = nn.LSTM(input_dim, hidden_dim1, batch_first=True)
        self.lstm2 = nn.LSTM(hidden_dim1, hidden_dim2, batch_first=True)
        self.dropout = nn.Dropout(dropout_p)
        self.fc1 = nn.Linear(hidden_dim2, 32)
        self.relu = nn.ReLU()
        self.fc2 = nn.Linear(32, num_classes)

    def forward(self, x, pad_mask=None):
        out, _ = self.lstm1(x)
        out, _ = self.lstm2(out)
        if pad_mask is not None:
            seq_lens = (pad_mask == 0).sum(dim=1)   # (B,)
            last_outputs = []
            for i, seq_len in enumerate(seq_lens):
                last_outputs.append(out[i, int(seq_len)-1, :])
            out = torch.stack(last_outputs, dim=0)
        else:
            out = out[:, -1, :]
        out = self.dropout(out)
        out = self.relu(self.fc1(out))
        out = self.dropout(out)
        return self.fc2(out)
    

def load_vision_models(yolo_path, lstm_path, lstm_params):
    """ YOLO와 LSTM 모델을 로드하여 반환하는 함수. """
    print("AI 모델을 호출합니다!")
    
    lstm_model = None
    yolo_model = None
    
    try:
        print("[LSTM 모델 로드 중]")
        # LSTM 모델 생성
        lstm_model = LSTMClassifier(
            input_dim=lstm_params['input_dim'],
            hidden_dim1=lstm_params['hidden_dim1'],
            hidden_dim2=lstm_params['hidden_dim2'],
            dropout_p=lstm_params['dropout_p'],
            num_classes=lstm_params['num_classes']
            )
        
        # 학습된 가중치 로드
        lstm_model.load_state_dict(torch.load(lstm_path))
        lstm_model.eval()
    except Exception as e:
        print(f"[LSTM 로드 실패!] {e}")
    
    try:
        print("[YOLO 모델 로드 중]")
        # YOLO 모델 로드
        yolo_model = YOLO(yolo_path)
    except Exception as e:
        print(f"[YOLO 로드 실패!] {e}")
    
    print("모델 호출 완료!")
    
    # 두 모델을 함께 반환
    return yolo_model, lstm_model


# YOLO ONNX 추론을 위한 함수를 새로 추가
def run_yolo_onnx_inference(yolo_session, frame):
    """
    ONNX YOLO 모델로 추론을 실행하고, 바운딩 박스 결과를 반환합니다.
    """
    # 1. 모델 입력 크기 가져오기
    input_shape = yolo_session.get_inputs()[0].shape
    input_height, input_width = input_shape[2:]

    # 2. 입력 프레임 전처리
    resized_frame = cv2.resize(frame, (input_width, input_height))
    input_tensor = resized_frame.transpose(2, 0, 1)
    input_tensor = np.expand_dims(input_tensor, axis=0)
    input_tensor = input_tensor / 255.0
    input_tensor = input_tensor.astype(np.float32)

    # 3. ONNX 모델로 추론 실행
    input_name = yolo_session.get_inputs()[0].name
    outputs = yolo_session.run(None, {input_name: input_tensor})

    # 4. 추론 결과 후처리 (NMS 등) - 이 부분이 가장 중요합니다!
    # Ultralytics 라이브러리가 자동으로 해주던 후처리를 직접 해야 합니다.
    # 여기서는 간단한 예시만 보여드리며, 프로젝트에 맞게 수정이 필요합니다.
    
    detections = []
    output_data = outputs[0][0]
    
    # (cx, cy, w, h, cls_prob...) 형식의 출력을 (x1, y1, x2, y2)로 변환
    boxes = []
    scores = []
    class_ids = []

    for i in range(output_data.shape[1]):
        # 클래스별 최대 확률 찾기
        class_id = np.argmax(output_data[4:, i])
        confidence = output_data[4+class_id, i]

        if confidence > 0.5:  # Confidence threshold
            cx, cy, w, h = output_data[:4, i]
            x1 = int((cx - w / 2) / input_width * frame.shape[1])
            y1 = int((cy - h / 2) / input_height * frame.shape[0])
            x2 = int((cx + w / 2) / input_width * frame.shape[1])
            y2 = int((cy + h / 2) / input_height * frame.shape[0])
            
            boxes.append([x1, y1, x2-x1, y2-y1]) # (x, y, w, h)
            scores.append(float(confidence))
            class_ids.append(class_id)

    # Non-Maximum Suppression 적용
    indices = cv2.dnn.NMSBoxes(boxes, scores, score_threshold=0.5, nms_threshold=0.4)
    
    for i in indices:
        box = boxes[i]
        detections.append({
            "box": (box[0], box[1], box[2], box[3]),
            "class_id": class_ids[i],
            "score": scores[i]
        })
        
    return detections



def run_lstm_inference(lstm_model, feature_buffer, prev, next, visit_fridge_copy):
    # 초기값 설정
    action_result = "None"
    prev_class = -1 # 탐지된 음식 없음
    next_class = -1 # 탐지된 음식 없음

    if not feature_buffer:
        print("[모델] feature_buffer가 비어있어 추론을 건너뜁니다.")
        return action_result, prev_class, next_class
    
    # --- 1. LSTM 추론 실행 ---
    lstm_input_tensor = torch.tensor(list(feature_buffer), dtype=torch.float32).unsqueeze(0)

    with torch.no_grad():
        output = lstm_model(lstm_input_tensor)
        _, predicted_label = torch.max(output, 1)
        print("[모델] LSTM 출력 클래스 확률 : ", output)
        print(f"[모델] LSTM 추론 완료, 예측된 레이블: {predicted_label.item()}")

        print("prev 딕셔너리 :", prev)
        print("next 딕셔너리 :", next)

    # --- 2. 추론 결과를 바탕으로 'prev/next' 클래스 결정 ---
    if not prev: # 탐지된 음식이 없을 때
        prev_class = -1
        print("[models] prev 탐지된 클래스 없음.")
    else: prev_class = max(prev, key=prev.get)

    if not next: # 탐지된 음식이 없을 때
        next_class = -1
        print("[models] next 탐지된 클래스 없음.")
    else: next_class = max(next, key=next.get)

    print("손이 들어갈 때 까지의 음식 : ", prev_class)
    print("손이 나온 이후의 음식 : ", next_class)
    
    label = predicted_label.item()
    print(f"[models] 결과 라벨 : {label}")

    if label == 0: # LSTM 결과가 None 일때
        if prev_class == -1 and next_class != -1:       # prev_class == -1 : 냉장고 진입 전 클래스 없음
            action_result = "Taking"
        elif prev_class != -1 and prev_class != next_class and next_class != -1:
            action_result = "Put&Take"
        elif prev_class != -1 and next_class == -1:
            action_result = "Putting"
        else: action_result = "None"
        
    elif label == 1: # LSTM 결과가 Put 일때           
        if prev_class == next_class:
            action_result = "None"
        elif prev_class == -1 and next_class != -1:
            action_result = "Taking"
        elif prev_class != -1 and prev_class != next_class and next_class != -1:
            action_result = "Put&Take"
        else: action_result = "Putting"

    elif label == 2: # LSTM 결과가 Take 일때
        if prev_class == -1 and next_class != -1:
            action_result = "Taking"
        elif prev_class != -1 and prev_class != next_class and next_class != -1:
            action_result = "Put&Take"

    elif label == 3: # LSTM 결과가 Put&Take 일때
        if prev_class == -1 and next_class != -1:
            action_result = "Taking"
        elif prev_class != -1 and prev_class != next_class and next_class != -1:
            action_result = "Put&Take"

    if not visit_fridge_copy:
        action_result = "None"
    print("동작 : ", action_result)

    # 계산 결과만 반환
    return action_result, prev_class, next_class
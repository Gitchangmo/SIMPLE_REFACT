# 라즈베리파이에서 프레임 받아 실시간 추론 코드
# 마지막 수정일 : 2025-05-09
# DeepSORT후처리 구현 안된 상태

import threading
import queue
import zmq
import time
import numpy as np
from collections import deque
import cv2
import firebase_admin

import config # 상수 및 이미지 설정값, 매핑값 등
import models # AI 모델 관련
from vision_processor import VisionProcessor   # 비전 처리 관련
from action_recognizer import ActionRecognizer # 행동 인식 관련

cv2.setUseOptimized(True)  # OpenCV 최적화 기능 활성화
cv2.setNumThreads(14)      # 사용할 스레드 개수 (PC 코어 수에 맞춰 조절)


# 초기 설정 (zmq 네트워크, 큐 생성)
context = zmq.Context()
pull = context.socket(zmq.PULL)
pull.bind(config.ZMQ_ADDRESS)
frame_queue = queue.Queue(maxsize=1) # 큐 생성 (프레임 전달용)

# LSTM 모델 생성에 필요한 파라미터들을 딕셔너리로 정리
lstm_parameters = {
    'input_dim'  : config.LSTM_FEAT_DIM,
    'hidden_dim1': config.LSTM_HIDDEN_DIM1,
    'hidden_dim2': config.LSTM_HIDDEN_DIM2,
    'dropout_p'  : config.LSTM_DROPOUT,
    'num_classes': config.LSTM_NUM_CLASSES
}

# AI 모델 호출 함수로 모델 불러오기
yolo_model, lstm_model = models.load_vision_models(
    yolo_path=config.YOLO_MODEL_PATH,
    lstm_path=config.LSTM_MODEL_PATH,
    lstm_params=lstm_parameters
)

# --- 모델 및 모듈 객체 생성 ---
print("프로그램 초기화 및 모델 로딩")
processor = VisionProcessor()               # 프레임 처리 전용 모듈
recognizer = ActionRecognizer(lstm_model)   # 객체, 손 상태 관리 모듈
print("초기화 완료. 메인 루프를 시작합니다.")

# 프레임 캡처 스레드: 지속적으로 프레임을 큐에 넣음
def frame_capture():
    """ZMQ를 통해 라즈베리파이로부터 프레임을 받아 큐에 넣는 역할만 수행"""
    while True:
        try:
            # ZMQ로부터 프레임 수신
            data = pull.recv()
            arr = np.frombuffer(data, dtype=np.uint8)
            frame = arr.reshape((config.IMG_WIDTH, config.IMG_HEIGHT, 3))

            while frame_queue.full():
                frame_queue.get()       # 큐가 가득 찼으면 이전 프레임을 버림
            frame_queue.put(frame)      # 최신 프레임을 큐에 넣음

        except Exception as e:
            print(f"프레임 수신 스레드 오류: {e}")  # 예외 메시지 출력
            time.sleep(1)


# 프레임 수신 스레드 시작
capture_thread = threading.Thread(target=frame_capture, daemon=True)
capture_thread.start()


# 메인 루프: 큐에서 프레임을 꺼내 처리하고, 추론 결과를 화면에 오버레이
while True:
    if not frame_queue.empty():
        recv_time = time.time()
        try:
            frame = frame_queue.get(timeout=1)      # 최신 프레임 가져오기
        except queue.Empty:
            continue

        print("[메인]", time.time() - recv_time, "큐에서 프레임 수신 완료")
        print(f"[디버그] 프레임 shape: {frame.shape}, dtype: {frame.dtype}, min: {frame.min()}, max: {frame.max()}")
        
        # 프레임 회전
        frame = cv2.rotate(frame, cv2.ROTATE_90_COUNTERCLOCKWISE) # 반시계 방향 90도 회전
     
        # 1. '상태 관리 모듈'에게서 현재 상태 정보를 가져온다.
        state_info = recognizer.get_current_state_info()
        
        # 2. '프레임 처리 모듈'에게 프레임 분석을 맡기고 결과를 받는다. -> (라벨링된 프레임)
        vision_report = processor.process_frame(frame, yolo_model, state_info)
        
        # 3. '상태 관리 모듈'에게 결과를 넘겨 '상태'를 업데이트하고 동작 결과를 라벨링 요청.
        recognizer.update(vision_report)
        
        # 4. 분석가가 예쁘게 그려준 최종 결과 프레임을 가져온다.
        frame_for_display = vision_report["frame_for_display"]

        # 5. LSTM 추론 결과를 화면에 추가로 그려준다.
        action_result = state_info.get('action_result', 'None')
        cv2.putText(frame_for_display, f"Action: {action_result}", (50, 360), # 150
                     cv2.FONT_HERSHEY_SIMPLEX, 1, (255, 0, 0), 2)

        # 6. 최종 화면을 시각화
        display_final = cv2.resize(frame_for_display, (1024, 576)) # 시연용 크기 조절 (0.8 크기)
        cv2.imshow("Smart Fridge Window", display_final)

        key = cv2.waitKey(1) & 0xFF

        if key == ord('q'):
            break
        elif key == ord('r'):
            recognizer._force_reset()

print("프로그램을 종료합니다.")
cv2.destroyAllWindows()
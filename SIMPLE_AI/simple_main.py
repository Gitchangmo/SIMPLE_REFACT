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
import sys  # 페이로드 크기 측정용

import config # 상수 및 이미지 설정값, 매핑값 등
import models # AI 모델 관련
from vision_processor import VisionProcessor   # 비전 처리 관련
from action_recognizer import ActionRecognizer # 행동 인식 관련

cv2.setUseOptimized(True)  # OpenCV 최적화 기능 활성화
cv2.setNumThreads(14)      # 사용할 스레드 개수 (PC 코어 수에 맞춰 조절)

# ============================================================
# 📊 AS-IS 성능 측정용 전역 변수
# ============================================================
perf_payload_kb = 0.0           # 수신된 페이로드 크기 (KB)
perf_frame_count = 0            # 처리된 프레임 수
perf_log_interval = 10          # 로그 출력 간격 (N 프레임마다)
perf_fps_history = deque(maxlen=30)  # FPS 이동평균용 (최근 30프레임)

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
    global perf_payload_kb
    
    while True:
        try:
            # ZMQ로부터 프레임 수신
            data = pull.recv()
            
            # 📊 페이로드 크기 측정 (수신된 압축 데이터 크기)
            perf_payload_kb = len(data) / 1024  # KB
            
            # [TO-BE] JPEG 디코딩 (압축된 데이터 → 이미지)
            arr = np.frombuffer(data, dtype=np.uint8)
            frame = cv2.imdecode(arr, cv2.IMREAD_COLOR)
            
            # 디코딩 실패 시 스킵
            if frame is None:
                print("[WARN] JPEG 디코딩 실패 - 프레임 스킵")
                continue

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
        # 📊 [AS-IS PERF] 프레임 처리 시작 시간
        frame_start_time = time.time()
        
        try:
            frame = frame_queue.get(timeout=1)      # 최신 프레임 가져오기
        except queue.Empty:
            continue
        
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

        # ============================================================
        # 📊 성능 지표 계산
        # ============================================================
        frame_end_time = time.time()
        latency_ms = (frame_end_time - frame_start_time) * 1000  # ms 단위
        current_fps = 1000 / latency_ms if latency_ms > 0 else 0
        
        perf_fps_history.append(current_fps)
        avg_fps = sum(perf_fps_history) / len(perf_fps_history)
        
        perf_frame_count += 1

        # 6. 최종 화면을 시각화
        display_final = cv2.resize(frame_for_display, (1024, 576)) # 시연용 크기 조절 (0.8 크기)
        
        # 📊 왼쪽 하단에 실시간 FPS 표시
        cv2.putText(display_final, f"FPS: {avg_fps:.1f} | Latency: {latency_ms:.1f}ms | Payload: {perf_payload_kb:.1f}KB", 
                    (10, 556), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)
        
        cv2.imshow("Smart Fridge Window", display_final)
        
        # N 프레임마다 콘솔 로그 출력
        if perf_frame_count % perf_log_interval == 0:
            print(f"[PERF] Frame #{perf_frame_count:04d} | "
                  f"Payload: {perf_payload_kb:.2f}KB | "
                  f"Latency: {latency_ms:.2f}ms | "
                  f"FPS: {current_fps:.2f} (avg: {avg_fps:.2f})")
        # ============================================================

        key = cv2.waitKey(1) & 0xFF

        if key == ord('q'):
            break
        elif key == ord('r'):
            recognizer._force_reset()

print("프로그램을 종료합니다.")
print(f"[AS-IS PERF] 총 처리 프레임: {perf_frame_count}")
cv2.destroyAllWindows()
import cv2
import mediapipe as mp
import config
import time
from concurrent.futures import ThreadPoolExecutor


# 이미지 처리의 모든 기능을 담당하는 클래스 (박스 그리기, 좌표값 저장 등)
class VisionProcessor:
    def __init__(self):
        print("[VisionProcessor] __init__ 시작")
        # MediaPipe 초기화 코드를 생성자(__init__)로 옮긴다
        self.mp_hands = mp.solutions.hands
        print("[VisionProcessor] mp.solutions.hands 완료")
        try:
            self.hands = self.mp_hands.Hands(
                static_image_mode=False, 
                max_num_hands=2, 
                min_detection_confidence=0.5
            )
        except Exception as e:
            print(f"[VisionProcessor] Hands() 에러: {e}")
            import traceback
            traceback.print_exc()
            raise
        print("[VisionProcessor] Hands() 초기화 완료")
        self.mp_drawing = mp.solutions.drawing_utils
        self.mp_drawing_styles = mp.solutions.drawing_styles
        
        # [Phase 2] 병렬 처리를 위한 ThreadPoolExecutor 생성
        # max_workers=2: YOLO용 1개 + MediaPipe용 1개
        self.executor = ThreadPoolExecutor(max_workers=2)
        
        # 📊 추론 시간 측정용
        self.yolo_times = []
        self.mediapipe_times = []
        self.frame_count = 0
        
        print("[VisionProcessor] MediaPipe Hands 모델이 초기화되었습니다.")
        print("[VisionProcessor] ThreadPoolExecutor 초기화 완료 (workers=2)")

    # IoU 계산 함수
    def calculate_iou(self, box1, box2):
        x1 = max(box1[0], box2[0])
        y1 = max(box1[1], box2[1])
        x2 = min(box1[2], box2[2])
        y2 = min(box1[3], box2[3])
        intersection = max(0, x2 - x1) * max(0, y2 - y1)
        area1 = (box1[2] - box1[0]) * (box1[3] - box1[1])
        area2 = (box2[2] - box2[0]) * (box2[3] - box2[1])
        union = area1 + area2 - intersection
        return intersection / union if union > 0 else 0

    # 프레임 하나를 처리하는 메인 함수
    def process_frame(self, frame, yolo_model, state_info=None):
        """
        입력된 프레임 하나에 대해 모든 비전 처리를 수행하고,
        분석된 결과 데이터를 딕셔너리 형태로 반환한다
        
        Args:
            frame: 입력 프레임
            yolo_model: YOLO 모델
            state_info: 상태 정보 딕셔너리 (In_fridge, prev_class, next_class 등)
        """
        # 1. 처리용/전시용 프레임 준비
        frame_for_display = frame.copy()
        print("[VisionProcessor] 시각화 용 프레임 크기:", frame_for_display.shape)
        frame_for_processing = cv2.resize(frame, (config.IMG_WIDTH // 2, config.IMG_HEIGHT // 2))  # 원본 코드와 동일한 크기로 리사이즈
        frame_rgb = cv2.cvtColor(frame_for_processing, cv2.COLOR_BGR2RGB)

        # 2. YOLO 및 MediaPipe 병렬 실행 [Phase 2]
        # ================================================================
        # 📊 각 모델 추론 시간 측정을 위한 래퍼 함수
        # ================================================================
        def timed_yolo(frame, conf):
            t0 = time.time()
            # OpenVINO 추론 (device 파라미터 제거 - 환경변수로 제어)
            result = yolo_model(frame, conf=conf)
            elapsed = (time.time() - t0) * 1000
            self.yolo_times.append(elapsed)
            return result
        
        def timed_mediapipe(frame_rgb):
            t0 = time.time()
            result = self.hands.process(frame_rgb)
            elapsed = (time.time() - t0) * 1000
            self.mediapipe_times.append(elapsed)
            return result
        
        # executor.submit(): 작업을 스레드 풀에 "제출"하고 즉시 Future 객체 반환
        yolo_future = self.executor.submit(timed_yolo, frame_for_processing, 0.5)
        hands_future = self.executor.submit(timed_mediapipe, frame_rgb)
        
        # ================================================================
        # .result(): Future가 완료될 때까지 대기 후 결과 반환
        # 두 작업이 동시에 실행되므로, 총 시간 = max(YOLO, MediaPipe)
        # ================================================================
        yolo_results = yolo_future.result()
        hands_results = hands_future.result()
        
        # 📊 10프레임마다 평균 추론 시간 출력
        self.frame_count += 1
        if self.frame_count % 10 == 0 and self.yolo_times and self.mediapipe_times:
            avg_yolo = sum(self.yolo_times[-30:]) / min(len(self.yolo_times), 30)
            avg_mp = sum(self.mediapipe_times[-30:]) / min(len(self.mediapipe_times), 30)
            print(f"[INFERENCE TIME] YOLO: {avg_yolo:.2f}ms | MediaPipe: {avg_mp:.2f}ms | "
                  f"Parallel Total: {max(avg_yolo, avg_mp):.2f}ms (Sequential would be: {avg_yolo + avg_mp:.2f}ms)")

        # 3. 결과 데이터 추출 및 좌표 복원
        # YOLO 결과 추출
        detected_objects = []
        for box, conf, cls in zip(yolo_results[0].boxes.xyxy, yolo_results[0].boxes.conf, yolo_results[0].boxes.cls):
            x1_small, y1_small, x2_small, y2_small = map(int, box.tolist())
            # 원본 코드와 동일한 방식으로 좌표 복원
            x1, y1, x2, y2 = x1_small * 2, y1_small * 2, x2_small * 2, y2_small * 2
            detected_objects.append({
                "class_id": int(cls.item()),
                "confidence": float(conf.item()),
                "box_small": (x1_small, y1_small, x2_small, y2_small),
                "box_original": (x1, y1, x2, y2)
            })

        # MediaPipe 결과 추출
        detected_hands = []
        if hands_results.multi_hand_landmarks:
            for hand_landmarks in hands_results.multi_hand_landmarks:
                h, w = frame_for_processing.shape[:2]
                # h, w = frame.shape
                # h, w = frame.shape[:2]
                x_coords = [lm.x * w for lm in hand_landmarks.landmark]
                y_coords = [lm.y * h for lm in hand_landmarks.landmark]
                x_min_small, y_min_small = int(min(x_coords)), int(min(y_coords))
                x_max_small, y_max_small = int(max(x_coords)), int(max(y_coords))
                # 원본 코드와 동일한 방식으로 좌표 복원
                x_min, y_min, x_max, y_max = x_min_small * 2, y_min_small * 2, x_max_small * 2, y_max_small * 2
                detected_hands.append({
                    "landmarks_small": hand_landmarks, # 랜드마크 정보 (작은 스케일)
                    "box_small": (x_min_small, y_min_small, x_max_small, y_max_small),
                    "box_original": (x_min, y_min, x_max, y_max)
                })

        # 4. 시각화 (큰 프레임에 그리기)

        # ROI 그리기 (원본 코드와 동일한 방식)
        roi_x1, roi_y1, roi_x2, roi_y2 = int(config.IMG_WIDTH * 0.65), 10, int(config.IMG_WIDTH) - 10, int(config.IMG_HEIGHT) - 10
        cv2.rectangle(frame_for_display, (roi_x1, roi_y1), (roi_x2, roi_y2), (255, 0, 0), 3)
        cv2.putText(frame_for_display, "ROI", (roi_x1 + 100, 50),
                    cv2.FONT_HERSHEY_SIMPLEX, 1, (255, 0, 0), 2)
        
        # 객체 박스 및 레이블 그리기
        for obj in detected_objects[:2]: # 최대 2개만
            box = obj["box_original"]
            class_name = yolo_model.names.get(obj["class_id"], "Unknown")
            label = f"{class_name} {obj['confidence']:.2f}"
            cv2.rectangle(frame_for_display, (box[0], box[1]), (box[2], box[3]), (0, 255, 0), 2)
            cv2.putText(frame_for_display, label, (box[0], box[1] - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.9, (0, 255, 0), 2)

        # 손 랜드마크 및 박스 그리기
        for hand in detected_hands:
            box = hand["box_original"]
            cv2.rectangle(frame_for_display, (box[0], box[1]), (box[2], box[3]), (0, 255, 255), 2)

        # 5. 추가 정보 계산W
        is_hand_in_roi = False
        hand_center_x = 0
        if detected_hands:
            hand_box = detected_hands[0]["box_original"]
            hand_center_x = (hand_box[0] + hand_box[2]) / 2
            if roi_x1 <= hand_center_x <= roi_x2: # 손 중심 좌표가 ROI 영역 안에 위치할 경우
                is_hand_in_roi = True
        
        if is_hand_in_roi:
            cv2.putText(frame_for_display, "Hands In", (50, 100),
                        cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 255), 2) # 255, 255, 0
        else:
            cv2.putText(frame_for_display, "Hands Out", (50, 100),
                        cv2.FONT_HERSHEY_SIMPLEX, 1, (128, 128, 128), 2)
        
        # 동작 감지 중일 때 "action_detecting" 표시
        if state_info and state_info.get('is_tracking', False):
            cv2.putText(frame_for_display, "action_detecting", (50, config.IMG_HEIGHT - 30),
                        cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 0), 2)
        
        # # 현재 프레임의 결과 표시 (None 또는 객체 이름)
        # if detected_objects:
        #     result_text = f"result: {yolo_model.names.get(detected_objects[0]['class_id'], 'Unknown')}"
        #     cv2.putText(frame_for_display, result_text, (50, 140),
        #                cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)
        # else:
        #     cv2.putText(frame_for_display, "result: None", (50, 140),
        #                cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 2)
        

        food_on_hands = False
        for obj in detected_objects:
            for hand in detected_hands:
                if self.calculate_iou(obj["box_original"], hand["box_original"]) > 0.3:
                    food_on_hands = True
                    print(f"[VisionProcessor] IoU 감지: {self.calculate_iou(obj['box_original'], hand['box_original']):.3f} > 0.3")
                    break
            if food_on_hands:
                break

        print(f"[VisionProcessor] food_on_hands 계산 결과: {food_on_hands}")

        if food_on_hands:
            cv2.putText(frame_for_display, "Food on hands", (50, 50),
                        cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)
        else:
            cv2.putText(frame_for_display, "No Food on hands", (50, 50),
                        cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 2)
        
        # 상태 정보 표시 (In_Fridge/Out_Fridge)
        if state_info:
            in_fridge = state_info.get('in_fridge', False)
            prev_class = state_info.get('prev_class', -1)
            next_class = state_info.get('next_class', -1)
            
            print("next클래스 들어왔나?" , next_class)
            # In_Fridge/Out_Fridge 상태 표시
            fridge_status = "In_Fridge" if in_fridge else "Out_Fridge"
            cv2.putText(frame_for_display, fridge_status, (50, 200),
                        cv2.FONT_HERSHEY_SIMPLEX, 1, (255, 255, 255), 2)
            
            # Prev 클래스 표시 (유효한 경우에만)
            if prev_class >= 0:
                prev_name = yolo_model.names.get(prev_class, f"Class_{prev_class}")
                cv2.putText(frame_for_display, f"Prev: {prev_name} ({prev_class})", (50, 240),
                           cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 255, 255), 2)
            else:
                cv2.putText(frame_for_display, "Prev: None", (50, 240),
                           cv2.FONT_HERSHEY_SIMPLEX, 0.8, (128, 128, 128), 2)
            
            # Next 클래스 표시 (유효한 경우에만)
            if next_class >= 0:
                next_name = yolo_model.names.get(next_class, f"Class_{next_class}")
                cv2.putText(frame_for_display, f"Next: {next_name} ({next_class})", (50, 280),
                           cv2.FONT_HERSHEY_SIMPLEX, 0.8, (255, 128, 0), 2)
            else:
                cv2.putText(frame_for_display, "Next: None", (50, 280),
                           cv2.FONT_HERSHEY_SIMPLEX, 0.8, (128, 128, 128), 2)
            
            # 현재 프레임의 감지된 객체 표시
            if detected_objects:
                current_classes = [obj["class_id"] for obj in detected_objects]
                current_names = [yolo_model.names.get(cls, f"Class_{cls}") for cls in current_classes]
                cv2.putText(frame_for_display, f"Current: {', '.join(current_names)}", (50, 320),
                           cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 255, 0), 2)
            else:
                cv2.putText(frame_for_display, "Current: None", (50, 320),
                           cv2.FONT_HERSHEY_SIMPLEX, 0.8, (128, 128, 128), 2)
        else:
            # state_info가 없는 경우 기본값 표시
            cv2.putText(frame_for_display, "Out_Fridge", (50, 200),
                        cv2.FONT_HERSHEY_SIMPLEX, 1, (255, 255, 255), 2)
        
        
        vision_report = {
            "frame_for_display": frame_for_display,
            "detected_objects": detected_objects,
            "detected_hands": detected_hands,
            "is_hand_in_roi": is_hand_in_roi,
            "food_on_hands": food_on_hands,
            "hand_center_x": hand_center_x,
            "obj_center_x": (detected_objects[0]["box_original"][0] + detected_objects[0]["box_original"][2]) / 2 if detected_objects else 0,
            "detected_classes": [obj["class_id"] for obj in detected_objects]
            }
        
        print(f"[VisionProcessor] vision_report의 food_on_hands: {vision_report['food_on_hands']}")
        
        return vision_report
    
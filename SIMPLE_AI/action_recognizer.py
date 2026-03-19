from collections import deque
import threading
import time
import numpy as np
import config
import models
import services

class ActionRecognizer:
    def __init__(self, lstm_model):
        """
        '상태'를 기억하고, 행동을 판단하는 모든 로직을 캡슐화합니다.
        """
        # --- 외부 전문가(모듈) ---
        self.lstm_model = lstm_model
        
        # --- 상태 관리를 위한 내부 변수 ---
        self.current_state = "IDLE"  # "IDLE", "TRACKING", "COOLDOWN", "DISPLAYING"
        self.last_action_result = "None"  # 마지막 LSTM 추론 결과 (None, Put, Take, Put&Take)
        self.is_processing = False  # 현재 추론 진행 중인지 여부
        
        # 타이머 관련 변수들
        self.result_display_start_time = None   # 동작 결과 표시 시작 시간
        self.result_display_duration = 5        # 동작 결과 표시 유지 시간 (5초)
        self.no_tracking_time = 10              # DISPLAYING 상태에서 새 TRACKING 없을 때 Ready로 전환하는 시간 (10초)

        # 냉장고 상태 관리를 위한 내부 변수
        self.out_flag = True
        self.enter_flag = False
        self.in_fridge = False

        # 행동 인식에 필요한 모든 상태 변수들
        self.feature_buffer = deque(maxlen=config.SEQ_LEN)
        self.prev_fridge_class_ids = {}
        self.next_fridge_class_ids = {}
        self.hand_object_class_ids = []
        self.visit_fridge_flag = False
        
        # 위치 변화량 계산을 위한 변수 
        self.prev_hand_center_x = None
        self.prev_obj_center_x = None

    def get_current_state_info(self):
        """
        현재 상태 정보를 딕셔너리로 반환
        vision_processor에서 라벨링에 사용할 수 있도록
        """
        # 가장 많이 감지된 prev, next 클래스 찾기
        prev_class = -1
        if self.prev_fridge_class_ids:
            prev_class = max(self.prev_fridge_class_ids, key=self.prev_fridge_class_ids.get)
        
        next_class = -1
        if self.next_fridge_class_ids:
            next_class = max(self.next_fridge_class_ids, key=self.next_fridge_class_ids.get)
        
        return {
            'in_fridge': self.in_fridge,
            'prev_class': prev_class,
            'next_class': next_class,
            'current_state': self.current_state,
            'visit_fridge_flag': self.visit_fridge_flag,
            'action_result': self.last_action_result,  # LSTM 추론 결과 추가
            #'is_tracking': self.current_state in ["TRACKING", "COOLDOWN", "DISPLAYING"]  # 동작 추적 중인지 여부
            'is_tracking': self.current_state == "TRACKING"  # TRACKING 상태일 때만 동작 감지 중으로 표시
        }

    def update(self, vision_report):
        # 1. vision_processor로부터 이미지 정보들을 전달받음
        is_hand_in_roi = vision_report["is_hand_in_roi"] # 손이 ROI 안에 있는지 여부
        hand_center_x = vision_report["hand_center_x"]   # 손의 중심 x좌표
        
        # 디버그: 매 프레임마다 food_on_hands 값 확인
        food_on_hands_debug = vision_report.get("food_on_hands", False)
        print(f"[ActionRecognizer] 현재 상태: {self.current_state}, food_on_hands: {food_on_hands_debug}")
        
        # 2. 그 정보들과 자신의 '과거 상태 정보(self.out_flag 등)'를 바탕으로 상태를 직접 업데이트
        if is_hand_in_roi and self.out_flag:                        # ROI내 손이 있고 out_flag가 True일 때
            self.out_flag = False                                   # out_flag는 False로
            self.enter_flag = True                                  # enter_flag는 True로

        if not is_hand_in_roi and self.enter_flag:                  # 손이 ROI 내에 없지만 enter_flag가 True일 때
            self.in_fridge = True                                   # 냉장고 안에 있는지 여부 True

        if is_hand_in_roi and self.enter_flag and self.in_fridge:   # ROI내에 손이 있고 enter_flag가 True, In_fridge가 True이면
            self.enter_flag = False
            self.in_fridge = False
            self.out_flag = True
            self.visit_fridge_flag = True  # 냉장고 방문 완료 플래그 설정
            
        if is_hand_in_roi and self.out_flag:                        # ROI 재진입 시
            self.visit_fridge_flag = True

        if hand_center_x < config.ROI_START and hand_center_x > 0:  # 손이 ROI를 완전히 나갔을 때 (ROI왼쪽을 벗어나면)
            self.enter_flag = False
            self.out_flag = True

        # === 상태 머신 로직 ===
        current_time = time.time() 
        
        # =====  [IDLE 상태]: 아무것도 안 하다가, 손이 ROI에 진입하면 행동 시작  =====
        if self.current_state == "IDLE":   
            if is_hand_in_roi:
                self.current_state = "TRACKING"
                print("[상태] 무상태 -> 동작 추론 진행 시작")
                self._reset_state()  # 행동 시작 시 모든 상태 초기화

                # 추론 진행 중이 아닐 때만 "Ready" 표시
                if not self.is_processing:
                    self.last_action_result = "Ready"

        # =====  [TRACKING 상태]: 손이 ROI 안에 있는 동안 계속 데이터 수집  =====
        elif self.current_state == "TRACKING":
            # TRACKING 중에는 타임아웃 없음 - 손이 ROI에서 나갈 때만 종료
            
            # 객체 클래스 ID 기록
            self._update_class_frequencies(vision_report)
            
            # LSTM 입력용 특징 벡터 계산 및 버퍼에 추가
            features = self._extract_features(vision_report)    # 특징 추출 로직을 별도 함수로
            self.feature_buffer.append(features)                # 실제 입력용 버퍼에 추가

            # 행동 종료 조건 검사: 손이 ROI에서 완전히 나갔을 때만 (832보다 작을 때)
            # in_fridge 상태일 때는 손이 일시적으로 감지 안될 수 있으므로 hand_center_x로 판단
            if hand_center_x < config.ROI_START and hand_center_x > 0:
                self.current_state = "COOLDOWN"
                self.is_processing = True  # 추론 시작
                print("[상태] 동작 추론 진행 중 -> 동작 종료 대기")
                
                # 행동이 끝났으므로, 모아둔 데이터로 추론 스레드를 실행!
                self._trigger_inference()

        # =====  [COOLDOWN 상태]: 추론이 진행되는 동안 잠시 대기  =====
        elif self.current_state == "COOLDOWN":
            # 추론이 완료되면 DISPLAYING 상태로 전환
            if not self.is_processing:  # 추론 완료 시
                self.current_state = "DISPLAYING"
                self.result_display_start_time = current_time
                print(f"[상태] 동작 종료 대기 -> 결과 표시 ({self.last_action_result})")

        # =====  [DISPLAYING 상태]: 동작 결과를 표시하면서 새로운 TRACKING 대기  =====
        elif self.current_state == "DISPLAYING":
            
            # 새로운 TRACKING이 시작되면 바로 전환
            if is_hand_in_roi:
                self.current_state = "TRACKING"
                print("[상태] 결과 표시 중 -> 새로운 동작 추론 시작")
                self._reset_state()  # 행동 시작 시 모든 상태 초기화
                
            # 일정 시간 동안 새로운 TRACKING이 없으면 Ready로 전환
            elif current_time - self.result_display_start_time > self.no_tracking_time:
                self.current_state = "IDLE"
                self.last_action_result = "Ready"
                print("[상태] 새 TRACKING 타임아웃 -> 무상태 전환 (Ready)")

        # UI에 표시할 현재 상태를 반환
        return self.last_action_result
    
    # 박스 좌표 정규화 함수
    def _normalize_box(self, box):
        x1, y1, x2, y2 = box
        x1_norm = x1 / config.IMG_WIDTH
        y1_norm = y1 / config.IMG_HEIGHT
        x2_norm = x2 / config.IMG_WIDTH
        y2_norm = y2 / config.IMG_HEIGHT
        return (x1_norm, y1_norm, x2_norm, y2_norm)

    # 상태 초기화 함수
    def _reset_state(self):
        """모든 상태 변수를 초기화"""
        print("[상태 초기화] 새로운 동작 감지를 시작합니다.")
        self.feature_buffer.clear()
        self.prev_fridge_class_ids.clear()
        self.next_fridge_class_ids.clear()
        self.hand_object_class_ids.clear()
        self.visit_fridge_flag = False
        self.in_fridge = False

        self.prev_hand_center_x = None
        self.prev_obj_center_x = None
        self.last_action_result = "Ready"

    def _force_reset(self):
        print("!!! EMERGENCY RESET !!! 사용자에 의해 모든 상태가 초기화 되었습니다.")
        self.current_state = "IDLE"
        self._reset_state()

    def _extract_features(self, vision_report):
        """현재 프레임에서 특징 벡터 추출"""
        features = []

        # --- 객체 정보 추출 ---
        detected_objects = vision_report.get("detected_objects", [])

        # Object1 정보 (없으면 빈 값으로 채움)
        if len(detected_objects) >= 1:
            obj1 = detected_objects[0]
            box_norm = self._normalize_box(obj1["box_original"])
            features.extend([obj1["class_id"], box_norm[0], box_norm[1], box_norm[2], box_norm[3]])
        else:
            features.extend([-1, 0, 0, 0, 0])

        # Object2 정보 (없으면 빈 값으로 채움)
        if len(detected_objects) >= 2:
            obj2 = detected_objects[1]
            box_norm = self._normalize_box(obj2["box_original"])
            features.extend([obj2["class_id"], box_norm[0], box_norm[1], box_norm[2], box_norm[3]])
        else:
            features.extend([-1, 0, 0, 0, 0])

        # --- 손 정보 추출 ---
        detected_hands = vision_report.get("detected_hands", [])
        hand_boxes_norm = []
        for hand in detected_hands:
            hand_boxes_norm.append(self._normalize_box(hand["box_original"]))

        while len(hand_boxes_norm) < 2:
            hand_boxes_norm.append((0,0,0,0)) # 손이 2개 미만일 경우 빈 값 채우기

        for box in hand_boxes_norm:
            features.extend(box)

        # --- 기타 정보 추출 ---
        obj_center_x = vision_report.get("obj_center_x", 0)
        hand_center_x = vision_report.get("hand_center_x", 0)

        # --- 이동량(dx) 계산 ---
        obj_dx = 0
        if self.prev_obj_center_x is not None:
            obj_dx = obj_center_x - self.prev_obj_center_x
            obj_dx = obj_dx / config.IMG_WIDTH

        hand_dx = 0
        if self.prev_hand_center_x is not None:
            hand_dx = hand_center_x - self.prev_hand_center_x
            hand_dx = hand_dx / config.IMG_WIDTH

        # --- 다음 프레임 계산을 위해 현재 x좌표를 이전 위치로 업데이트 ---
        self.prev_hand_center_x = hand_center_x
        self.prev_obj_center_x = obj_center_x

        features.append(obj_dx)
        features.append(hand_dx)
        
        print(f"[특징 벡터] obj_dx : {obj_dx}")
        print(f"[특징 벡터] hand_dx : {hand_dx}")
        
        # ROI 내 손 위치 여부
        is_hand_in_roi = vision_report.get("is_hand_in_roi", False)
        features.append(1 if is_hand_in_roi else 0)

        print(f"[특징 벡터] is_hand_in_roi : {is_hand_in_roi}")

        # 손에 음식을 들고 있는지 여부
        food_on_hands = vision_report.get("food_on_hands", False)
        features.append(1 if food_on_hands else 0)

        print(f"[특징 벡터] food_on_hands : {food_on_hands}")

        # 손이 냉장고에 들어갔는지 여부 
        # in_fridge = self._update_fridge_state(is_hand_in_roi, hand_center_x if detected_hands else 0)
        features.append(1 if self.in_fridge else 0)

        print(f"[특징 벡터] in_fridge : {self.in_fridge}")
        
        # 피처 벡터 차원 검사
        if len(features) != config.LSTM_FEAT_DIM:
            raise ValueError(f"특징 벡터 차원 오류: {len(features)} (기대 차원: {config.LSTM_FEAT_DIM})")
        else:
            print(f"[extract_features] 특징 벡터 추출 완료! : {len(features)}차원")
        
        return np.array(features, dtype=np.float32)

    def _update_class_frequencies(self, vision_report):
        # """객체 클래스 빈도 업데이트"""
        # detected_classes = vision_report["detected_classes"]
        """객체 클래스 빈도 업데이트 - ROI 내부 객체만 포함"""
        detected_objects = vision_report.get("detected_objects", [])
        
        # ROI 내부에 있는 객체들만 필터링
        roi_objects = []
        roi_x1 = int(config.IMG_WIDTH * 0.65)  # ROI 시작점
        
        for obj in detected_objects:
            box = obj["box_original"]
            obj_center_x = (box[0] + box[2]) / 2
            
            # 객체의 중심이 ROI 내부에 있는지 확인
            if obj_center_x >= roi_x1:
                roi_objects.append(obj["class_id"])
                print(f"[디버그] ROI 내부 객체 감지: class_id={obj['class_id']}, center_x={obj_center_x:.1f} (ROI_start={roi_x1})")
        
        # 손이 냉장고에 들어가기 전 (초기 상태)
        if not self.visit_fridge_flag:
            # for class_id in detected_classes:
            for class_id in roi_objects:
                self.prev_fridge_class_ids[class_id] = self.prev_fridge_class_ids.get(class_id, 0) + 1
                print(f"[디버그] prev_fridge_class_ids 업데이트: {class_id}")
        
        # 손이 냉장고를 방문한 후 (visit_fridge_flag = True 이후)
        elif self.visit_fridge_flag:
            #for class_id in detected_classes:
            for class_id in roi_objects:
                self.next_fridge_class_ids[class_id] = self.next_fridge_class_ids.get(class_id, 0) + 1
                print(f"[디버그] next_fridge_class_ids 업데이트: {class_id}, 현재 in_fridge: {self.in_fridge}")
        
        # 디버그: 현재 상태 출력
        print(f"[디버그] ROI 내부 객체 수: {len(roi_objects)}")
        print(f"[디버그] visit_fridge_flag: {self.visit_fridge_flag}, in_fridge: {self.in_fridge}")
        print(f"[디버그] prev_classes: {self.prev_fridge_class_ids}")
        print(f"[디버그] next_classes: {self.next_fridge_class_ids}")

    def _trigger_inference(self):
        """
        행동이 끝났을 때, 별도의 스레드에서 LSTM 추론 및 DB 업데이트를 실행한다.
        """
        # 스레드에서 사용할 현재 상태 값들을 안전하게 복사
        buffer_copy = self.feature_buffer.copy()
        prev_ids_copy = self.prev_fridge_class_ids.copy()
        next_ids_copy = self.next_fridge_class_ids.copy()
        visit_fridge_copy = self.visit_fridge_flag

        print(f"[trigger] 모델 입력할 프레임 버퍼 {buffer_copy}")
        print(f"[trigger] 모델 입력 버퍼 길이 {len(buffer_copy)}")
        print(f"[trigger] 모델 입력할 prev 클래스 {prev_ids_copy}")
        print(f"[trigger] 모델 입력할 next 클래스 {next_ids_copy}")
        print(f"[trigger] 냉장고 방문 여부 {visit_fridge_copy} <= 이게 False면 무조건 동작은 None 나옴")

        def inference_target():
            # 1. 동작인식기에 추론을 맡기고 결과 받기
            result, p_class, n_class = models.run_lstm_inference(
                self.lstm_model, buffer_copy,
                prev_ids_copy, next_ids_copy,
                visit_fridge_copy
            )
            
            print(f"[추론 결과] {result}")
            # 2. UI에 표시될 결과를 업데이트
            self.last_action_result = result
            print(f"[추론 결과] 받은 결과는 {self.last_action_result}")
            self.is_processing = False  # 추론 완료
            
            # 3. 의미 있는 행동일 때만 외부 통신 전문가에게 DB 업데이트를 지시
            if result != "None":
                print(f"[Action Recognizer] 예측된 동작: {result}")
                services.update_inventory(p_class, n_class, result)

        # 스레드 생성 및 시작
        inference_thread = threading.Thread(target=inference_target, daemon=True)
        inference_thread.start()

        
        
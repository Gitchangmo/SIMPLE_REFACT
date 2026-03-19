# ===== 하드웨어 및 이미지 설정 =====
import os
from dotenv import load_dotenv

load_dotenv()

IMG_WIDTH = int(os.getenv("IMG_WIDTH", 1280))
IMG_HEIGHT = int(os.getenv("IMG_HEIGHT", 720))
ROI_START = int(os.getenv("ROI_START", 832))
SEQ_LEN = int(os.getenv("SEQ_LEN", 120))
ZMQ_ADDRESS = os.getenv("ZMQ_BIND_ADDRESS", "tcp://*:5560")

# ===== 모델 경로 설정 =====
LSTM_MODEL_PATH = os.getenv("LSTM_MODEL_PATH", "./LSTM_MODEL/lstm_model_new_final_simple(0722)_v2.pth")
YOLO_MODEL_PATH = os.getenv("YOLO_MODEL_PATH", "./YOLO_MODEL/final_v1.pt")
FIREBASE_CRED_PATH = os.getenv("FIREBASE_CRED_PATH_VISION", "./JSON_FILE/simple.json")

# ===== 모델 파라미터 설정 =====
LSTM_FEAT_DIM = 23
LSTM_HIDDEN_DIM1 = 64
LSTM_HIDDEN_DIM2 = 32
LSTM_DROPOUT = 0.3
LSTM_NUM_CLASSES = 4

# ===== 데이터 매핑 =====
CLASS_NAME = {-1: 'Unknown', 0: '청양고추', 1: '레몬', 2: '파프리카',
            3: '아보카도', 4: '사과', 5: '콩나물', 6: '브로콜리',
            7: '양배추', 8: '당근', 9: '부추', 10: '오이',
            11: '무', 12: '계란', 13: '가지', 14: '팽이버섯',
            15: '바나나', 16: '마늘', 17: '대파',
            18: '양상추', 19: '양파', 20: '오렌지', 21: '참외',
            22: '감자', 23: '토마토'}

CATEGORY_NAME = {-1: 'Unknown', 0: '채소', 1: '과일', 2: '채소',
            3: '채소', 4: '과일', 5: '채소', 6: '채소',
            7: '채소', 8: '채소', 9: '채소', 10: '채소',
            11: '채소', 12: '가공/유제품', 13: '채소', 14: '채소',
            15: '과일', 16: '채소', 17: '채소',
            18: '채소', 19: '채소', 20: '과일', 21: '과일',
            22: '채소', 23: '과일'
}

IMAGE_ID_NAME = {-1: 'Unknown', 0: 'food8_50', 1: 'food4_5', 2: 'food8_55',
            3: 'food8_35', 4: 'food4_13', 5: 'food8_52', 6: 'food8_26',
            7: 'food8_39', 8: 'food8_11', 9: 'food8_25', 10: 'food8_45',
            11: 'food8_18', 12: 'food1_2', 13: 'food8_1', 14: 'food8_56',
            15: 'food4_9', 16: 'food8_16', 17: 'food8_12',
            18: 'food8_40', 19: 'food8_42', 20: 'food4_16'
}

EXPIRATION_DAYS = {-1: 0, 0: 10, 1: 21, 2: 14,
            3: 5, 4: 30, 5: 5, 6: 7,
            7: 21, 8: 21, 9: 5, 10: 7,
            11: 30, 12: 21, 13: 7, 14: 5,
            15: 7, 16: 30, 17: 7,
            18: 7, 19: 60, 20: 21}

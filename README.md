# 🥬 SIMPLE - Smart Inventory Manager for Personalized Life Enhancement

> AI 기반 스마트 냉장고 재고 관리 시스템

[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Python](https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=python&logoColor=white)](https://www.python.org/)
[![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)](https://firebase.google.com/)
[![FastAPI](https://img.shields.io/badge/FastAPI-009688?style=for-the-badge&logo=fastapi&logoColor=white)](https://fastapi.tiangolo.com/)
[![YOLOv8](https://img.shields.io/badge/YOLOv8-00FFFF?style=for-the-badge&logo=yolo&logoColor=black)](https://ultralytics.com/)
[![PyTorch](https://img.shields.io/badge/PyTorch-EE4C2C?style=for-the-badge&logo=pytorch&logoColor=white)](https://pytorch.org/)

---

## 📋 프로젝트 소개

**SIMPLE**은 냉장고 내 식재료의 입출고를 AI가 자동으로 인식하여 재고를 관리하고, 유통기한 알림 및 맞춤형 레시피를 추천하는 스마트 냉장고 솔루션입니다.

YOLO 객체 탐지와 LSTM 기반 행동 인식을 결합하여 사용자가 냉장고에 식재료를 **넣거나(Putting)**, **빼거나(Taking)**, **동시에 넣고 빼는(Put&Take)** 행동을 자동으로 감지합니다. 라즈베리파이에 장착된 카메라로 실시간 영상을 촬영하고, PC에서 AI 추론을 수행하여 Firebase에 재고 정보를 자동 업데이트합니다.

## 📅 개발 기간
* **2025.01 ~ 2025.06** (경남대학교 졸업작품)
* **2026.03 ~** 상용화 리팩토링 진행 중

## 🚧 현재 상태: 리팩토링 진행 중

> 본 프로젝트는 졸업작품에서 **B2C 상용 서비스 수준**으로 업그레이드 중입니다.

### ✅ 완료된 작업
- [x] 환경 변수 분리 (`.env` 기반 시크릿 관리)
- [x] `.gitignore` 보안 설정 완료
- [x] 아키텍처 분석 리포트 작성 (`ARCHITECTURE_ANALYSIS_REPORT.md`)
- [x] 리팩토링 로그 템플릿 준비 (`docs/REFACTORING_LOG.md`)

### 📋 예정된 작업
- [ ] Phase 1: ZeroMQ 네트워크 최적화 (JPEG 압축)
- [ ] Phase 2: YOLO/MediaPipe 병렬화
- [ ] Phase 3: 모델 경량화 (ONNX/TensorRT)
- [ ] Phase 4: Celery + Redis 비동기 아키텍처

## 👥 멤버 구성
| 이름 | 역할 | 담당 업무 |
|------|------|-----------|
| **박한아** | Frontend | Android 앱 UI/UX 개발 |
| **김현도** | Frontend | Android 앱 기능 구현 |
| **배채은** | Backend | FastAPI 서버 및 API 개발 |
| **김창모** | AI/Embedded | YOLO/LSTM 모델 개발, 라즈베리파이 연동 |
| **이동제** | AI/ML | 딥러닝 모델 학습 및 최적화 |
| **황도연** | Embedded | 하드웨어 구성 및 통신 |

---

## 🎯 주요 기능

### 1. AI 기반 자동 식재료 인식
- **YOLOv8 객체 탐지**: 24종 식재료 실시간 인식
- **LSTM 행동 인식**: 넣기/빼기/넣고빼기 3가지 행동 분류
- **MediaPipe 손 추적**: ROI(Region of Interest) 기반 손 위치 추적
- 정확도: YOLO mAP 0.85+, LSTM 행동 분류 정확도 92%+

### 2. 실시간 재고 관리
- Firebase Firestore 연동 자동 재고 업데이트
- 식재료별 수량, 유통기한, 카테고리 관리
- 입출고 이력 로깅 (history 컬렉션)

### 3. 스마트 알림 시스템
- **유통기한 임박 알림**: D-3, D-1, 당일 자동 푸시 알림
- **재고 변동 알림**: 식재료 입출고 시 실시간 알림
- APScheduler 기반 매일 오후 8시 13분 자동 알림 발송

### 4. AI 레시피 추천
- **오늘의 레시피**: 유통기한 임박 재료 기반 추천 (만개의레시피 CSV 데이터)
- **YouTube 레시피 검색**: 보유 재료 키워드 기반 영상 검색
- **주간 식단 추천**: OpenAI GPT 기반 7일 식단 자동 생성

### 5. AI 챗봇
- GPT-3.5-turbo 기반 요리 관련 질의응답
- 재료 대체, 조리법 문의 등 다양한 질문 지원

---

## 🏗️ 시스템 아키텍처

### 전체 구조
```
┌─────────────────────────────────────────────────────────────────────────┐
│                        SIMPLE System Architecture                        │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  🍓 Raspberry Pi          🤖 AI Module (PC)           🖥️ Backend Server  │
│  ┌──────────────┐        ┌──────────────────┐        ┌──────────────┐   │
│  │   Camera     │──ZMQ──►│    SIMPLE_AI     │──HTTP─►│  server_test │   │
│  │  (1280x720)  │        │  YOLO + LSTM     │        │   (FastAPI)  │   │
│  └──────────────┘        └──────────────────┘        └──────────────┘   │
│         │                        │                          │           │
│         │                        │                          │           │
│         ▼                        ▼                          ▼           │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                       🔥 Firebase Cloud                          │   │
│  │  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐     │   │
│  │  │ Firestore │  │    FCM    │  │  Storage  │  │   Auth    │     │   │
│  │  │ (재고DB)  │  │ (푸시알림)│  │ (이미지)  │  │ (사용자) │     │   │
│  │  └───────────┘  └───────────┘  └───────────┘  └───────────┘     │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                      │                                  │
│                                      ▼                                  │
│                            📱 Android App (App-main)                    │
│                            ┌──────────────────────┐                     │
│                            │  사용자 인터페이스   │                     │
│                            │  재고 조회/관리      │                     │
│                            │  레시피 추천/검색    │                     │
│                            │  알림 수신           │                     │
│                            └──────────────────────┘                     │
└─────────────────────────────────────────────────────────────────────────┘
```

### AI 추론 파이프라인
```
┌─────────────────────────────────────────────────────────────────────────┐
│                         AI Inference Pipeline                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  1. 프레임 수신 (ZMQ)                                                    │
│     └─► Raspberry Pi에서 1280x720 프레임 전송                            │
│                                                                          │
│  2. 비전 처리 (VisionProcessor)                                          │
│     ├─► YOLO 추론: 640x360으로 리사이즈 후 객체 탐지                     │
│     ├─► MediaPipe: 손 랜드마크 21개 포인트 추출                          │
│     └─► ROI 분석: x > 832 영역을 냉장고 내부로 판정                      │
│                                                                          │
│  3. 행동 인식 (ActionRecognizer)                                         │
│     ├─► 상태 머신: IDLE → TRACKING → COOLDOWN → DISPLAYING               │
│     ├─► Feature 추출: 손 좌표 + 객체 정보 → 23차원 벡터                  │
│     ├─► LSTM 추론: 120 프레임 시퀀스 → 4클래스 분류                      │
│     └─► 결과: None(0) / Putting(1) / Taking(2) / Put&Take(3)             │
│                                                                          │
│  4. Firebase 업데이트 (services.py)                                      │
│     └─► FastAPI 서버로 HTTP POST 요청 → Firestore 재고 갱신              │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 📁 프로젝트 구조

```
SIMPLE/
├── 📱 App-main/                        # Android 모바일 앱
│   ├── app/
│   │   └── src/main/java/...           # Android 소스 코드
│   └── build.gradle.kts
│
├── 🖥️ server_test/                     # FastAPI 백엔드 서버
│   ├── search_server.py                # 메인 API 서버
│   ├── firebase_admin_init.py          # Firebase 초기화
│   ├── chatbot.py                      # OpenAI GPT 챗봇
│   ├── csv_recommender.py              # CSV 기반 레시피 추천
│   ├── weekly_meal.py                  # 주간 식단 추천
│   ├── youtube_filter.py               # YouTube 검색
│   ├── notify.py                       # FCM 알림
│   └── fcm.py                          # FCM API
│
├── 🤖 SIMPLE_AI/                       # AI 추론 모듈 (PC)
│   ├── simple_main.py                  # 메인 추론 루프
│   ├── config.py                       # 설정값 (환경변수 연동)
│   ├── models.py                       # YOLO/LSTM 모델 정의
│   ├── vision_processor.py             # 비전 처리 (YOLO + MediaPipe)
│   ├── action_recognizer.py            # 행동 인식 상태 머신
│   └── services.py                     # Firebase/API 연동
│
├── 📚 docs/                            # 문서
│   └── REFACTORING_LOG.md              # 리팩토링 성능 측정 로그
│
├── 📄 설정 파일
│   ├── .env.example                    # 환경 변수 템플릿
│   ├── .env                            # 실제 환경 변수 (Git 제외)
│   ├── .gitignore                      # Git 제외 규칙
│   ├── requirements.txt                # Python 의존성
│   └── ARCHITECTURE_ANALYSIS_REPORT.md # 아키텍처 분석 리포트
│
└── 🔒 보안 파일 (Git 제외)
    ├── YOLO_MODEL/*.pt                 # YOLO 가중치
    ├── LSTM_MODEL/*.pth                # LSTM 가중치
    └── JSON_FILE/simple.json           # Firebase 서비스 키
```

---

## 🛠️ 기술 스택

### 📱 Frontend (Android)
| 기술 | 버전 | 용도 |
|------|------|------|
| **Java** | 1.8 | 앱 개발 언어 |
| **Android SDK** | 34 (min 26) | 타겟 플랫폼 |
| **Firebase Auth** | 23.2.1 | 사용자 인증 (이메일/비밀번호) |
| **Firebase Firestore** | 25.1.2 | 실시간 NoSQL 데이터베이스 |
| **Firebase Storage** | 21.0.1 | 이미지 파일 저장 |
| **Firebase Cloud Messaging** | 23.4.1 | 푸시 알림 |
| **Retrofit2** | 2.9.0 | HTTP REST API 통신 |
| **Glide** | 4.15.1 | 이미지 로딩 및 캐싱 |
| **MPAndroidChart** | 3.1.0 | 통계 그래프 (파이, 바 차트) |
| **FlexboxLayout** | 3.0.0 | 유연한 태그 레이아웃 |
| **CardView** | 1.0.0 | 카드 형태 UI |

### 🖥️ Backend (Server)
| 기술 | 용도 |
|------|------|
| **Python 3.11+** | 서버 개발 언어 |
| **FastAPI** | 비동기 REST API 프레임워크 |
| **Pydantic** | 요청/응답 데이터 검증 |
| **Firebase Admin SDK** | 서버 측 Firebase 관리 |
| **OpenAI API** | GPT-3.5-turbo 챗봇 |
| **Google YouTube Data API v3** | 레시피 영상 검색 |
| **APScheduler** | 백그라운드 스케줄링 (알림) |
| **Pandas** | CSV 데이터 처리 |
| **pytz** | 시간대 처리 (Asia/Seoul) |

### 🤖 AI Module
| 기술 | 버전 | 용도 |
|------|------|------|
| **YOLOv8** | ultralytics | 실시간 객체 탐지 (24종 식재료) |
| **LSTM** | PyTorch | 시계열 행동 인식 (4클래스) |
| **PyTorch** | 2.x | 딥러닝 프레임워크 |
| **ONNX Runtime** | - | 모델 최적화 및 추론 가속 |
| **MediaPipe** | 0.10+ | 손 랜드마크 추적 (21개 포인트) |
| **OpenCV** | 4.x | 영상 처리 및 시각화 |
| **ZeroMQ** | pyzmq | 라즈베리파이 ↔ PC 통신 |
| **NumPy** | - | 수치 연산 |

### ☁️ Infrastructure
| 기술 | 용도 |
|------|------|
| **Firebase** | BaaS (Backend as a Service) |
| **Raspberry Pi 5** | 엣지 디바이스 (카메라 모듈) |
| **ZMQ (tcp://*:5560)** | 실시간 프레임 스트리밍 |

---

## 🍎 인식 가능 식재료 (24종)

### 클래스별 상세 정보
| ID | 식재료 | 카테고리 | 기본 유통기한 | 이미지 ID |
|----|--------|----------|---------------|-----------|
| 0 | 청양고추 | 채소 | 10일 | food8_50 |
| 1 | 레몬 | 과일 | 21일 | food4_5 |
| 2 | 파프리카 | 채소 | 14일 | food8_55 |
| 3 | 아보카도 | 채소 | 5일 | food8_35 |
| 4 | 사과 | 과일 | 30일 | food4_13 |
| 5 | 콩나물 | 채소 | 5일 | food8_52 |
| 6 | 브로콜리 | 채소 | 7일 | food8_26 |
| 7 | 양배추 | 채소 | 21일 | food8_39 |
| 8 | 당근 | 채소 | 21일 | food8_11 |
| 9 | 부추 | 채소 | 5일 | food8_25 |
| 10 | 오이 | 채소 | 7일 | food8_45 |
| 11 | 무 | 채소 | 30일 | food8_18 |
| 12 | 계란 | 가공/유제품 | 21일 | food1_2 |
| 13 | 가지 | 채소 | 7일 | food8_1 |
| 14 | 팽이버섯 | 채소 | 5일 | food8_56 |
| 15 | 바나나 | 과일 | 7일 | food4_9 |
| 16 | 마늘 | 채소 | 30일 | food8_16 |
| 17 | 대파 | 채소 | 7일 | food8_12 |
| 18 | 양상추 | 채소 | 7일 | food8_40 |
| 19 | 양파 | 채소 | 60일 | food8_42 |
| 20 | 오렌지 | 과일 | 21일 | food4_16 |
| 21 | 참외 | 과일 | - | - |
| 22 | 감자 | 채소 | - | - |
| 23 | 토마토 | 과일 | - | - |

---

## 🚀 설치 및 실행

### 사전 요구사항
- **Python 3.11+**
- **Android Studio** (Hedgehog 이상)
- **Raspberry Pi 5** + 카메라 모듈
- **Firebase 프로젝트**
- **GPU (권장)**: CUDA 지원 GPU

### 0️⃣ 환경 변수 설정 (필수!)

```bash
# 1. .env.example을 복사하여 .env 파일 생성
cp .env.example .env

# 2. .env 파일을 열어 실제 값으로 수정
# - OPENAI_API_KEY
# - YOUTUBE_API_KEY
# - FASTAPI_SERVER_URL
# - Firebase 키 경로 등
```

### 1️⃣ Backend Server 실행

1. **프로젝트 클론**
   ```bash
   git clone <repository-url>
   cd SIMPLE/server_test
   ```

2. **가상환경 생성 및 활성화**
   ```bash
   python -m venv venv
   # Windows
   venv\Scripts\activate
   # Linux/Mac
   source venv/bin/activate
   ```

3. **의존성 설치**
   ```bash
   pip install python-dotenv fastapi uvicorn firebase-admin openai google-api-python-client pandas apscheduler pytz pydantic
   ```

4. **Firebase 설정**
   - Firebase Console에서 서비스 계정 키 다운로드
   - `json_file/simple.json`으로 저장

5. **서버 실행**
   ```bash
   uvicorn search_server:app --host 0.0.0.0 --port 8000 --reload
   ```

6. **API 문서 확인**
   - Swagger UI: http://localhost:8000/docs
   - ReDoc: http://localhost:8000/redoc

### 2️⃣ AI Module 실행

1. **의존성 설치**
   ```bash
   cd SIMPLE/SIMPLE_AI
   pip install python-dotenv torch torchvision ultralytics mediapipe opencv-python pyzmq firebase-admin numpy onnxruntime
   ```

2. **모델 파일 확인**
   - `YOLO_MODEL/final_v1.pt` (YOLO 가중치)
   - `LSTM_MODEL/lstm_model_new_final_simple(0722)_v2.pth` (LSTM 가중치)

3. **Firebase 설정**
   - `JSON_FILE/simple.json`에 서비스 계정 키 배치

4. **라즈베리파이 카메라 스트리밍 시작** (별도 터미널)
   ```python
   # Raspberry Pi에서 실행
   import zmq
   import cv2
   context = zmq.Context()
   socket = context.socket(zmq.PUSH)
   socket.connect("tcp://<PC_IP>:5560")
   # ... 프레임 전송 코드
   ```

5. **AI 추론 시작**
   ```bash
   python simple_main.py
   ```

### 3️⃣ Android App 빌드

1. **Android Studio에서 프로젝트 열기**
   ```
   File → Open → SIMPLE/App-main 선택
   ```

2. **Firebase 설정**
   - `app/google-services.json` 파일 확인
   - Firebase Console에서 Android 앱 등록

3. **API 서버 주소 설정**
   ```java
   // RetrofitClient.java에서 BASE_URL 수정
   private static final String BASE_URL = "http://<서버IP>:8000/";
   ```

4. **빌드 및 실행**
   ```bash
   ./gradlew assembleDebug
   # 또는 Android Studio에서 Run 버튼 클릭
   ```

5. **권한 확인**
   - 알림 권한 (Android 13+)
   - 인터넷 권한

---

## 📡 API 엔드포인트

### 레시피 검색 API

#### `POST /search` - YouTube 레시피 검색
**Request Body:**
```json
{
  "ingredients": ["양파", "당근", "감자"]
}
```
**Response:**
```json
[
  {
    "title": "감자 양파 볶음",
    "thumbnail_url": "https://...",
    "video_url": "https://www.youtube.com/watch?v=...",
    "description": "..."
  }
]
```

#### `POST /recommend` - 오늘의 레시피 추천
**Request Body:**
```json
{
  "ingredient": "당근"  // 선택 (없으면 랜덤)
}
```
**Response:**
```json
[
  {
    "title": "당근 라페",
    "image_url": "https://...",
    "url": "https://www.10000recipe.com/recipe/...",
    "difficulty": "초급",
    "time": "30분 이내",
    "ingredient": "당근 200g, 올리브유...",
    "selectedIngredient": "당근"
  }
]
```

### 챗봇 API

#### `POST /chat` - AI 챗봇 대화
**Request Body:**
```json
{
  "message": "계란으로 만들 수 있는 요리 추천해줘"
}
```
**Response:**
```json
{
  "reply": "계란으로 만들 수 있는 요리를 추천해 드릴게요! 1. 계란찜..."
}
```

### 주간 식단 API

#### `POST /recommend/mealplan` - 주간 식단 추천
**Request Body:**
```json
{
  "userID": "aDfCOX4kY6eNXvf8jbBpQNel1sG3",
  "inventory": [
    {
      "name": "당근",
      "expirationDate": "2025-01-20",
      "quantity": 3,
      "category": "채소"
    }
  ]
}
```
**Response:**
```json
{
  "weekPlan": [
    {
      "day": "월",
      "meals": [
        {"meal": "아침", "recipe": "당근죽", "ingredients": ["당근", "쌀"]}
      ]
    }
  ]
}
```

---

## 🔍 동작 흐름

### 식재료 인식 및 재고 업데이트 흐름
```
1. 라즈베리파이 카메라에서 프레임 캡처
   └─► ZMQ PUSH 소켓으로 PC에 전송 (1280x720)

2. PC에서 프레임 수신 (simple_main.py)
   └─► frame_queue에 최신 프레임만 유지

3. 비전 처리 (VisionProcessor)
   ├─► 프레임 리사이즈 (640x360)
   ├─► YOLO 추론: 객체 탐지 (conf > 0.5)
   ├─► MediaPipe: 손 랜드마크 추출
   └─► ROI 판정: 손 x좌표 > 832 → 냉장고 내부

4. 행동 인식 (ActionRecognizer)
   ├─► 상태 머신 업데이트
   │   └─► IDLE → TRACKING → COOLDOWN → DISPLAYING
   ├─► Feature 추출 (23차원)
   │   └─► 손 좌표, 객체 IoU, 이동량 등
   ├─► LSTM 추론 (120 프레임 시퀀스)
   └─► 결과: Putting(1) / Taking(2) / Put&Take(3)

5. Firebase 업데이트 (services.py)
   ├─► FastAPI 서버로 HTTP POST 요청
   │   └─► /event 엔드포인트 (notify.py)
   ├─► Firestore 재고 수량 갱신
   │   └─► fridges/{userId}/items/{foodName}
   ├─► 입출고 이력 기록
   │   └─► fridges/{userId}/history
   └─► FCM 푸시 알림 발송
       └─► "당근이(가) 냉장고에 들어갔습니다"
```

### 유통기한 알림 흐름
```
1. 스케줄러 실행 (매일 20:13:10)
   └─► APScheduler BackgroundScheduler

2. 전체 사용자 순회 (notify_expiry_all_users)
   └─► Firestore users 컬렉션 조회

3. 유통기한 확인
   ├─► D-3: "당근의 유통기한이 3일 남았습니다"
   ├─► D-1: "당근의 유통기한이 내일까지입니다"
   └─► D-Day: "당근의 유통기한이 오늘까지입니다!"

4. FCM 알림 발송
   └─► FCM v1 API (fcm.py)
```

---

## 📊 데이터베이스 스키마 (Firestore)

### 컬렉션 구조
```
firestore/
├── users/                          # 사용자 정보
│   └── {userId}/
│       ├── email: string
│       ├── fcmToken: string        # FCM 토큰
│       ├── createdAt: timestamp
│       └── notifies/               # 알림 기록 (서브컬렉션)
│           └── {notifyId}/
│               ├── title: string
│               ├── body: string
│               └── timestamp: timestamp
│
├── fridges/                        # 냉장고 재고
│   └── {userId}/
│       ├── items/                  # 식재료 목록 (서브컬렉션)
│       │   └── {itemId}/
│       │       ├── name: string            # 식재료명
│       │       ├── quantity: number        # 수량
│       │       ├── expirationDate: string  # 유통기한 (YYYY-MM-DD)
│       │       ├── category: string        # 카테고리
│       │       ├── imageName: string       # 이미지 ID
│       │       ├── storage: string         # 보관 위치 (냉장/냉동)
│       │       ├── addedDate: string       # 등록일
│       │       └── notifiedStages: array   # 알림 발송 단계
│       │
│       └── history/                # 입출고 이력 (서브컬렉션)
│           └── {logId}/
│               ├── foodName: string
│               ├── action: string          # "put" / "take"
│               ├── quantity: number
│               ├── remainAfter: number
│               ├── category: string
│               ├── memo: string
│               └── timestamp: timestamp
│
├── weekly_meal/                    # 주간 식단
│   └── {userId}/
│       └── meals/
│           └── {date}/             # YYYY-MM-DD
│               ├── created_at: timestamp
│               └── week_plan: array
│
└── favorites/                      # 찜한 레시피
    └── {userId}/
        └── recipes/
            └── {recipeId}/
                ├── title: string
                ├── url: string
                └── image_url: string
```

---

## 🐛 디버깅 및 로그

### AI 모듈 로그
```bash
# 실행 시 콘솔 출력 예시
[VisionProcessor] MediaPipe Hands 모델이 초기화되었습니다.
[메인] 0.0023 큐에서 프레임 수신 완료
[디버그] 프레임 shape: (720, 1280, 3), dtype: uint8
[ActionRecognizer] 현재 상태: TRACKING, food_on_hands: True
[update inventory 함수 진입] prev_class: 8, next_class: -1, action: Putting
✅ 당근 신규 등록 (수량: 1)
```

### 서버 로그
```bash
# FastAPI 서버 로그 예시
INFO:     Started server process [12345]
INFO:     Uvicorn running on http://0.0.0.0:8000
[서버 실행] 스케줄러 시작됨! 2025-01-13 10:00:00
데이터 전달받음
토큰 받음: dK3x...
🔹 전송할 token: dK3x...
```

### 트러블슈팅

| 문제 | 원인 | 해결 방법 |
|------|------|-----------|
| YOLO 모델 로드 실패 | 모델 파일 경로 오류 | `config.py`의 `YOLO_MODEL_PATH` 확인 |
| ZMQ 연결 안됨 | 포트 충돌 또는 방화벽 | 5560 포트 개방, IP 주소 확인 |
| FCM 알림 안옴 | 토큰 만료 또는 권한 | 앱 재설치, 알림 권한 확인 |
| Firebase 연결 실패 | 서비스 계정 키 오류 | `simple.json` 파일 재다운로드 |
| 앱 API 호출 실패 | BASE_URL 설정 오류 | `RetrofitClient.java` 서버 IP 확인 |

---

## 🚧 향후 개발 계획

### 🔥 상용화 리팩토링 (진행 중)
- [ ] **Phase 1**: ZeroMQ 네트워크 최적화 (JPEG 압축, 대역폭 90%↓)
- [ ] **Phase 2**: YOLO/MediaPipe 병렬화 (`ThreadPoolExecutor`)
- [ ] **Phase 3**: 모델 경량화 (ONNX → TensorRT, 추론 3×↑)
- [ ] **Phase 4**: Celery + Redis 비동기 태스크 큐
- [ ] **Phase 5**: Dynamic Batching (멀티 냉장고 대응)

### 📱 기능 확장
- [ ] 추가 식재료 클래스 확장 (50종 이상)
- [ ] 딥러닝 기반 유통기한 자동 인식 (OCR)
- [ ] 음성 인식 기반 재고 관리
- [ ] 다중 사용자 지원 (가족 공유)
- [ ] 레시피 영양 정보 분석
- [ ] 장보기 목록 자동 생성
- [ ] 웹 대시보드 개발
- [ ] Docker 컨테이너화

---

## 📄 라이선스

이 프로젝트는 **경남대학교 졸업작품**으로 개발되었습니다.
학술 및 교육 목적으로만 사용 가능합니다.

---

## 📞 문의

- **GitHub Issues**: 버그 리포트 및 기능 제안
- **Email**: ---

---

## 📚 참고 자료

- [YOLOv8 Documentation](https://docs.ultralytics.com/)
- [Firebase Documentation](https://firebase.google.com/docs)
- [FastAPI Documentation](https://fastapi.tiangolo.com/)
- [MediaPipe Hands](https://google.github.io/mediapipe/solutions/hands.html)
- [PyTorch LSTM](https://pytorch.org/docs/stable/generated/torch.nn.LSTM.html)
- [ZeroMQ Guide](https://zguide.zeromq.org/)
- [만개의레시피](https://www.10000recipe.com/) - 레시피 데이터 출처

---

**Last Updated**: 2026년 3월 18일

---

> 💡 **Note**: 이 프로젝트는 환경 변수 기반 설정을 사용합니다. `.env.example`을 참고하여 `.env` 파일을 생성한 후 실행해 주세요.

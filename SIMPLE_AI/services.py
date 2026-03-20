from datetime import date, timedelta
from firebase_admin import db, firestore
import requests
import config
from firebase_admin import credentials
import firebase_admin
import os
from dotenv import load_dotenv

load_dotenv()

# 파이어베이스 연결
db = None
fridge_items = None

try:
    cred = credentials.Certificate(config.FIREBASE_CRED_PATH)
    firebase_admin.initialize_app(cred)
    db = firestore.client()
    fridge_items = db.collection("fridges").document("aDfCOX4kY6eNXvf8jbBpQNel1sG3").collection("items")
    print("[DB] 파이어베이스와 연결을 성공했습니다.")
except Exception as e:
    print(f"🛑 파이어베이스 연결 실패: {e}")
    print("⚠️ DB 기능이 비활성화됩니다. 로컬 테스트만 가능합니다.")

def update_inventory(prev_class, next_class, action):
        print(f"[update inventory 함수 진입] prev_class: {prev_class}, next_class: {next_class}, action: {action}")
        # prev 클래스 정보
        prev_className = config.CLASS_NAME[prev_class]
        prev_category_name = config.CATEGORY_NAME[prev_class]
        prev_imageName = config.IMAGE_ID_NAME[prev_class]
        prev_expiration = config.EXPIRATION_DAYS[prev_class]
        today = date.today()
        prev_expire_date = today + timedelta(days=prev_expiration)
        prev_expire_date_str = prev_expire_date.strftime("%Y-%m-%d")
        prev_food_doc = fridge_items.document(prev_className)
        prev_doc_snapshot = prev_food_doc.get()

        # next 클래스 정보
        next_className = config.CLASS_NAME[next_class]
        next_category_name = config.CATEGORY_NAME[next_class]
        next_imageName = config.IMAGE_ID_NAME[next_class]
        next_expiration = config.EXPIRATION_DAYS[next_class]
        next_expire_date = today + timedelta(days=next_expiration)
        next_expire_date_str = next_expire_date.strftime("%Y-%m-%d")
        next_food_doc = fridge_items.document(next_className)
        next_doc_snapshot = next_food_doc.get()

        action = action

        print("[update inventory] : ", action)

        if action == "Putting":
            print(f"✅ {prev_className} 신규 등록 (수량: 1)")
            send_to_fastapi(
                user_id="aDfCOX4kY6eNXvf8jbBpQNel1sG3",
                name= prev_className,
                quantity=1,
                expiration_date=prev_expire_date_str,
                imageName = prev_imageName,
                category= prev_category_name,
                action=action,
                storage = "냉장"
            )
        elif action == "Taking":
            # next_food_doc = fridge_items.document(next_className)
            # doc_snapshot = next_food_doc.get()
            if next_doc_snapshot.exists:
                expiration_date = next_doc_snapshot.to_dict().get("expirationDate")
            else:
                expiration_date = "기한없음"
            send_to_fastapi(
                user_id="aDfCOX4kY6eNXvf8jbBpQNel1sG3",
                name=next_className,
                quantity=1,  # 빼는 동작은 -1로 표기 (혹은 빼는 개수)
                expiration_date=expiration_date,  # 빼는 동작이면 유통기한은 불필요, 혹은 기존 값 사용
                imageName=next_imageName,  # 필요 시 넘김
                category= next_category_name,       # 필요 시 넘김
                action=action,
                storage = "냉장"
            )
        elif action == "Put&Take":
            # 1) 넣기 (추가)
            if prev_class is not None and prev_class != -1:
                send_to_fastapi(
                    user_id="aDfCOX4kY6eNXvf8jbBpQNel1sG3",
                    name=prev_className,
                    quantity=1,
                    expiration_date=prev_expire_date_str,
                    imageName=prev_imageName,
                    category=prev_category_name,
                    action="Putting",
                    storage="냉장"
                )
                
            # 2) 빼기 (제거)
            if next_class is not None and next_class != -1:
                if next_doc_snapshot.exists:
                    expiration_date = next_doc_snapshot.to_dict().get("expirationDate")
                else:
                    expiration_date = "기한없음"
                send_to_fastapi(
                    user_id="aDfCOX4kY6eNXvf8jbBpQNel1sG3",
                    name=next_className,
                    quantity=1,  # or -1 for removing
                    expiration_date=expiration_date,
                    imageName=next_imageName,
                    category=next_category_name,
                    action="Taking",
                    storage="냉장"
                )



def send_to_fastapi(user_id, name, quantity, expiration_date, imageName, category, action, storage):
    url = os.getenv("FASTAPI_SERVER_URL", "http://localhost:8000") + "/event"
    data = {
        "userId": user_id,
        "name": name,
        "quantity": quantity,
        "expirationDate": expiration_date,
        "imageName" : imageName,
        "category": category,
        "action" : action,
        "storage" : storage
    }
    print("[Fastapi 전송 데이터]", data)  # 디버깅용
    try:
        response = requests.post(url, json=data)
        print("Fastapi 응답:", response.status_code, response.text)
    except Exception as e:
        print("Fastapi 전송 실패:", e)
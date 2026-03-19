from firebase_admin_init import db
from fcm import send_fcm_v1
from datetime import date, datetime
from firebase_admin import messaging
import datetime
from google.cloud import firestore

def process_event(userId,
        name,
        quantity,
        expirationDate,
        imageName,
        category,
        action,
        storage):
    print("데이터 전달받음")
    # 토큰 저장소에서 토큰 받아오기
    token_doc = db.collection("users").document(userId).get()
    if not token_doc.exists:
        return {"status": "error", "message": "Invalid userId"}
    token = token_doc.to_dict().get('fcmToken')
    print("토큰 받음:", token)
    print("문서 존재함?:", token_doc.exists)
    print("token_doc dict:", token_doc.to_dict())
    print("🔹 전송할 token:", token)

    item_col = db.collection("fridges").document(userId).collection("items") # 음싣 저장 주소
    item_query = item_col.where("name", "==", name).limit(1).stream() # 클래스 이름과 음식이름이 같은 필드 하나
    item_doc = next(item_query, None)

    log_ref = db.collection("fridges").document(userId).collection("history") # 로그 기록 저장소소

    noti_ref = db.collection("users").document(userId).collection("notifies") # 알림 기록 저장소
    
    put_final_quan = None
    if action == "Putting":
        prev_qty = 0
        if item_doc:
            doc_ref = item_col.document(item_doc.id)
            prev_qty = doc_ref.get().to_dict().get('quantity', 0)
            # 이미 있으면 수량 증가
            put_final_quan = prev_qty + quantity
            doc_ref.update({
                "quantity": put_final_quan,
            })
            # 사용자 행동 로그 기록 : 음식 추가
            log_ref.add({
                "foodName": name, "category": category,
                "action": "put", "quantity": quantity, "remainAfter": quantity+1,
                "timestamp": firestore.SERVER_TIMESTAMP, "memo": "음식 추가" 
            })
            print("재고 수량증가")
        else:
            # 없으면 새로 추가
            item_col.add({
                "name": name,
                "quantity": quantity,
                "expirationDate": expirationDate,
                "userId": userId,
                "notifiedStages": [],
                "addedDate": str(date.today()),
                "category": category,
                "imageName": imageName,
                "storage": "냉장"
            })
            # 사용자 행동 로그 기록 : 음식 추가
            log_ref.add({
                "foodName": name, "category": category,
                "action": "put", "quantity": quantity, "remainAfter": quantity,
                "timestamp": firestore.SERVER_TIMESTAMP, "memo": "음식 추가" 
            })
            print("재고 하나추가가")
        if token:
            title = "식재료 추가"
            body = f"{name}이(가) 냉장고에 들어갔습니다.\n현재 수량 : {prev_qty+1}"
            send_fcm_v1(title, body, token)
            noti_ref.add({
                "title" : title,
                "body" : body,
                "timestamp" : firestore.SERVER_TIMESTAMP,
                "read" : False
            })
        return{"result": "추가 완료!"}
    
    elif action == "Taking":
        if item_doc:
            doc_ref = item_col.document(item_doc.id)
            prev_qty = doc_ref.get().to_dict().get('quantity', 0)
            print("차감할 문서 ID:", item_doc.id)
            if prev_qty > 1:
                # 수량 감소
                doc_ref.update({
                    "quantity": prev_qty - quantity,
                })
                # 사용자 행동 로그 기록 : 음식 소비
                log_ref.add({
                "foodName": name, "category": category,
                "action": "consume", "quantity": quantity, "remainAfter": quantity-1,
                "timestamp": firestore.SERVER_TIMESTAMP, "memo": "소비함" 
                })
                print("재고 하나 뺌뺌")
                if token:
                    title = "식재료 사용"
                    body = f"{name}(이)가 냉장고에서 꺼내졌습니다.\n잔여 수량 : {prev_qty -1}"
                    send_fcm_v1(title, body, token)
                    noti_ref.add({
                        "title" : title,
                        "body" : body,
                        "timestamp" : firestore.SERVER_TIMESTAMP,
                        "read" : False
                    })
                return {"result": "차감 완료!"}
            else:
                # 사용자 행동 로그 기록 : 음식 소비
                log_ref.add({
                "foodName": name, "category": category,
                "action": "consume", "quantity": quantity, "remainAfter": 0,
                "timestamp": firestore.SERVER_TIMESTAMP, "memo": "소비함" 
                #str(datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
                })
                # 1개면 삭제
                doc_ref.delete()
                print("재고 하나 빼고 0개됨")
                if token:
                    title = "식재료 사용"
                    body = f"{name}(이)가 냉장고에서 꺼내졌습니다.\n잔여 수량 : 0"
                    send_fcm_v1(title, body, token)
                    noti_ref.add({
                        "title" : title,
                        "body" : body,
                        "timestamp" : firestore.SERVER_TIMESTAMP,
                        "read" : False
                    })
                return {"result": "마지막 재고 삭제 완료!"}
        else:
            print("문서 없음 - 빼는 동작 실패")
            return {"result": "재고 없음, 차감 불가"}
    else:
        return {"result": "알 수 없는 동작"}
    

# 모든 유저 대상으로 조건에 따라 알림 발송 (토큰 없으면 안감)
def notify_expiry_all_users():
    today = date.today()
    users = db.collection("users").stream()
    print("스케줄러 트리거됨! 현재 서버 시각:", datetime.datetime.now())

    for user in users:
        user_data = user.to_dict()
        user_id = user_data.get('uid')
        fcm_token = user_data.get('fcmToken')
        noti_ref = db.collection("users").document(user_id).collection("notifies")
        if not fcm_token:
            print(f"fcmToken 없음! 유저 {user_id}")
            continue

        foods_ref = db.collection("fridges").document(user_id).collection("items")
        foods = foods_ref.stream()

        # 7/3/1일 남은 식품 그룹별로 저장
        group_foods = {7: [], 3: [], 1: []}
        for food in foods:
            food_data = food.to_dict()
            exp_str = food_data.get('expirationDate')
            if not exp_str or exp_str == "기한 없음":
                continue
            exp_date = datetime.datetime.strptime(exp_str, "%Y-%m-%d").date()
            dday = (exp_date - today).days
            if dday in group_foods:
                group_foods[dday].append((food_data['name'], exp_str))

        # 7,3,1일 남은 재고가 1개라도 있으면 한 번에 알림
        total_items = sum(len(v) for v in group_foods.values())
        if total_items > 0:
            lines = []
            for d in [1, 3, 7]:
                if group_foods[d]:
                    names = [name for name, _ in group_foods[d]]
                    lines.append(f"{d}일 남음: {', '.join(names)}")
            body2 = "\n".join(lines)
            print("body2:", body2)
            lines.append("\n해당 식재료로 레시피를 추천해드릴까요?")
            body = "\n".join(lines)
            print("body", body)
            title = f"유통기한 임박 재료 {total_items}개!"

            # 가장 임박한 d-day와 식재료도 data로 전송(앱에서 활용 가능)
            most_urgent = min([d for d in group_foods if group_foods[d]], default=7)
            most_urgent_names = [name for name, _ in group_foods[most_urgent]]

            message = messaging.Message(
                notification=messaging.Notification(
                    title=title,
                    body=body
                ),
                token=fcm_token,
                android=messaging.AndroidConfig(
                    notification=messaging.AndroidNotification(
                        channel_id="default"
                    )
                ),
                data={
                    'type': 'expiry_alert',
                    'food_names': ','.join(most_urgent_names),
                    'dday': str(most_urgent),
                    'detail': body
                }
            )

            noti_ref.add({
                "title" : title,
                "body" : body2,
                "timestamp" : firestore.SERVER_TIMESTAMP,
                "read" : False
            })
            response = messaging.send(message)
            print("FCM sent:", response)

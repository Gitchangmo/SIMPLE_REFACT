import requests
from firebase_admin import messaging

def send_fcm_v1(title, body, token):
    message = messaging.Message(
        notification=messaging.Notification(
            title=title,
            body=body,
        ),
        token=token,
    )
    response = messaging.send(message)
    print("✅ 알림 전송 성공:", response)
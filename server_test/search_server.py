from datetime import date
import datetime
from fastapi import FastAPI
from pydantic import BaseModel
from youtube_filter1 import search_youtube_videos # 유튜브 레시피 검색 기능
from csv_recommender import recommend_recipe # 오늘의 레시피 추천 기능
from chatbot import query_chatbot  # 챗봇 기능
from weekly_meal import recommend_weekly_plan, save_weekly_plan_to_firestore, to_days_left # 주간 식단 추천 기능
from typing import List
from notify import process_event, notify_expiry_all_users
from apscheduler.schedulers.background import BackgroundScheduler
from contextlib import asynccontextmanager
from pytz import timezone
from firebase_admin_init import db


# FastAPI에서 생명주기 이벤트에 실행할 명령어들 전부 나열. 현재는 scheduler만
@asynccontextmanager
async def app_lifespan(app: FastAPI):
    scheduler = BackgroundScheduler(timezone=timezone('Asia/Seoul'))
    scheduler.add_job(notify_expiry_all_users, 'cron', hour=20, minute=13, second=10)
    scheduler.start()
    print("[서버 실행] 스케줄러 시작됨!", datetime.datetime.now())
    yield

    #서버 종료 시
    scheduler.shutdown()
    print("[서버 종료] 스케줄러 종료됨!", datetime.datetime.now())

# FastAPI 앱 생성
app = FastAPI(lifespan = app_lifespan)

class IngredientsRequest(BaseModel):
    ingredients: list[str]

class RecommendRequest(BaseModel):  
    ingredient: str = None

class ChatRequest(BaseModel):
    message: str

class InventoryItem(BaseModel):
    name: str
    expirationDate: str
    quantity: int
    category: str

class InventoryRequest(BaseModel):
    userID: str
    inventory: List[InventoryItem]

class EventRequest(BaseModel):
    userId: str
    name: str
    quantity: int
    expirationDate: str
    imageName: str
    category: str
    action: str
    storage: str


@app.post("/search") # 유튜브 레시피 검색 
def search_videos(req: IngredientsRequest):
    return search_youtube_videos(req.ingredients, target_total=100)

@app.post("/recommend") # 오늘의 레시피 추천
def recommend(req: RecommendRequest):
    return recommend_recipe(req.ingredient)

@app.post("/chat") # 챗봇 기능능
def chat(req: ChatRequest):
    reply = query_chatbot(req.message)
    if reply.startswith("Error:"):
        return {"error": reply}
    return {"reply": reply}

@app.post("/recommend/mealplan") # 주간 식단 추천
async def mealplan_api(request: InventoryRequest):
    user_id = request.userID
    user_inventory = [
        {"name": item.name, "expiryDate": to_days_left(item.expirationDate),
         "quantity" : item.quantity, "category": item.category }  # 변환해서 추천 함수로 넘김
        for item in request.inventory
    ]
    # 주간 식단 추천 함수 호출
    week_plan = recommend_weekly_plan(user_inventory)
    save_weekly_plan_to_firestore(user_id, week_plan, db)
    return { "weekPlan": week_plan}

# @app.route('/recommend', methods=['POST'])
# def handle_recommendation():
#     user_data = request.json
#     user_id = user_data.get('userId')
#     inventory = user_data.get('inventory')

#     # 메인 추천 로직 호출
#     weekly_plan = recommend_weekly_plan(inventory)

#     # Firestore에 결과 저장
#     if 'error' not in weekly_plan:
#         save_weekly_plan_to_firestore(user_id, weekly_plan, db)

#     return jsonify({"week": weekly_plan})

@app.post("/event") # 알림 전송
async def event_handler(req: EventRequest):
    result = process_event(
        req.userId,
        req.name,
        req.quantity,
        req.expirationDate,
        req.imageName,
        req.category,
        req.action,
        req.storage
    )
    return result
    
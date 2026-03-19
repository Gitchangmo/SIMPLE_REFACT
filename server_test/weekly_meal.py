# import pandas as pd
# import random
# from collections import defaultdict
# import firebase_admin
# from firebase_admin import credentials, firestore
# from firebase_admin_init import cred, db
# from datetime import datetime, date

# # cred = credentials.Certificate("./json_file/simple.json")  # 다운로드한 서비스 계정 키 파일
# # firebase_admin.initialize_app(cred)
# # db = firestore.client()

# # 1. 레시피 데이터 로딩 (CSV → DataFrame)
# df = pd.read_csv('TB_RECIPE_SEARCH_241226.csv', usecols=[
#     'RCP_SNO', 'RCP_TTL', 'CKG_MTRL_CN', 'RCP_IMG_URL'
# ])
# df = df.dropna(subset=['CKG_MTRL_CN', 'RCP_TTL'])

# # 2. 요일/끼니 세팅
# DAYS = ["월", "화", "수", "목", "금", "토", "일"]
# MEALS = ["아침", "점심", "저녁"]

# # 동의어 사전 선언
# SYNONYM_MAP = {
#     "계란": ["계란", "달걀"],
#     "소고기": ["소고기", "쇠고기"],
# }
# def get_synonyms(name):
#     return SYNONYM_MAP.get(name, [name])

# # D-Day계산
# def to_days_left(exp):
#     # 이미 int면 그대로, "5" (string number)면 int로 변환
#     try:
#         return int(exp)
#     except Exception:
#         pass
#     # "2024-06-14" 같은 날짜 문자열이면 날짜 차이 계산
#     try:
#         today = date.today()
#         expirationDate = datetime.strptime(exp, "%Y-%m-%d").date()
#         return (expirationDate - today).days
#     except Exception:
#         return 999  # 못 읽으면 넉넉하게
    
# def expand_with_synonyms(name_list):
#     result = []
#     for name in name_list:
#         result.extend(get_synonyms(name))
#     return list(set(result))  # 중복 제거

# def save_weekly_plan_to_firestore(user_id, week_plan):
#     import datetime
#     today = datetime.date.today().isoformat()  # 오늘 날짜 문자열 (예: 2024-06-13)

#     doc_ref = db.collection('weekly_meal').document(user_id).collection('meals').document(today)
#     doc_ref.set({
#         'created_at': firestore.SERVER_TIMESTAMP,
#         'week_plan': week_plan  # JSON 직렬화 가능 구조여야 함
#     })
#     print(f"Saved meal plan for user {user_id} ({today})")

# def recommend_weekly_plan(user_inventory, days=7, meals_per_day=3):
#     # 1. 임박 우선 정렬
#     inventory_sorted = sorted(user_inventory, key=lambda x: x['expiryDate']) #임박한 유통기한 순으로 정렬 진행
#     inventory_names = [item['name'] for item in inventory_sorted] #정렬된 아이템들(음식들)의 이름 저장
#     expire_dict = {item['name']: item['expiryDate'] for item in inventory_sorted}
#     quantity_dict = {item['name']: item['quantity'] for item in inventory_sorted}

#     # 2. 재고 사용 카운트 (최대 소진까지 체크)
#     used_count = defaultdict(int)
#     used_recipes = set()
#     result = []

#     for d in range(days):
#         today_plan = {"day": DAYS[d%7], "meals": []}
#         for m in range(meals_per_day):
#             meal_type = MEALS[m % 3]
#             # 3. 오늘 기준 임박 재고 선정 (D-3 이하)
#             soon_expire = [name for name in inventory_names if expire_dict[name] <= 3 and used_count[name] < quantity_dict[name]]
#             normal_stock = [name for name in inventory_names if used_count[name] < quantity_dict[name]]
            
#             soon_expire_syn = expand_with_synonyms(soon_expire)
#             normal_stock_syn = expand_with_synonyms(normal_stock)

#             # 4. 후보 레시피 필터링 (임박재고 우선, 없으면 모든 재고)
#             if soon_expire:
#                 candidates = df[df['CKG_MTRL_CN'].apply(lambda x: any(ing in x for ing in soon_expire))]
#             else:
#                 candidates = df[df['CKG_MTRL_CN'].apply(lambda x: any(ing in x for ing in normal_stock))]
#             # 5. 이미 사용한 레시피 제외
#             candidates = candidates[~candidates['RCP_SNO'].isin(used_recipes)]

#             # 6. 점수화 (임박재고 많이 쓸수록, 재고 여러개 쓸수록 점수↑)
#             def calc_score(row):
#                 count_soon = sum(1 for ing in soon_expire if ing in row['CKG_MTRL_CN']) if soon_expire else 0
#                 count_normal = sum(1 for ing in normal_stock if ing in row['CKG_MTRL_CN'])
#                 return count_soon * 10 + count_normal

#             if not candidates.empty:
#                 candidates = candidates.copy()
#                 candidates['score'] = candidates.apply(calc_score, axis=1)
#                 top_score = candidates['score'].max()
#                 top_candidates = candidates[candidates['score'] == top_score]
#                 selected = top_candidates.sample(1).iloc[0]  # 동점시 랜덤
#                 used_recipes.add(selected['RCP_SNO']) # 사용한 레시피 리스트에 레시피 넘버 추가
#                 # 7. 사용한 재고 표시
#                 for ing in inventory_names:
#                     if ing in selected['CKG_MTRL_CN'] and used_count[ing] < quantity_dict[ing]:
#                         used_count[ing] += 1
#                 today_plan["meals"].append({
#                     "type": meal_type,
#                     "title": selected['RCP_TTL'],
#                     "desc": selected['CKG_MTRL_CN'],
#                     "imageUrl": selected['RCP_IMG_URL'],
#                     "link": f"https://www.10000recipe.com/recipe/{int(selected['RCP_SNO'])}",
#                     "usedIngredients": [ing for ing in inventory_names if ing in selected['CKG_MTRL_CN']]
#                 })
#             else:
#                 today_plan["meals"].append({
#                     "type": meal_type,
#                     "title": "추천 불가",
#                     "desc": "해당 재고로 만들 수 있는 레시피 없음",
#                     "imageUrl": "",
#                     "link": "",
#                     "usedIngredients": []
#                 })
#         result.append(today_plan)
#         # 8. 하루 지날 때마다 유통기한 -1 업데이트
#         for ing in expire_dict:
#             expire_dict[ing] -= 1
#     return result


import pandas as pd
import random
from collections import defaultdict
from datetime import datetime, date
import firebase_admin
from firebase_admin import credentials, firestore

# ==============================================================================
# 1. 서버 시작 시에만 실행되는 초기화 구간
# ==============================================================================

# Firebase 초기화 (이미 다른 곳에서 초기화했다면 주석 처리 유지)
# cred = credentials.Certificate("./json_file/simple.json")
# firebase_admin.initialize_app(cred)
# db = firestore.client()

print("서버 초기화를 시작합니다...")

# 레시피 데이터 로딩
try:
    df = pd.read_csv('TB_RECIPE_SEARCH_241226.csv', usecols=[
        'RCP_SNO', 'RCP_TTL', 'CKG_MTRL_CN', 'RCP_IMG_URL'
    ])
    df = df.dropna(subset=['CKG_MTRL_CN', 'RCP_TTL'])
    print(f"레시피 {len(df)}개를 로드했습니다.")
except FileNotFoundError:
    print("[오류] 'TB_RECIPE_SEARCH_241226.csv' 파일을 찾을 수 없습니다.")
    df = pd.DataFrame() # 빈 데이터프레임으로 초기화

# [핵심 최적화] 역색인 (Inverted Index) 생성
# 재료 이름을 key로, 해당 재료가 포함된 레시피 ID(RCP_SNO) 리스트를 value로 갖는 딕셔너리
ingredient_index = defaultdict(list)
if not df.empty:
    for _, row in df.iterrows():
        # 'CKG_MTRL_CN' 컬럼의 재료들을 파싱합니다.
        # 예시: "돼지고기 100g, 양파 1개" -> ["돼지고기", "양파"]
        # 실제 데이터 형식에 맞게 파싱 로직을 정교하게 만들어야 합니다.
        # 여기서는 간단하게 공백, 쉼표 등으로 단어를 분리하는 예시를 사용합니다.
        ingredients_text = row['CKG_MTRL_CN'].replace(',', ' ').replace('(', ' ').replace(')', ' ')
        ingredients = {word.strip() for word in ingredients_text.split() if len(word.strip()) > 1}

        for ingredient in ingredients:
            ingredient_index[ingredient].append(row['RCP_SNO'])
    print(f"역색인 생성을 완료했습니다. {len(ingredient_index)}개의 재료가 인덱싱되었습니다.")

# ==============================================================================
# 2. 유틸리티 함수들
# ==============================================================================

DAYS = ["월", "화", "수", "목", "금", "토", "일"]
MEALS = ["아침", "점심", "저녁"]

# 동의어 사전 (필요에 따라 확장)
SYNONYM_MAP = {
    "계란": ["계란", "달걀"],
    "소고기": ["소고기", "쇠고기"],
    "돼지고기": ["돼지고기", "돈육"],
}

def get_synonyms(name):
    return SYNONYM_MAP.get(name, [name])

def expand_with_synonyms(name_list):
    result = set()
    for name in name_list:
        result.update(get_synonyms(name))
    return list(result)

def to_days_left(exp_str):
    try:
        today = date.today()
        expiration_date = datetime.strptime(exp_str, "%Y-%m-%d").date()
        return (expiration_date - today).days
    except (ValueError, TypeError):
        # 날짜 형식이 아니거나 None일 경우 넉넉하게 999일로 처리
        return 999

def save_weekly_plan_to_firestore(user_id, week_plan, db):
    today_str = date.today().isoformat()
    doc_ref = db.collection('weekly_meal').document(user_id).collection('meals').document(today_str)
    doc_ref.set({
        'created_at': firestore.SERVER_TIMESTAMP,
        'week_plan': week_plan
    })
    print(f"Firestore에 {user_id}의 식단을 저장했습니다.")

# ==============================================================================
# 3. 최적화된 메인 추천 로직
# ==============================================================================

def recommend_weekly_plan(user_inventory, days=7, meals_per_day=3):
    if df.empty:
        return {"error": "레시피 데이터가 로드되지 않았습니다."}
    
    # 1. [최적화] 유통기한을 미리 D-day로 계산하고 수량 정보와 함께 가공
    inventory_processed = [
        {
            'name': item['name'],
            'days_left': to_days_left(item['expiryDate']),
            'quantity': item.get('quantity', 1)
        }
        for item in user_inventory
    ]
    inventory_sorted = sorted(inventory_processed, key=lambda x: x['days_left'])
    inventory_names = [item['name'] for item in inventory_sorted]
    quantity_dict = {item['name']: item['quantity'] for item in inventory_sorted}

    used_count = defaultdict(int)
    used_recipes = set()
    result = []

    for d in range(days):
        today_plan = {"day": DAYS[d % 7], "meals": []}
        for m in range(meals_per_day):
            meal_type = MEALS[m % 3]

            # 2. 오늘 기준 사용 가능한 재고 선정
            soon_expire = [item['name'] for item in inventory_sorted if item['days_left'] <= 3 and used_count[item['name']] < quantity_dict[item['name']]]
            normal_stock = [item['name'] for item in inventory_sorted if used_count[item['name']] < quantity_dict[item['name']]]
            
            search_ingredients = expand_with_synonyms(soon_expire if soon_expire else normal_stock)

            # 3. [핵심 최적화] 역색인을 사용해 후보 레시피 ID를 빠르게 조회
            candidate_ids = set()
            for ingredient in search_ingredients:
                candidate_ids.update(ingredient_index.get(ingredient, []))
            
            if not candidate_ids:
                # 추천할 레시피가 없는 경우 처리
                continue

            # 4. 후보 레시피들을 DataFrame에서 한번에 필터링
            candidates = df[df['RCP_SNO'].isin(candidate_ids)]
            candidates = candidates[~candidates['RCP_SNO'].isin(used_recipes)]

            if candidates.empty:
                # 추천할 레시피가 없는 경우 처리
                continue

            # 5. 점수화 및 최종 레시피 선택 
            def calc_score(row):
                count_soon = sum(1 for ing in soon_expire if ing in row['CKG_MTRL_CN'])
                count_normal = sum(1 for ing in normal_stock if ing in row['CKG_MTRL_CN'])
                return count_soon * 10 + count_normal
            
            candidates = candidates.copy()
            candidates['score'] = candidates.apply(calc_score, axis=1)
            top_score = candidates['score'].max()
            
            if top_score > 0:
                top_candidates = candidates[candidates['score'] == top_score]
                selected = top_candidates.sample(1).iloc[0]
                used_recipes.add(selected['RCP_SNO'])

                used_ingredients_list = []
                for ing in inventory_names:
                    if ing in selected['CKG_MTRL_CN'] and used_count[ing] < quantity_dict[ing]:
                        used_count[ing] += 1
                        used_ingredients_list.append(ing)

                today_plan["meals"].append({
                    "type": meal_type,
                    "title": selected['RCP_TTL'],
                    "desc": selected['CKG_MTRL_CN'],
                    "imageUrl": selected['RCP_IMG_URL'],
                    "link": f"https://www.10000recipe.com/recipe/{int(selected['RCP_SNO'])}",
                    "usedIngredients": used_ingredients_list
                })
            else:
                # 점수가 0인 경우 (추천 불가) 처리
                pass

        result.append(today_plan)
        
        # 6. 하루 지날 때마다 유통기한 D-day 업데이트
        for item in inventory_sorted:
            item['days_left'] -= 1
            
    return result

# --- API 서버에서 이 함수를 호출할 때의 예시 ---
# @app.route('/recommend', methods=['POST'])
# def handle_recommendation():
#     user_data = request.json
#     user_id = user_data.get('userId')
#     inventory = user_data.get('inventory')
#
#     # 메인 추천 로직 호출
#     weekly_plan = recommend_weekly_plan(inventory)
#
#     # Firestore에 결과 저장
#     if 'error' not in weekly_plan:
#         save_weekly_plan_to_firestore(user_id, weekly_plan, db)
#
#     return jsonify({"week": weekly_plan})
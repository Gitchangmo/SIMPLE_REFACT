# csv_recommender.py

import pandas as pd
import random
from ingredient_keyword_map import ingredient_keyword_map
import re

# CSV 로딩
df = pd.read_csv("TB_RECIPE_SEARCH_241226.csv", usecols=[
    'RCP_SNO', 'RCP_TTL', 'CKG_MTRL_CN', 'CKG_MTRL_ACTO_NM',
    'CKG_TIME_NM', 'CKG_DODF_NM', 'RCP_IMG_URL'
])
df.dropna(subset=['CKG_MTRL_CN', 'RCP_TTL'], inplace=True)
df['CKG_MTRL_CN'] = df['CKG_MTRL_CN'].astype(str)
df['CKG_MTRL_ACTO_NM'] = df['CKG_MTRL_ACTO_NM'].astype(str)

# 단어 단위 포함 검수
# (ingredient_keyword_map.py)에 등록해놓은 키워드들 검수
# ex) 들어온 단어 "감" -> "감", "단감" 포함
# 부분 문자열도 삭제 감 -> 감자 xxxx
def contains_any_keyword(series, keywords):
    pattern = "|".join(rf'\b{re.escape(k)}\b' for k in keywords)
    return series.str.contains(pattern, case=False, regex=True, na=False)


def recommend_recipe(ingredient=None, count=4):
    if not ingredient:
        ingredient = random.choice(df['CKG_MTRL_ACTO_NM'].dropna().unique())
    
    keywords = ingredient_keyword_map.get(ingredient, [ingredient])
    
#    filtered = df[
#        df['CKG_MTRL_CN'].str.contains(ingredient, case=False) |
#        df['CKG_MTRL_ACTO_NM'].str.contains(ingredient, case=False)
#    ]

    filtered = df[
        contains_any_keyword(df['CKG_MTRL_CN'], keywords) |
        contains_any_keyword(df['CKG_MTRL_ACTO_NM'], keywords)
    ]

    sampled = filtered.sample(n=min(count, len(filtered)), random_state=random.randint(1, 9999))

    return [
        {
        "title": row["RCP_TTL"] if not pd.isna(row["RCP_TTL"]) else "",
        "image_url": row["RCP_IMG_URL"] if not pd.isna(row["RCP_IMG_URL"]) else "",
        "url": f"https://www.10000recipe.com/recipe/{int(row['RCP_SNO'])}",
        "difficulty": row["CKG_DODF_NM"] if not pd.isna(row["CKG_DODF_NM"]) else "",
        "time": row["CKG_TIME_NM"] if not pd.isna(row["CKG_TIME_NM"]) else "",
        "ingredient": row["CKG_MTRL_CN"] if not pd.isna(row["CKG_MTRL_CN"]) else "", 
        "selectedIngredient": ingredient
        }
        for _, row in sampled.iterrows()
    ]
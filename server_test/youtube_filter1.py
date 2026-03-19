import os
from googleapiclient.discovery import build
from dotenv import load_dotenv

load_dotenv()

def search_youtube_videos(ingredients, target_total=100):
    api_key = os.getenv("YOUTUBE_API_KEY")
    youtube = build('youtube', 'v3', developerKey=api_key)

    query = " ".join(ingredients) + " 레시피"
    filtered_videos = []
    fetched = 0
    next_page_token = None
    video_id_map = {}

    while fetched < target_total:
        search_request = youtube.search().list(
            q=query,
            part='snippet',
            maxResults=min(50, target_total - fetched),
            type='video',
            pageToken=next_page_token
        )
        search_response = search_request.execute()
        items = search_response.get('items', [])

        video_ids = []
        for item in items:
            video_id = item['id']['videoId']
            title = item['snippet']['title']
            thumbnail_url = item['snippet']['thumbnails']['default']['url']
            video_url = f"https://www.youtube.com/watch?v={video_id}"

            # 임시 저장 (description은 videos().list로 다시 불러올 거라 생략)
            video_id_map[video_id] = {
                'title': title,
                'thumbnail_url': thumbnail_url,
                'video_url': video_url,
            }
            video_ids.append(video_id)

        if not video_ids:
            break

        # 이제 videos API로 전체 description 받아오기
        video_request = youtube.videos().list(
            part="snippet",
            id=",".join(video_ids),
            maxResults=50
        )
        video_response = video_request.execute()

        for item in video_response.get("items", []):
            video_id = item["id"]
            description = item["snippet"]["description"]
            title = item["snippet"]["title"]

            # 쇼츠 제외
            if "#shorts" in title.lower() or "#shorts" in description.lower():
                continue

            # 설명란에 모든 재료가 포함되어야 함
            if all(ingredient in description for ingredient in ingredients if ingredient.strip()):
                base = video_id_map.get(video_id)
                if base:
                    base['description'] = description
                    filtered_videos.append(base)

        fetched += len(items)
        next_page_token = search_response.get('nextPageToken')

        if not next_page_token:
            break

    return filtered_videos

# youtube_filter.py
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

    while fetched < target_total:
        request = youtube.search().list(
            q=query,
            part='snippet',
            maxResults=min(50, target_total - fetched),
            type='video',
            pageToken=next_page_token
        )
        response = request.execute()
        items = response.get('items', [])

        for item in items:
            title = item['snippet']['title']
            description = item['snippet']['description']
            video_id = item['id']['videoId']
            thumbnail_url = item['snippet']['thumbnails']['default']['url']
            video_url = f"https://www.youtube.com/watch?v={video_id}"

            # 쇼츠 제외: 제목이나 설명에 #shorts 포함된 경우 제외
            if "#shorts" in title.lower() or "#shorts" in description.lower():
                continue

            # 설명란에 모든 재료가 포함되어야 함
            if all(ingredient in description for ingredient in ingredients if ingredient.strip()):
                filtered_videos.append({
                    'title': title,
                    'thumbnail_url': thumbnail_url,
                    'video_url': video_url,
                    'description': description
                })

        fetched += len(items)
        next_page_token = response.get('nextPageToken')

        if not next_page_token:
            break

    return filtered_videos

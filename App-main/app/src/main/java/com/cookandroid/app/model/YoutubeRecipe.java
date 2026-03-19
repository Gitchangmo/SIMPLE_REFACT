package com.cookandroid.app.model;
//ㄱㄱㄱㄱㄱㄱ


public class YoutubeRecipe {
    private String title;
    private String thumbnail_url;
    private String video_url;

    private String image;
    private String description;


    public YoutubeRecipe() {}

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) { this.title = title; }


    public String getThumbnail_url() {
        return thumbnail_url;
    }
    public void setThumbnail_url(String thumbnail_url) { this.thumbnail_url = thumbnail_url; }

    public String getVideo_url() {
        return video_url;
    }
    public void setVideo_url(String video_url) { this.video_url = video_url; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

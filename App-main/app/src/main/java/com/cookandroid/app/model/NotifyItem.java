package com.cookandroid.app.model;

public class NotifyItem {
    private String id;
    private String title;
    private String content;
    private String time;
    private boolean read;

    public NotifyItem() {}

    public NotifyItem(String title, String content, String time, boolean read) {
        this.title = title;
        this.content = content;
        this.time = time;
        this.read = read;
    }

    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getTime() { return time; }

    public String getId() { return id; }

    public boolean isRead() { return read; }

    public void setId(String id) {
        this.id = id;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

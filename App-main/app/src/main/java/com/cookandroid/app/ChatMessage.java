package com.cookandroid.app;

public class ChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_BOT = 1;

    private String message;
    private int senderType;

    public ChatMessage(String message, int senderType) {
        this.message = message;
        this.senderType = senderType;
    }

    public String getMessage() {
        return message;
    }

    public int getSenderType() {
        return senderType;
    }
}

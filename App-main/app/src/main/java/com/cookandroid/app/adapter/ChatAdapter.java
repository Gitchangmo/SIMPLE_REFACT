package com.cookandroid.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cookandroid.app.ChatMessage;
import com.cookandroid.app.R;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ChatMessage> chatList;

    public ChatAdapter(List<ChatMessage> chatList) {
        this.chatList = chatList;
    }

    @Override
    public int getItemViewType(int position) {
        return chatList.get(position).getSenderType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ChatMessage.TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_user, parent, false);
            return new UserViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_bot, parent, false);
            return new BotViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage chat = chatList.get(position);
        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).messageText.setText(chat.getMessage());
        } else if (holder instanceof BotViewHolder) {
            ((BotViewHolder) holder).messageText.setText(chat.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        UserViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.textUserMessage);
        }
    }

    static class BotViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        BotViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.textBotMessage);
        }
    }
}

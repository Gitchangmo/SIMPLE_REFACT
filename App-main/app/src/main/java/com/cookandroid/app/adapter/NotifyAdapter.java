package com.cookandroid.app.adapter;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cookandroid.app.R;
import com.cookandroid.app.model.NotifyItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class NotifyAdapter extends RecyclerView.Adapter<NotifyAdapter.ViewHolder>{
    private List<NotifyItem> notifyItemList;

    public NotifyAdapter(List<NotifyItem> notifyItemList) {
        this.notifyItemList = notifyItemList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvContent, tvTime, btnSelectAll, btnSelectUnread;

        public ViewHolder(View view) {
            super(view);
            tvTitle = view.findViewById(R.id.tvNotiTitle);
            tvContent = view.findViewById(R.id.tvNotiBody);
            tvTime = view.findViewById(R.id.tvNotiTime);
            btnSelectAll = view.findViewById(R.id.btnSelectAll);
            btnSelectUnread = view.findViewById(R.id.btnSelectUnread);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notify, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotifyItem item = notifyItemList.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvContent.setText(item.getContent());
        holder.tvTime.setText(item.getTime());

        // 읽지 않은 메시지 스타일 강조
        if (!item.isRead()) {
            holder.itemView.setBackgroundColor(Color.parseColor("#F1F6FF")); // 연한 파랑
            //holder.title.setTypeface(null, Typeface.BOLD);
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE);
            //holder.title.setTypeface(null, Typeface.NORMAL);
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        holder.itemView.setOnClickListener(v -> {
            if (!item.isRead()) {
                FirebaseFirestore.getInstance()
                        .collection("users").document(uid)
                        .collection("notifies").document(item.getId())
                        .update("read", true)
                        .addOnSuccessListener(aVoid -> {
                            item.setRead(true);
                            notifyItemChanged(holder.getAdapterPosition());
                        });
            }
            // (필요하다면 상세보기 등 다른 동작 추가)
        });
    }

    @Override
    public int getItemCount() {
        return notifyItemList.size();
    }
}

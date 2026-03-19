package com.cookandroid.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cookandroid.app.R;
import com.cookandroid.app.model.LogItem;

import java.util.ArrayList;
import java.util.List;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.ViewHolder> {
    private List<LogItem> logList;

    public LogAdapter(List<LogItem> logList) {
        this.logList = logList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvLogTitle, tvLogBody, tvLogTime;

        public ViewHolder(View v) {
            super(v);
            tvLogTitle = v.findViewById(R.id.tvLogTitle);
            tvLogBody = v.findViewById(R.id.tvLogBody);
            tvLogTime = v.findViewById(R.id.tvLogTime);
        }
    }

    @NonNull
    @Override
    public LogAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull LogAdapter.ViewHolder holder, int position) {
        LogItem item = logList.get(position);
        String action = "";
        if (item.getAction().equals("put")) { action = "추가"; }
        else if (item.getAction().equals("consume")) { action = "소비"; }
        else if (item.getAction().equals("delete")) { action = "삭제"; }
        else if (item.getAction().equals("dispose")) { action = "폐기"; }
        String title = action;
        String body = item.getFoodName() + " " +  item.getQuantity() + "개가 " + action + "되었습니다.";

        holder.tvLogTitle.setText(title);
        holder.tvLogBody.setText(body);
        holder.tvLogTime.setText(item.getTime());

    }

    @Override
    public int getItemCount() {
        return logList.size();
    }
}

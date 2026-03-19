package com.cookandroid.app.Fragment;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cookandroid.app.R;
import com.cookandroid.app.adapter.LogAdapter;
import com.cookandroid.app.model.LogItem;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LogHistoryFragment extends DialogFragment {
    private List<LogItem> logList = new ArrayList<>();
    private List<LogItem> filteredList = new ArrayList<>();
    private LogAdapter adapter;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_log_history, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.logRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new LogAdapter(filteredList);
        recyclerView.setAdapter(adapter);

        loadLogsFromFirestore();
        setupFilterButtons(view);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    (int)(getResources().getDisplayMetrics().widthPixels * 0.95),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void loadLogsFromFirestore() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance()
                .collection("fridges").document(uid).collection("history")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot) {
                        LogItem item = doc.toObject(LogItem.class);
                        item.setAction(doc.getString("action"));
                        item.setFoodName(doc.getString("foodName"));
                        item.setQuantity(doc.getLong("quantity").intValue());
                        Timestamp timestamp = doc.getTimestamp("timestamp");
                        String timeAgo = null;
                        if (timestamp != null) {
                            timeAgo = getTimeAgo(timestamp.toDate());
                            // date를 이용해서 '몇분전', '몇시간전' 변환
                        }
                        item.setTime(timeAgo);
                        logList.add(item);
                    }
                    filteredList.clear();
                    filteredList.addAll(logList);
                    adapter.notifyDataSetChanged();
                });
    }

    // 각 버튼 별 로그 구분하여 해당 동작 로그 리스트함
    private void setupFilterButtons(View view) {
        view.findViewById(R.id.btnAll).setOnClickListener(v -> filterLogs("all"));
        view.findViewById(R.id.btnAdd).setOnClickListener(v -> filterLogs("put"));
        view.findViewById(R.id.btnConsume).setOnClickListener(v -> filterLogs("consume"));
        view.findViewById(R.id.btnDispose).setOnClickListener(v -> filterLogs("dispose"));
        view.findViewById(R.id.btnDelete).setOnClickListener(v -> filterLogs("delete"));
    }

    private void filterLogs(String type) {
        filteredList.clear();
        if ("all".equals(type)) {
            filteredList.addAll(logList);
        } else {
            for (LogItem item : logList) {
                if (type.equals(item.getAction())) {
                    filteredList.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private String getTimeAgo(Date date) {
        long diffMillis = new Date().getTime() - date.getTime();
        long minutes = diffMillis / (60 * 1000);
        if (minutes < 60) return minutes + "분 전";
        long hours = minutes / 60;
        if (hours < 24) return hours + "시간 전";
        long days = hours / 24;
        return days + "일 전";
    }
}

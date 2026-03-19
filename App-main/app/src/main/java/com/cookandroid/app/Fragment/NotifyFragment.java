package com.cookandroid.app.Fragment;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cookandroid.app.R;
import com.cookandroid.app.adapter.NotifyAdapter;
import com.cookandroid.app.model.NotifyItem;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NotifyFragment extends Fragment {
    private RecyclerView recyclerView;
    private NotifyAdapter adapter;
    private List<NotifyItem> notiList = new ArrayList<>();
    private List<NotifyItem> displayList = new ArrayList<>();
    private FirebaseFirestore db;
    TextView btnSelectAll, btnSelectUnread, btnDeleteAll;
    private String currentTab = "ALL";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_notify_list, container, false);

        recyclerView = v.findViewById(R.id.rvNotification);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();

        adapter = new NotifyAdapter(displayList);
        recyclerView.setAdapter(adapter);

        btnSelectAll = v.findViewById(R.id.btnSelectAll);
        btnSelectUnread = v.findViewById(R.id.btnSelectUnread);
        btnDeleteAll = v.findViewById(R.id.btnDeleteAll);

        btnSelectAll.setOnClickListener(view -> switchTab("ALL"));
        btnSelectUnread.setOnClickListener(view -> switchTab("UNREAD"));
        btnDeleteAll.setOnClickListener(view -> deleteAllNotifications());

        updateTabStyle();

        loadNotificationData();

        return v;
    }

    // 알림 전체 삭제 함수
    private void deleteAllNotifications() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("users").document(uid).collection("notifies")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // 알림 전부 삭제
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        doc.getReference().delete();
                    }
                    Toast.makeText(getContext(),
                            "알림 삭제 완료!",
                            Toast.LENGTH_SHORT).show();
                    // 리스트 비우고 UI 갱신함
                    notiList.clear();
                    displayList.clear();
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("NotifyFragment",
                            "알림 전체 삭제 실패",
                            e);
                });
    }

    private void switchTab(String tab) {
        currentTab = tab;
        updateTabStyle();
        filterList();
    }

    private void updateTabStyle() {
        // 간단 예시. 커스텀 색상/스타일로 바꿔도 됨
        if ("ALL".equals(currentTab)) {
            btnSelectAll.setTypeface(null, Typeface.BOLD);
            btnSelectAll.setTextColor(Color.BLACK);
            btnSelectUnread.setTypeface(null, Typeface.NORMAL);
            btnSelectUnread.setTextColor(Color.GRAY);
        } else {
            btnSelectUnread.setTypeface(null, Typeface.BOLD);
            btnSelectUnread.setTextColor(Color.BLACK);
            btnSelectAll.setTypeface(null, Typeface.NORMAL);
            btnSelectAll.setTextColor(Color.GRAY);
        }
    }

    private void filterList() {
        displayList.clear();
        if ("ALL".equals(currentTab)) {
            displayList.addAll(notiList);
        } else {
            for (NotifyItem item : notiList) {
                if (!item.isRead()) {
                    displayList.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void loadNotificationData() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("users").document(uid).collection("notifies")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    notiList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        NotifyItem item = doc.toObject(NotifyItem.class);
                        item.setId(doc.getId());
                        item.setTitle(doc.getString("title"));
                        item.setContent(doc.getString("body"));
                        item.setRead(doc.getBoolean("read"));
                        Timestamp timestamp = doc.getTimestamp("timestamp");
                        String timeAgo = null;
                        if (timestamp != null) {
                            timeAgo = getTimeAgo(timestamp.toDate());
                            // date를 이용해서 '몇분전', '몇시간전' 변환
                        }
                        item.setTime(timeAgo);
                        notiList.add(item);
                    }
                    filterList();
                })
                .addOnFailureListener(e -> {
                    Log.e("NotifyFragment",
                            "알림 로드 실패",
                            e);
                });
    }
    public static String getTimeAgo(Date date) {
        long now = System.currentTimeMillis();
        long diff = now - date.getTime();

        long minute = 60 * 1000;
        long hour = 60 * minute;
        long day = 24 * hour;

        if (diff < minute)
            return "방금 전";
        else if (diff < hour)
            return (diff / minute) + "분 전";
        else if (diff < day)
            return (diff / hour) + "시간 전";
        else
            return (diff / day) + "일 전";
    }
}




package com.cookandroid.app.Fragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;

import java.util.Arrays;
import java.util.Calendar;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.formatter.IFillFormatter;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.components.XAxis;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.cookandroid.app.R;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class FoodFragmentChart extends Fragment {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    int totalDiscarded = 0; // 폐기율
    // 현재 날짜 가져오기
    Calendar calendar = Calendar.getInstance();
    private int year;
    private int month;
    private View view;
    private static final LinkedHashMap<String, Integer> CATEGORY_COLOR = new LinkedHashMap<>();
    static {
        CATEGORY_COLOR.put("가공/유제품", Color.parseColor("#4CAF50"));
        CATEGORY_COLOR.put("곡물",       Color.parseColor("#FFEB3B"));
        CATEGORY_COLOR.put("과일",       Color.parseColor("#F44336"));
        CATEGORY_COLOR.put("면",         Color.parseColor("#2196F3"));
        CATEGORY_COLOR.put("빵/떡",      Color.parseColor("#FF9800"));
        CATEGORY_COLOR.put("음료/주류",   Color.parseColor("#9C27B0"));
        CATEGORY_COLOR.put("채소",       Color.parseColor("#3F51B5"));
        CATEGORY_COLOR.put("콩/견과류",   Color.parseColor("#00BCD4"));
        CATEGORY_COLOR.put("해산물",     Color.parseColor("#009688"));
        CATEGORY_COLOR.put("조미료/양념", Color.parseColor("#8BC34A"));
        CATEGORY_COLOR.put("고기",       Color.parseColor("#E91E63"));
        CATEGORY_COLOR.put("기타",       Color.parseColor("#9E9E9E"));
    }

    // 달 시작, 끝 날짜 계산
    private Timestamp getStartOfMonth(int year, int month) {
        Calendar c = Calendar.getInstance(); // 현재 달 정보 가져오기 : 월-일-시-분-초
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month - 1); // Calendar.MONTH: 0~11이기 때문에 인덱스값 맞추기 위해 -1
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        return new Timestamp(c.getTime());
    }

    private Timestamp getStartOfNextMonth(int year, int month) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month - 1); // Calendar.MONTH: 0~11
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.add(Calendar.MONTH, 1); // 다음 달 정보 가져오기 : 월-일-시-분-초
        return new Timestamp(c.getTime());
    }

    public FoodFragmentChart() {
        super(R.layout.fragment_food_chart);

    }

    // 냉장고에 넣은 음식 수 (put − delete)
    public Task<Integer> getPutQuantity(int year, int month) {
        Timestamp start = getStartOfMonth(year, month), end = getStartOfNextMonth(year, month);
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Task<QuerySnapshot> putTask = db.collection("fridges")
                .document(userID).collection("history")
                .whereEqualTo("action", "put")
                .whereGreaterThanOrEqualTo("timestamp", start)
                .whereLessThan("timestamp", end)
                .get(); // put 동작 데이터 스냅샷 저장

        Task<QuerySnapshot> delTask = db.collection("fridges")
                .document(userID).collection("history")
                .whereEqualTo("action", "delete")
                .whereGreaterThanOrEqualTo("timestamp", start)
                .whereLessThan("timestamp", end)
                .get(); // delete 동작 데이터 스냅샷 저장

        return Tasks.whenAllSuccess(putTask, delTask)
                .continueWith(task -> {
                    List<?> results = task.getResult();
                    QuerySnapshot putSnap = ((QuerySnapshot) results.get(0)); // putTask 스냅샷
                    QuerySnapshot delSnap = ((QuerySnapshot) results.get(1)); // delTask 스냅샷

                    int putNum = 0;
                    for (DocumentSnapshot doc : putSnap.getDocuments()) {
                        Long q = doc.getLong("quantity"); // 넣은 음식 수
                        if (q != null) putNum += q;
                    }
                    int delNum = 0;
                    for (DocumentSnapshot d : delSnap.getDocuments()) {
                        Long q = d.getLong("quantity"); // 삭제한 음식 수
                        if (q != null) delNum += q;
                    }
                    Log.d("FoodChart", "DEBUG putDocs=" + putSnap.size()
                            + " putNum=" + putNum
                            + " delDocs=" + delSnap.size()
                            + " delNum=" + delNum);
                    return putNum - delNum; // 넣은 음식 수에서 삭제한 음식 수 뺀 수량 반환
                });
    }

    // 음식 소비율을 리턴
    public Task<Integer> getConsumptionRate(int year, int month) {
        Timestamp start = getStartOfMonth(year, month), end = getStartOfNextMonth(year, month);

        Task<Integer> putQtyTask = getPutQuantity(year, month); // 넣은 음식 수량 가져오기
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Task<QuerySnapshot> consumeSnap = db.collection("fridges").document(userID)
                .collection("history")
                .whereEqualTo("action", "consume")
                .whereGreaterThanOrEqualTo("timestamp", start)
                .whereLessThan("timestamp", end)
                .get(); // 소비한 데이터 조회
        return Tasks.whenAllSuccess(putQtyTask, consumeSnap) // 둘다 성공했을 시,
                .continueWith(task -> {
                    List<?> results = task.getResult(); // 결과 가져오기 각 스냅샷에서

                    int putQty = (Integer) results.get(0); // 넣은 음식 수

                    QuerySnapshot snap = (QuerySnapshot) results.get(1); // consumeSnap 스냅샷 저장
                    int consumedSum = 0;
                    for (DocumentSnapshot d : snap.getDocuments()) { // 각 문서에서 수량 조회
                        Long q = d.getLong("quantity");
                        if (q != null) consumedSum += q.intValue(); // 총 소비 수량
                    }

                    // 퍼센트 계산 (0일 땐 0)
                    return (putQty == 0)
                            ? 0
                            : (int) (consumedSum * 100.0 / putQty); // 퍼센트 구하기 소비율
                });
    }

    // 유통기한 내 소비한 음식 수
    public Task<Integer> getConsumeQuantity(int year, int month) {
        Timestamp start = getStartOfMonth(year, month), end = getStartOfNextMonth(year, month);
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Task<QuerySnapshot> putTask = db.collection("fridges")
                .document(userID).collection("history")
                .whereEqualTo("action", "consume")
                .whereGreaterThanOrEqualTo("timestamp", start)
                .whereLessThan("timestamp", end)
                .get(); // 소비 동작의 데이터 조회

        return Tasks.whenAllSuccess(putTask)
                .continueWith(task -> {
                    List<?> results = task.getResult();
                    QuerySnapshot conSnap = ((QuerySnapshot) results.get(0));

                    int conNum = 0;
                    for (DocumentSnapshot doc : conSnap.getDocuments()) {
                        Long q = doc.getLong("quantity");
                        if (q != null) conNum += q;
                    }
                    return conNum; // 소비한 음식 수량 합해서 반환
                });
    }

    // 유통기한 내 소비 못한 음식 수
    public Task<Integer> getDisposeQuantity(int year, int month) {
        Timestamp start = getStartOfMonth(year, month), end = getStartOfNextMonth(year, month);
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Task<QuerySnapshot> putTask = db.collection("fridges")
                .document(userID).collection("history")
                .whereEqualTo("action", "dispose")
                .whereGreaterThanOrEqualTo("timestamp", start)
                .whereLessThan("timestamp", end)
                .get(); // 폐기 동작 데이터 조회

        return Tasks.whenAllSuccess(putTask)
                .continueWith(task -> {
                    List<?> results = task.getResult(); // 쿼리 결과 받아서 저장 각 쿼리들 전체 결과를 저장하는 '리스트'임
                    QuerySnapshot disSnap = ((QuerySnapshot) results.get(0)); // 리스트에서 0번째 인덱스의 전체결과 가져오기

                    int disNum = 0;
                    for (DocumentSnapshot doc : disSnap.getDocuments()) {
                        Long q = doc.getLong("quantity");
                        if (q != null) disNum += q;
                    }
                    return disNum; // 폐기한 음식 수량 합 반환
                });
    }

    // 유통기한 못 지킨 카테고리별 합계를 Task<Map<카테고리, 수량>>으로로 리턴
    public Task<Map<String, Integer>> getDiscardedByCategory(int year, int month) {
        Timestamp start = getStartOfMonth(year, month);
        Timestamp end = getStartOfNextMonth(year, month);

        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        return db.collection("fridges").document(userID)
                .collection("history")
                .whereEqualTo("action", "dispose")
                .whereGreaterThanOrEqualTo("timestamp", start)
                .whereLessThan("timestamp", end)
                .get()
                .continueWith(task -> {
                    Map<String, Integer> map = new LinkedHashMap<>();
                    for (DocumentSnapshot d : task.getResult().getDocuments()) {
                        String cat = d.getString("category");
                        Integer q = d.getLong("quantity").intValue();
                        if (cat == null || q == null) continue;
                        map.put(cat, map.getOrDefault(cat, 0) + q);
                    }
                    return map;
                });
    }

    // 월별 소비, 폐기 데이터 집계 (1~12월)
    private Task<List<List<Entry>>> fetchMonthlyEntries(int year) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, Calendar.JANUARY, 1, 0, 0, 0); // 올해 1월1일00시00분00초
        Timestamp start = new Timestamp(cal.getTime()); // Firebase용 Timestamp로 변환
        cal.add(Calendar.YEAR, 1); // +1년해서 내년 1월1일00시00분00초
        Timestamp end = new Timestamp(cal.getTime()); // Firebase용 Timestamp로 변환

        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        return db.collection("fridges").document(userID).collection("history")
                .whereIn("action", Arrays.asList("consume", "dispose")) // 소비, 폐기 동작만
                .whereGreaterThanOrEqualTo("timestamp", start)
                .whereLessThan("timestamp", end) // 해당 년도의 데이터만 조회
                .get()
                .continueWith(task -> {
                    int[] cons = new int[13];
                    int[] disc = new int[13];
                    for (DocumentSnapshot d : task.getResult().getDocuments()) {
                        String act = d.getString("action"); // 동작 구분
                        int qty    = d.getLong("quantity").intValue(); // 수량 가져오기
                        Date date  = d.getTimestamp("timestamp").toDate(); // 발생한 시간(일시) 가져오기
                        cal.setTime(date); // 캘린더를 해당 일시로 변경
                        int m = cal.get(Calendar.MONTH) + 1;
                        if ("consume".equals(act)) cons[m] += qty; // 월별 수량 합계
                        else                       disc[m] += qty; // 월별 수량 합계
                    }
                    List<Entry> consumedEntries = new ArrayList<>();
                    List<Entry> discardedEntries = new ArrayList<>();
                    for (int m = 1; m <= 12; m++) {
                        consumedEntries.add(new Entry(m, cons[m]));
                        discardedEntries.add(new Entry(m, disc[m]));
                    } // 월별 소비, 폐기 Entry 생성
                    return Arrays.asList(consumedEntries, discardedEntries);
                });
    }

    public void loadData(){
        TextView foodInCount = view.findViewById(R.id.foodInCount);
        TextView foodTotalCount = view.findViewById(R.id.foodTotalCount);
        TextView foodEatenCount = view.findViewById(R.id.foodEatenCount);
        TextView foodExpiredCount = view.findViewById(R.id.foodExpiredCount);

        // 냉장고에 넣은 음식 수
        getPutQuantity(year, month).addOnSuccessListener(putQty -> {
                    Log.d("FoodChart", "putQty = " + putQty);
                    foodInCount.setText(String.valueOf(putQty));
                })
                .addOnFailureListener(e -> {
                    foodInCount.setText("0");
                    Log.e("FoodChart", "넣은 수량 로드 실패", e);
                });

        // 냉장고 음식 소비율
        getConsumptionRate(year, month).addOnSuccessListener(consumQty -> {
                    foodTotalCount.setText(String.valueOf(consumQty) + "%");
                })
                .addOnFailureListener(e -> {
                    foodTotalCount.setText("0%");
                    Log.e("FoodChart", "음식 소비율 로드 실패", e);
                });

        // 유통기한 내 소비한 음식 수
        getConsumeQuantity(year, month).addOnSuccessListener(conQty -> {
                    foodEatenCount.setText(String.valueOf(conQty));
                })
                .addOnFailureListener(e -> {
                    foodEatenCount.setText("0");
                    Log.e("FoodChart", "기한 내 음식 소비 수 로드 실패", e);
                });

        // 유통기한 내 소비하지 못한 음식 수(폐기한 음식 수)
        getDisposeQuantity(year, month).addOnSuccessListener(disQty -> {
                    foodExpiredCount.setText(String.valueOf(disQty));
                })
                .addOnFailureListener(e -> {
                    foodExpiredCount.setText("0");
                    Log.e("FoodChart", "기한 내 소비 못 한 음식 수 로드 실패", e);
                });
    }

    private void updatePieChart(int year, int month) {
        PieChart pieChart = view.findViewById(R.id.pieChart);


// 1. 데이터 로드
        getDiscardedByCategory(year, month)
                .addOnSuccessListener(map -> {
                    List<PieEntry> entries = new ArrayList<>();
                    List<Integer> colors   = new ArrayList<>();
                    totalDiscarded = 0;
                    for (Map.Entry<String, Integer> e : CATEGORY_COLOR.entrySet()) {
                        String category = e.getKey();
                        Integer qty     = map.get(category);
                        if (qty != null && qty > 0) {
                            entries.add(new PieEntry(qty, category));
                            colors.add(e.getValue());
                            totalDiscarded += qty;
                        }
                    }

// 2. 데이터셋 구성
                    PieDataSet dataSet = new PieDataSet(entries, "유통기한 만료 식품군");

                    dataSet.setColors(colors);

                    // 기본 센터 텍스트 계산 (폐기 수량 + 퍼센트)
                    getPutQuantity(year, month)
                            .addOnSuccessListener(putQty -> {
                                double discardRate = putQty == 0
                                        ? 0
                                        : (totalDiscarded * 100.0 / putQty);
                                String titleLine = "폐기된 음식 수\n";
                                String countLine = totalDiscarded + "개 (" + String.format("%.0f%%", discardRate) + ")";
                                SpannableStringBuilder ssb = new SpannableStringBuilder();
                                ssb.append(titleLine);

                                int start = ssb.length();
                                ssb.append(countLine);
                                ssb.setSpan(
                                        new StyleSpan(Typeface.BOLD),
                                        start, ssb.length(),
                                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                );
                                ssb.setSpan(
                                        new RelativeSizeSpan(1.5f),
                                        start, ssb.length(),
                                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                );

                                String defaultCenter = totalDiscarded + "개\n("
                                        + String.format("%.0f%%", discardRate) + ")";

                                dataSet.setDrawValues(false);
                                pieChart.setDrawEntryLabels(false);

                                pieChart.setCenterText(ssb);
                                pieChart.setDrawCenterText(true);

                                dataSet.setValueTextSize(14f);
                                dataSet.setValueTextColor(android.graphics.Color.WHITE);

// 3. 파이 데이터 구성
                                PieData data = new PieData(dataSet);

// 4. 파이차트에 데이터 연결
                                pieChart.setData(data);
                                pieChart.getLegend().setEnabled(false);

// 5. 기타 설정
                                pieChart.setUsePercentValues(true); // 퍼센트 표시
                                pieChart.setEntryLabelColor(android.graphics.Color.BLACK);
                                pieChart.setCenterTextSize(18f);
                                pieChart.setDrawHoleEnabled(true); // 가운데 구멍
                                pieChart.setHoleRadius(40f);       // 가운데 구멍 크기
                                pieChart.setTransparentCircleRadius(45f);

                                // 도넛 형태 설정
                                pieChart.setDrawHoleEnabled(true);        // 중앙 구멍 보이기
                                pieChart.setHoleRadius(70f);              // 구멍 크기 조정 (기본보다 크게)
                                pieChart.setTransparentCircleRadius(75f); // 투명 원 크기 (외곽 흐림 효과)

                                // 중앙에 텍스트 표현
                                pieChart.setCenterTextColor(Color.BLACK); // 글자 색
                                pieChart.setHighlightPerTapEnabled(true);

                                // 조각 간 간격
                                dataSet.setSliceSpace(2f);
                                Description desc = new Description();
                                desc.setText(""); // 설명 텍스트 제거
                                pieChart.setDescription(desc);
                                pieChart.invalidate();
                                pieChart.animateY(1000); // 애니메이션

                                // 각 카테고리 클릭 시
                                pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
                                    @Override
                                    public void onValueSelected(Entry e, Highlight h) {
                                        String label = ((PieEntry) e).getLabel();
                                        // 볼드/사이즈 조정
                                        SpannableString ss = new SpannableString(label);
                                        ss.setSpan(
                                                new StyleSpan(Typeface.BOLD),
                                                0, label.length(),
                                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                        );
                                        ss.setSpan(
                                                new RelativeSizeSpan(1.5f),
                                                0, label.length(),
                                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                        );
                                        pieChart.setCenterText(ss);
                                    }

                                    @Override
                                    public void onNothingSelected() {
                                        pieChart.setCenterText(ssb);
                                    }
                                });
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("FoodChart", "폐기 카테고리 집계 실패", e);
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_food_chart, container, false);

        TextView tvMonth = view.findViewById(R.id.tvMonth);
//        TextView foodInCount = view.findViewById(R.id.foodInCount);
//        TextView foodTotalCount = view.findViewById(R.id.foodTotalCount);
//        TextView foodEatenCount = view.findViewById(R.id.foodEatenCount);
//        TextView foodExpiredCount = view.findViewById(R.id.foodExpiredCount);
        TextView btnListLog = view.findViewById(R.id.btnListLog);
        ImageView ivPrevMonth = view.findViewById(R.id.ivPrevMonth);
        ImageView ivNextMonth = view.findViewById(R.id.ivNextMonth);

        // 현재 날짜 가져오기
        Calendar calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH) + 1; // 0부터 시작하므로 +1

        // 텍스트 적용
        tvMonth.setText(year + "년 " + month + "월");

        // 이전 달 조회
        ivPrevMonth.setOnClickListener(v -> {
            if (month == 1) {
                year -= 1;
                month = 12;
            } else {
                month -= 1;
            }
            tvMonth.setText(year + "년 " + month + "월");
            loadData();
            updatePieChart(year, month);
        });

        // 다음 달 조회
        ivNextMonth.setOnClickListener(v -> {
            if (month == 12) {
                year += 1;
                month = 1;
            } else {
                month += 1;
            }
            tvMonth.setText(year + "년 " + month + "월");
            loadData();
            updatePieChart(year, month);
        });

        loadData(); // 수치 수량 불러오기
        updatePieChart(year, month); // 파이차트 불러오기

        btnListLog.setOnClickListener(v -> {
            new LogHistoryFragment().show(getParentFragmentManager(), "LogSheet");
        });


        // 월별 분석 부분
        LineChart lineChart = view.findViewById(R.id.lineChart);

        year = Calendar.getInstance().get(Calendar.YEAR);
        // ✅ 데이터 준비 (x = 월, y = 수치)
        fetchMonthlyEntries(year)
                .addOnSuccessListener(lists -> {
                    List<Entry> consumedEntries = lists.get(0);
                    List<Entry> discardedEntries = lists.get(1);

// === 정렬 (LineDataSet 만들기 전에!) ===
                    Collections.sort(consumedEntries, (e1, e2) -> Float.compare(e1.getX(), e2.getX()));
                    Collections.sort(discardedEntries, (e1, e2) -> Float.compare(e1.getX(), e2.getX()));

// ✅ 데이터셋 구성/ 표현 방식 세팅
                    LineDataSet consumedDataSet = new LineDataSet(consumedEntries, "유통기한 내 소비");
                    consumedDataSet.setColor(Color.parseColor("#6495ED")); // 파랑
                    consumedDataSet.setDrawFilled(true);

                    consumedDataSet.setDrawCircles(false);
                    consumedDataSet.setDrawValues(false);
                    consumedDataSet.setFillFormatter((dataSet1, dataProvider) -> 0f); // ✅ 추가

                    LineDataSet wastedDataSet = new LineDataSet(discardedEntries, "유통기한 내 소비하지 못함");
                    wastedDataSet.setColor(Color.parseColor("#FF7F7F")); // 빨강
                    wastedDataSet.setDrawFilled(true);

                    wastedDataSet.setDrawCircles(false);
                    wastedDataSet.setDrawValues(false);
                    wastedDataSet.setFillFormatter((dataSet1, dataProvider) -> 0f); // ✅ 추가

                    Drawable blueGradient = ContextCompat.getDrawable(requireContext(), R.drawable.gradient_blue);
                    Drawable redGradient = ContextCompat.getDrawable(requireContext(), R.drawable.gradient_red);

                    consumedDataSet.setFillDrawable(blueGradient);
                    wastedDataSet.setFillDrawable(redGradient);

                    // 축 세팅
                    lineChart.getDescription().setEnabled(false); // Description 제거
                    lineChart.getAxisLeft().setEnabled(false); // 왼쪽 Y축 제거
                    lineChart.getAxisRight().setEnabled(false); // 오른쪽 Y축 제거

                    lineChart.getXAxis().setDrawGridLines(false); // X축 배경선 제거
                    lineChart.getAxisLeft().setDrawGridLines(false);
                    lineChart.getAxisRight().setDrawGridLines(false);
// ✅ Y축 최소값 0으로 고정
                    lineChart.getAxisLeft().setAxisMinimum(0f);
                    lineChart.getAxisRight().setAxisMinimum(0f);
                    lineChart.getLegend().setEnabled(false); // 범례 숨기고 싶으면 false

                    XAxis xAxis = lineChart.getXAxis();
                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                    xAxis.setGranularity(1f);
                    xAxis.setLabelCount(7, true);
                    xAxis.setTextColor(Color.BLACK);
                    xAxis.setDrawAxisLine(false); // 축 선 제거

                    xAxis.setValueFormatter(new ValueFormatter() {
                        @Override
                        public String getFormattedValue(float value) {
                            return ((int) value) + "월";
                        }
                    });

// ✅ 차트에 데이터 설정
                    LineData lineData = new LineData(consumedDataSet, wastedDataSet);
                    lineChart.setData(lineData);
                    lineChart.invalidate(); // 차트 갱신
                })
                .addOnFailureListener(e -> Log.e("FoodChart", "월별 분석 실패", e));
        return view;
    }
}

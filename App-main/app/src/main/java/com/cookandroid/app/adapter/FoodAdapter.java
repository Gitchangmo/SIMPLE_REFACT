package com.cookandroid.app.adapter;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.cookandroid.app.R;
import com.cookandroid.app.model.Food;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Consumer;

public class FoodAdapter
        extends RecyclerView.Adapter<FoodAdapter.ViewHolder> {

    private List<Food> foodList;
    private List<Food> copyList;
    private Context context;
    private FirebaseFirestore db;

    public FoodAdapter(Context context, List<Food> foodList) {
        this.context = context;
        this.foodList = new ArrayList<>(foodList);
        this.copyList = new ArrayList<>(foodList);
        db = FirebaseFirestore.getInstance();
    }

    // ValueFragment에서 파싱한 FoodList를 여기서 생성한 리스트들에 매핑함
    public void setData(List<Food> list) {
        foodList.clear();
        foodList.addAll(list);
        copyList.clear();
        copyList.addAll(list);
        notifyDataSetChanged();
    }

    // 내 보관 중인 재고에서 검색 기능
    public void filter(String text) {
        copyList.clear();
        if (text == null || text.isEmpty()) {
            copyList.addAll(foodList);
        } else {
            text = text.toLowerCase();
            for (Food food : foodList) {
                if (food.getName().toLowerCase().contains(text)) {
                    copyList.add(food);
                }
            }
        }
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView foodImageView;
        TextView  foodName, foodInfo, foodQuantity;
        View bar1, bar2, bar3, bar4;
        ImageButton deleteButton;

        ViewHolder(View v) {
            super(v);
            foodImageView = v.findViewById(R.id.food_image);
            foodName      = v.findViewById(R.id.food_name);
            foodInfo      = v.findViewById(R.id.food_info);
            foodQuantity  = v.findViewById(R.id.food_quantity);
            deleteButton = itemView.findViewById(R.id.btn_delete);


            // 막대 4개 연결
            bar1 = v.findViewById(R.id.bar1);
            bar2 = v.findViewById(R.id.bar2);
            bar3 = v.findViewById(R.id.bar3);
            bar4 = v.findViewById(R.id.bar4);
        }
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                         int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_food, parent, false);
        return new ViewHolder(v);
    }

    // 로그 생성 함수 동작 별 구분. 추가만 됨
    @Nullable
    public void logHistory(
            String foodDocId,
            String foodName,
            String category,
            String action,       // "put", "remove", "use", "discard", "delete"
            int quantity,
            int remainAfter,
            @Nullable String recipeId,
            @Nullable String memo
    ) {
        Map<String, Object> log = new HashMap<>();
        log.put("foodDocId", foodDocId);
        log.put("foodName", foodName);
        log.put("category", category);
        log.put("action", action);        // 예: "put", "remove", "use", "discard", "delete"
        log.put("quantity", quantity);    // +N 또는 -N
        log.put("remainAfter", remainAfter);
        log.put("timestamp", FieldValue.serverTimestamp());
        if (recipeId != null) log.put("recipeId", recipeId);
        if (memo != null)     log.put("memo", memo);

        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Firestore에 저장
        FirebaseFirestore.getInstance()
                .collection("fridges").document(userID)
                .collection("history")
                .add(log)
                .addOnSuccessListener(ref -> Log.d("History", "저장 성공"))
                .addOnFailureListener(e -> Log.e("History", "저장 실패: " + e.getMessage()));
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder,
                                 int position) {
        Food food = copyList.get(position);
        holder.foodName.setText(food.getName());
        if (food.getStorage().equals("냉동")) {
            String addedDate = food.getAddedDate();
            String storageDay = calculateFrozenDDay(addedDate);
            holder.foodInfo.setText("냉동\n" + storageDay);  // 줄바꿈으로 아래에 표시
        }
        else {
            holder.foodInfo.setText(food.getDaysLeft());
        }
        holder.foodQuantity
                .setText("수량: " + food.getQuantity());

        String daysLeft = food.getDaysLeft();

        int colorRed    = Color.parseColor("#C78B7A");
        int colorYellow = Color.parseColor("#D7C388");
        int colorGreen  = Color.parseColor("#9FBA98");
        int colorBlue   = Color.parseColor("#93A8C8");
        int colorGray   = Color.parseColor("#E0E0E0");


        // 우선 모두 기본 색상
        holder.bar1.setBackgroundColor(colorGray);
        holder.bar2.setBackgroundColor(colorGray);
        holder.bar3.setBackgroundColor(colorGray);
        holder.bar4.setBackgroundColor(colorGray);



        if (daysLeft == null || daysLeft.equals("-")) {
            // 유통기한 입력 없음 - 기본 색상 유지
        } else if (daysLeft.startsWith("D+")) {
            // 유통기한 지남 -> bar4 빨강
            holder.bar1.setBackgroundColor(colorGray);
            holder.bar2.setBackgroundColor(colorGray);
            holder.bar3.setBackgroundColor(colorGray);
            holder.bar4.setBackgroundColor(colorRed);

        } else if (daysLeft.equals("D-DAY")) {
            // 오늘까지 유통기한 -> bar4 빨강
            holder.bar1.setBackgroundColor(colorGray);
            holder.bar2.setBackgroundColor(colorGray);
            holder.bar3.setBackgroundColor(colorGray);
            holder.bar4.setBackgroundColor(colorRed);

        } else if (daysLeft.startsWith("D-")) {
            try {
                int dayValue = Integer.parseInt(daysLeft.replace("D-", ""));
                if (dayValue <= 3) {
                    // D-3 이하 -> bar4 빨강
                    holder.bar1.setBackgroundColor(colorGray);
                    holder.bar2.setBackgroundColor(colorGray);
                    holder.bar3.setBackgroundColor(colorGray);
                    holder.bar4.setBackgroundColor(colorRed);
                } else if (dayValue <= 7) {
                    // D-4 ~ D-7 -> bar3, bar4 노랑
                    holder.bar1.setBackgroundColor(colorGray);
                    holder.bar2.setBackgroundColor(colorGray);
                    holder.bar3.setBackgroundColor(colorYellow);
                    holder.bar4.setBackgroundColor(colorYellow);

                } else if (dayValue <= 14) {
                    // D-8 ~ D-14 -> bar2, bar3, bar4 초록
                    holder.bar1.setBackgroundColor(colorGray);
                    holder.bar2.setBackgroundColor(colorGreen);
                    holder.bar3.setBackgroundColor(colorGreen);
                    holder.bar4.setBackgroundColor(colorGreen);

                } else {
                    // D-15 이상 -> bar1 ~ bar4 모두 파랑
                    holder.bar1.setBackgroundColor(colorBlue);
                    holder.bar2.setBackgroundColor(colorBlue);
                    holder.bar3.setBackgroundColor(colorBlue);
                    holder.bar4.setBackgroundColor(colorBlue);

                }
            } catch (NumberFormatException e) {
                // 숫자 파싱 실패 시 기본 회색 유지
                holder.bar1.setBackgroundColor(colorGray);
                holder.bar2.setBackgroundColor(colorGray);
                holder.bar3.setBackgroundColor(colorGray);
                holder.bar4.setBackgroundColor(colorGray);
            }
        } else {
            // 위 조건에 안 맞는 경우 기본 회색 유지
            holder.bar1.setBackgroundColor(colorGray);
            holder.bar2.setBackgroundColor(colorGray);
            holder.bar3.setBackgroundColor(colorGray);
            holder.bar4.setBackgroundColor(colorGray);
        }

        Log.d("FoodAdapter", "imageName=" + food.getImageName());

        // imageName 받아와서 해당 이미지에 리소스ID 매핑
        @SuppressLint("DiscouragedApi") int resId = context.getResources().getIdentifier(food.getImageName(), "drawable", context.getPackageName());
        Log.d("FoodAdapter", "resId=" + resId);
        if (resId != 0) {
            holder.foodImageView.setImageResource(resId);
        } else {
            // 예비 이미지
            holder.foodImageView.setImageResource(R.drawable.refrigerator);
        }

        // 삭제 버튼
        holder.deleteButton.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            Log.d("FoodAdapter", "삭제버튼 pos=" + pos + ", name=" + food.getName());
            showConsumeDialog(context, food, "delete", consumeCount -> {
                int remain = food.getQuantity() - consumeCount;
                if (remain > 0) {
                    // 수량만 줄임
                    updateFoodQuantity(food.getDocumentId(), remain, pos, null);
                } else {
                    // 0개면 DB에서 완전 삭제
                    deleteFoodItem(food.getDocumentId(), pos, null);
                }
                logHistory(food.getDocumentId(), food.getName(), food.getCategory(), "delete",
                        consumeCount, remain, null, "삭제함");
            });
        });



        // 카드 클릭 시 팝업
        holder.itemView.setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_food_detail, null);
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setView(dialogView);
            AlertDialog dialog = builder.create();

            // 팝업 내부 컴포넌트 연결
            ImageView detailImage = dialogView.findViewById(R.id.detail_image);
            TextView detailName = dialogView.findViewById(R.id.detail_name);
            TextView detailCategory = dialogView.findViewById(R.id.detail_category);
            EditText detailAdded = dialogView.findViewById(R.id.detail_added_date);
            EditText detailExp = dialogView.findViewById(R.id.detail_expiry_date);
            Spinner quantitySpinner = dialogView.findViewById(R.id.detail_quantity_spinner);
            Spinner storageSpinner = dialogView.findViewById(R.id.storage_spinner);
            Button consumeButton = dialogView.findViewById(R.id.consume_button);
            Button trashButton = dialogView.findViewById(R.id.discard_button);
            Button completeButton = dialogView.findViewById(R.id.complete_button);

            final String[] selectedStorage = {food.getStorage() != null ? food.getStorage() : "냉장"};

            // 데이터 세팅
            detailImage.setImageResource(resId);
            detailName.setText(food.getName());
            detailCategory.setText(food.getCategory());

            // 수량 스피너 최대 99개로 수정. 100개 이상은 이미지 깨짐Add commentMore actions
            Integer[] quantityList = new Integer[99];
            for (int i = 0; i < 99; i++) {
                quantityList[i] = i + 1;
            }

            ArrayAdapter<Integer> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, quantityList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            quantitySpinner.setAdapter(adapter);
            quantitySpinner.setSelection(food.getQuantity() - 1);

            // 💡 여기에 보관방식 스피너 코드 추가!
            String[] storageOptions = {"냉장", "냉동", "실온"};
            ArrayAdapter<String> storageAdapter = new ArrayAdapter<>(context,
                    android.R.layout.simple_spinner_item, storageOptions);
            storageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            storageSpinner.setAdapter(storageAdapter);

            //selectedStorage[0] = storageOptions[food.getStorage()];

            // 기본 선택값 설정
            int selectedIndex = 0;
            String currentStorage = food.getStorage();  // "냉장", "냉동", "실온"
            if (currentStorage != null) {
                for (int i = 0; i < storageOptions.length; i++) {
                    if (storageOptions[i].equals(currentStorage)) {
                        selectedIndex = i;
                        break;
                    }
                }
            }
            storageSpinner.setSelection(selectedIndex);
            selectedStorage[0] = storageOptions[selectedIndex];

            // DB에 저장된 날짜로 표시
            String addedDate = food.getAddedDate();
            String expiryDate = food.getExpirationDate();
            detailAdded.setText(addedDate != null ? addedDate : "날짜 선택");;
            detailExp.setText(expiryDate != null ? expiryDate : "날짜 선택");

            // ✅ 보관방식 변경 감지해서 "냉동"일 때 소비기한 비활성화
            storageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedStorage[0] = storageOptions[position];

                    if (selectedStorage[0].equals("냉동")) {
                        detailExp.setAlpha(0.5f);
                        detailExp.setText("기한 없음");
                    } else {
                        detailExp.setAlpha(1f);
                        if (expiryDate != null) {
                            detailExp.setText(expiryDate);
                        } else {
                            detailExp.setText("날짜 선택");
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            // 날짜 선택기
            detailAdded.setOnClickListener(v1 -> showDatePicker(detailAdded));
            detailExp.setOnClickListener(v1 -> {
                if (selectedStorage[0].equals("냉동")) {
                    return; // 냉동이면 무시
                }

                showDatePicker(detailExp);
            });


            // 소비 버튼
            consumeButton.setOnClickListener(v1 -> {
                int pos = holder.getAdapterPosition();
                showConsumeDialog(context, food, "consume", consumeCount -> {
                    int remain = food.getQuantity() - consumeCount;
                    if (remain > 0) {
                        // 수량 차감만
                        updateFoodQuantity(food.getDocumentId(), remain, pos, dialog);
                    } else {
                        // 0이면 완전 삭제
                        deleteFoodItem(food.getDocumentId(), pos, dialog);
                    }
                    logHistory(food.getDocumentId(), food.getName(), food.getCategory(), "consume",
                            consumeCount, remain, null, "소비함");
                });
            });


            // 폐기 버튼
            trashButton.setOnClickListener(v1 -> {
                int pos = holder.getAdapterPosition();
                showConsumeDialog(context, food, "dispose", consumeCount -> {
                    int remain = food.getQuantity() - consumeCount;
                    if (remain > 0) {
                        // 수량 차감만
                        updateFoodQuantity(food.getDocumentId(), remain, pos, dialog);
                    } else {
                        // 0이면 완전 삭제
                        deleteFoodItem(food.getDocumentId(), pos, dialog);
                    }
                    logHistory(food.getDocumentId(), food.getName(), food.getCategory(), "dispose",
                            consumeCount, remain, null, "폐기함");
                });
            });

            // 완료 버튼
            completeButton.setOnClickListener(v1 -> {
                // 날짜와 수량 가져오기
                String addDate = detailAdded.getText().toString();
                String expDate = detailExp.getText().toString();

                int selectedQuantity = (int) quantitySpinner.getSelectedItem();
                String storage = selectedStorage[0];

                int plusQuan = selectedQuantity - food.getQuantity(); // 현재 저장된 음식 갯수 가져와서 선택한 개수와 차이 계산


                // userID 저장
                String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                String docID = food.getDocumentId();
                CollectionReference itemsRef = db.collection("fridges")
                        .document(userID)
                        .collection("items");

                // food 객체에 값 적용
                food.setAddedDate(addDate);
                food.setExpirationDate(expDate);
                food.setQuantity(selectedQuantity);
                food.setStorage(storage);
                Log.d("FoodAdapter", "selectedStorage : " + selectedStorage);

                // DB 업데이트용 맵 생성
                Map<String, Object> updateItem = new HashMap<>();
                updateItem.put("addedDate", addDate);
                updateItem.put("expirationDate", expDate);
                updateItem.put("quantity", selectedQuantity);
                updateItem.put("storage", storage);

                itemsRef.document(docID).update(updateItem)
                        .addOnSuccessListener(aVoid -> {
                            if (plusQuan == 0) {
                                Log.d("FoodAdapter", "수량 : " + "수정없음");
                            }
                            else if (plusQuan < 0) {
                                logHistory(food.getDocumentId(), food.getName(), food.getCategory(),
                                        "consume", Math.abs(plusQuan), food.getQuantity(), null, "소비함");
                            }
                            else {
                                logHistory(food.getDocumentId(), food.getName(), food.getCategory(), "put", plusQuan,
                                        food.getQuantity(), null, "음식 추가");
                            }
                            // 카드뷰 새로고침
                            notifyItemChanged(position);
                            dialog.dismiss();
                            Toast.makeText(context, "수정되었습니다!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(context, "수정 실패", Toast.LENGTH_SHORT).show();
                        });
            });

            dialog.show();
        });
    }

    // 소비 다이얼로그 띄우는 함수
    public void showConsumeDialog(Context context, Food food, String actionType, Consumer<Integer> onConsume) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_action_quantity, null);

        TextView tvCount = dialogView.findViewById(R.id.dialog_txt_count);
        TextView btnMinus = dialogView.findViewById(R.id.dialog_btn_minus);
        TextView btnPlus = dialogView.findViewById(R.id.dialog_btn_plus);
        TextView btnCancel = dialogView.findViewById(R.id.btnConCancel);
        TextView btnConfirm = dialogView.findViewById(R.id.btnConConfirm);
        TextView dialog_title = dialogView.findViewById(R.id.dialog_title);
        TextView dialog_body = dialogView.findViewById(R.id.dialog_body);

        // 타입에 따라 메시지 변경
        switch (actionType) {
            case "consume":
                dialog_title.setText("몇 개 소비할까요?");
                dialog_body.setText("통계 분석에 포함돼요");
                break;
            case "dispose":
                dialog_title.setText("몇 개 폐기할까요?");
                dialog_body.setText("통계 분석에 포함돼요");
                break;
            case "delete":
                dialog_title.setText("몇 개 삭제할까요?");
                dialog_body.setText("통계 분석에는 제외돼요");
                break;
        }


        int maxCount = food.getQuantity();
        final int[] count = {1}; // 최소 1개

        tvCount.setText(count[0] + "개");

        btnMinus.setOnClickListener(v -> {
            if (count[0] > 1) {
                count[0]--;
                tvCount.setText(count[0] + "개");
            }
        });

        btnPlus.setOnClickListener(v -> {
            if (count[0] < maxCount) {
                count[0]++;
                tvCount.setText(count[0] + "개");
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            if ("delete".equals(actionType)) {

            }
            else {
                onConsume.accept(count[0]);
            }
        });

        dialog.show();
    }

    public void updateFoodQuantity(String docId, int newQty, int pos, Dialog dialog) {
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("fridges")
                .document(userID)
                .collection("items")
                .document(docId)
                .update("quantity", newQty)
                .addOnSuccessListener(aVoid -> {Toast.makeText(context, "소비 처리되었습니다!", Toast.LENGTH_SHORT).show();
                    foodList.get(pos).setQuantity(newQty);
                    notifyItemChanged(pos);
                    if (dialog != null) dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "소비 처리 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (dialog != null) dialog.dismiss();
                });
    }

    public void deleteFoodItem(String docId, int pos, Dialog dialog) {
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("fridges")
                .document(userID)
                .collection("items")
                .document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "모두 소비하여 품목이 삭제되었습니다!", Toast.LENGTH_SHORT).show();
                    // 리스트/어댑터 동기화
                    if (pos >= 0 && pos < foodList.size()) {
                        foodList.remove(pos);
                        notifyItemRemoved(pos);
                        notifyItemRangeChanged(pos, foodList.size());
                        if (dialog != null) dialog.dismiss();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (dialog != null) dialog.dismiss();
                });
    }


    private void showDatePicker(EditText editText) {
        TimeZone seoulTZ = TimeZone.getTimeZone("Asia/Seoul");
        Calendar calendar = Calendar.getInstance(seoulTZ);

        DatePickerDialog dialog = new DatePickerDialog(context,
                (view, year, month, dayOfMonth) -> {
                    String dateStr = year + "-" + String.format("%02d", (month + 1)) + "-" + String.format("%02d", dayOfMonth);
                    editText.setText(dateStr);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        dialog.setOnShowListener(d -> {
            // 버튼 색 변경 (예: 확인, 취소 버튼)
            dialog.getButton(DatePickerDialog.BUTTON_POSITIVE).setTextColor(0xFF2E5130);
            dialog.getButton(DatePickerDialog.BUTTON_NEGATIVE).setTextColor(0xFF2E5130);

            // 다이얼로그 배경색 변경 (필요시)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.white);

            // --- 상단 헤더 배경색 변경 ---
            DatePicker datePicker = dialog.getDatePicker();
            int headerId = context.getResources().getIdentifier("date_picker_header", "id", "android");
            View header = datePicker.findViewById(headerId);
            if (header != null) {
                header.setBackgroundColor(Color.parseColor("#2E5130"));  // 원하는 초록색
            }
        });


        dialog.show();
    }


    private String calculateDDay(String expiryDateStr) {
        try {
            String[] parts = expiryDateStr.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]) - 1; // 0부터 시작
            int day = Integer.parseInt(parts[2]);

            TimeZone seoulTZ = TimeZone.getTimeZone("Asia/Seoul");

            Calendar today = Calendar.getInstance(seoulTZ);
            Calendar expiry = Calendar.getInstance(seoulTZ);
            expiry.set(year, month, day, 0, 0, 0);
            expiry.set(Calendar.MILLISECOND, 0);

            long diffMillis = expiry.getTimeInMillis() - today.getTimeInMillis();
            long days = diffMillis / (24 * 60 * 60 * 1000);

            if (days > 0) return "D-" + days;
            else if (days == 0) return "D-DAY";
            else return "D+" + Math.abs(days);

        } catch (Exception e) {
            return "-";
        }
    }

    private String calculateFrozenDDay(String addedDateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
            Date addedDate = sdf.parse(addedDateStr);

            long diffMillis = System.currentTimeMillis() - addedDate.getTime();
            int days = (int) (diffMillis / (1000 * 60 * 60 * 24));

            return "D+" + days;
        } catch (Exception e) {
            return "D+?";
        }
    }


    @Override
    public int getItemCount() {
        return copyList.size();
    }
}

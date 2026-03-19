package com.cookandroid.app.Fragment;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.cookandroid.app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import com.google.firebase.firestore.ListenerRegistration; //메모 저장

import android.widget.HorizontalScrollView;
import com.cookandroid.app.model.HomeRecipe;
import com.cookandroid.app.network.ApiService;
import com.cookandroid.app.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import java.util.Random;
import java.util.function.Consumer;

import android.text.InputType;
import android.view.ViewGroup;
import com.google.android.flexbox.FlexboxLayout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.graphics.Color;



public class HomeFragment extends Fragment {
    private List<ImageView> recipeImageViews = new ArrayList<>();
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private List<String> imageNames = new ArrayList<>();  // 🔹 유연한 이미지 리스트
    private List<String> expiringIngredients = new ArrayList<>(); // 유통기한 임박재료 리스트(오늘의 레시피 추천용)
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration memoListener;


    public HomeFragment() {
        super(R.layout.fragment_home);  // fragment_home.xml 레이아웃을 사용
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 🔹 ImageView 리스트 초기화
        //recipeImageViews.add(view.findViewById(R.id.recipe_image_1));
        //recipeImageViews.add(view.findViewById(R.id.recipe_image_2));
        //recipeImageViews.add(view.findViewById(R.id.recipe_image_3));

        // 🔹 이미지 파일명 리스트 (Firebase Storage에서 불러올 이미지 파일명)
        // 이거 쓰는건가?
        imageNames.add("recipe_image_1.png");
        imageNames.add("recipe_image_2.png");
        imageNames.add("recipe_image_3.png");

        // Firebase Storage 초기화
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        // 🔹 Firebase Storage에서 이미지 로드
        for (int i = 0; i < recipeImageViews.size(); i++) {
            loadRecipeImage(recipeImageViews.get(i), imageNames.get(i));
        }

        // Firestore 및 Auth 초기화
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        // Firestore 경로 수정: fridges/{userUid}/items
        String userUid = auth.getCurrentUser().getUid();

        // 유통기한 임박 재고를 표시할 컨테이너(LinearLayout) 선언
        LinearLayout expiringItemContainer = view.findViewById(R.id.inner_linear_container);
        // 오늘 날짜 가져오기
        //Date today = new Date();
        Calendar todayCal = Calendar.getInstance();
        todayCal.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        todayCal.set(Calendar.HOUR_OF_DAY, 0);
        todayCal.set(Calendar.MINUTE, 0);
        todayCal.set(Calendar.SECOND, 0);
        todayCal.set(Calendar.MILLISECOND, 0);

        Date today = todayCal.getTime();

        db.collection("fridges")
                .document(userUid)
                .collection("items")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // 가져온 문서 개수 로그(디버깅용)
                    Log.d("HomeFragment", "items count = " + querySnapshot.size());

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        // 문서 내부 필드 읽기
                        String name = doc.getString("name");
                        String expirationStr = doc.getString("expirationDate"); // 예: "2025-06-08"
                        String imageName = doc.getString("imageName"); // 예: "food8_47"

                        Log.d("HomeFragment", "읽어온 문서 → name: " + name + ", expirationDate: " + expirationStr);

                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
                            Date expirationDate = sdf.parse(expirationStr);
                            long diffInMillies = expirationDate.getTime() - today.getTime();
                            long daysRemaining = TimeUnit.MILLISECONDS.toDays(diffInMillies);
                            Log.d("HomeFragment", "→ daysRemaining = " + daysRemaining);

                            if (daysRemaining >= 0 && daysRemaining <= 3) {
                                expiringIngredients.add(name); // 임박 재료 파싱(오늘의 레시피 추천용)
                                // 남은 일 수에 따라 표시 문자열 생성
                                String displayText = (daysRemaining == 0)
                                        ? "D-Day"
                                        : daysRemaining + "일 남음";
                                Log.d("HomeFragment", "유통기한 임박! addExpiringItemCard 호출: " + displayText);
                                addExpiringItemCard(expiringItemContainer, name, displayText, imageName);
                            }
                        } catch (Exception e) {
                            Log.e("HomeFragment", "expirationDate 파싱 오류", e);
                        }
                    }

                    // 임박재료중 하나 서버로 보냄
                    if (!expiringIngredients.isEmpty()) {
                        String selected = expiringIngredients.get(new Random().nextInt(expiringIngredients.size()));
                        sendRecipeRequest(selected);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("HomeFragment", "Firestore 아이템 조회 실패", e);
                });

        // 오늘의레시피 추천
        HorizontalScrollView scrollView = view.findViewById(R.id.recipe_scroll);
        LinearLayout recipeContainer = (LinearLayout) scrollView.getChildAt(0);



        // 메모 기능
        LinearLayout memoContainer = view.findViewById(R.id.memo_container);
        ImageButton btnAddMemo = view.findViewById(R.id.btn_add_memo);
        Switch switchShowChecked = view.findViewById(R.id.switch_show_checked); // 🔹 스위치

        switchShowChecked.setText("해야할 일");

        // Switch 텍스트 온오프 설정
        //switchShowChecked.setShowText(true);
        //switchShowChecked.setTextOff("해야할 일");
        //switchShowChecked.setTextOn("완료한 일");

        // 메모 실시간 로드 및 렌더링
        memoListener = db.collection("users")
                .document(userUid)
                .collection("memos")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, err) -> {
                    if (err != null) {
                        Log.e("HomeFragment", "메모 불러오기 실패", err);
                        return;
                    }
                    memoContainer.removeAllViews();
                    memoContainer.addView(btnAddMemo);
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String memoId = doc.getId();
                        String text = doc.getString("text");
                        boolean done = Boolean.TRUE.equals(doc.getBoolean("done"));
                        addMemoRow(memoContainer, memoId, text, done, switchShowChecked);
                    }
                });

        ImageView notify_list_btn = view.findViewById(R.id.notify_list_btn);
        // 알림 리스트 버튼 클릭
        notify_list_btn.setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new NotifyFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // 알림 미확인 개수
        TextView tv_badge = view.findViewById(R.id.tv_badge);
        getUnreadNotificationCount(userUid, count -> {
            if (count > 0) {
                tv_badge.setText(String.valueOf(count));
                tv_badge.setVisibility(View.VISIBLE);
            } else {
                tv_badge.setVisibility(View.GONE);
            }
        });


        // 새 메모 생성
        btnAddMemo.setOnClickListener(v -> {
            Map<String,Object> data = new HashMap<>();
            data.put("text", "");
            data.put("done", false);
            data.put("timestamp", FieldValue.serverTimestamp());
            db.collection("users")
                    .document(userUid)
                    .collection("memos")
                    .add(data)
                    .addOnFailureListener(e -> Log.e("HomeFragment", "메모 생성 실패", e));
        });

        // 스위치 토글 시 text 속성만 변경
        switchShowChecked.setOnCheckedChangeListener((sw, isChecked) -> {
            // textOff/textOn 없이 text 속성으로만 바꿉니다
            sw.setText(isChecked ? "완료한 일" : "해야할 일");

            // 메모 필터링 재적용
            if (memoListener != null) memoListener.remove();
            memoListener = db.collection("users")
                    .document(userUid)
                    .collection("memos")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .addSnapshotListener((snap, err) -> {
                        if (err != null) return;
                        memoContainer.removeAllViews();
                        memoContainer.addView(btnAddMemo);
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            String memoId = doc.getId();
                            String text   = doc.getString("text");
                            boolean done  = Boolean.TRUE.equals(doc.getBoolean("done"));
                            addMemoRow(memoContainer, memoId, text, done, (Switch) sw);
                        }
                    });
        });
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (memoListener != null) {
            memoListener.remove();
        }
    }

    public void getUnreadNotificationCount(String uid, final Consumer<Integer> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .document(uid)
                .collection("notifies")
                .whereEqualTo("read", false)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        int count = task.getResult().size();
                        callback.accept(count);
                    } else {
                        callback.accept(0); // 실패시 0으로 처리
                    }
                });
    }


    // Firebase에서 이미지를 불러와서 ImageView에 설정하는 메소드
    private void loadRecipeImage(ImageView imageView, String imageName) {
        if (!isAdded() || getContext() == null) {
            return; // Fragment가 Attach되지 않은 경우 Glide 실행 방지
        }

        storageRef.child(imageName).getDownloadUrl().addOnSuccessListener(uri -> {
            if (isAdded() && getContext() != null) {  // 🔹 Fragment가 Attach된 경우에만 Glide 실행
                Glide.with(requireContext())
                        .load(uri)
                        .placeholder(R.drawable.test)  // 🔹 로딩 중 기본 이미지
                        .into(imageView);
            }
        }).addOnFailureListener(exception -> {
            // 실패 시 기본 이미지 설정
            imageView.setImageResource(R.drawable.test);
        });
    }
    // 유통기한 임박 카드 생성 메소드 (item_expiration_card.xml을 inflate)
    private void addExpiringItemCard(LinearLayout container, String name, String displayInfo, String imageName) {
        Context context = requireContext();

        // item_expiration_card.xml 레이아웃을 inflate
        View cardView = LayoutInflater.from(context)
                .inflate(R.layout.item_expiration_card, container, false);

        // inflate된 뷰 내부 위젯을 findViewById로 가져오기
        ImageView imageView = cardView.findViewById(R.id.food_image);
        TextView nameText = cardView.findViewById(R.id.food_name);
        TextView infoText = cardView.findViewById(R.id.food_info);

        // 음식 이름 & 남은 일수(D-Day/“n일 남음”) 설정
        nameText.setText(name);
        infoText.setText(displayInfo);

        // 로컬 drawable에서 imageName으로 리소스 ID 찾아서 이미지 세팅
        //    ex) imageName = "food8_47" → R.drawable.food8_47 으로 변환
        int resId = context.getResources().getIdentifier(
                imageName,                   // ex) "food8_47"
                "drawable",                  // drawable 리소스 탐색
                context.getPackageName()
        );
        if (resId != 0) {
            imageView.setImageResource(resId);
        } else {
            // 만약 리소스가 없으면 기본 이미지로 대체 (test.png 등)
            imageView.setImageResource(R.drawable.test);
        }

        // 5) 최종적으로 container에 카드뷰 추가
        container.addView(cardView);
    }

    private void addMemoRow(LinearLayout container,
                            String memoId,
                            String text,
                            boolean done,
                            Switch filterSwitch) {
        Context ctx = requireContext();
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 16, 0, 16);

        CheckBox cb = new CheckBox(ctx);
        cb.setChecked(done);
        cb.setButtonTintList(ColorStateList.valueOf(Color.parseColor("#2E5130")));
        row.addView(cb);

        EditText et = new EditText(ctx);
        et.setText(text);
        et.setTextSize(16);
        et.setBackground(null);
        // 한 줄 입력 + 엔터키만 완료로 동작하도록 설정
        et.setSingleLine(true);
        et.setMaxLines(1);
        et.setImeOptions(EditorInfo.IME_ACTION_DONE);
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        etParams.setMargins(16, 0, 0, 0);
        et.setLayoutParams(etParams);
        row.addView(et);

        // Delete Button
        Button btnDelete = new Button(ctx);
        btnDelete.setText("삭제");
        btnDelete.setTextSize(12);
        btnDelete.setAllCaps(false);
        int widthInDp = (int) (70 * ctx.getResources().getDisplayMetrics().density);
        int heightInDp = (int) (36 * ctx.getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.WRAP_CONTENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT
                widthInDp, heightInDp
        );
        btnParams.setMargins(16, 0, 0, 0);
        btnDelete.setLayoutParams(btnParams);
        row.addView(btnDelete);


        // Enter 키(완료) 처리
        et.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String newText = et.getText().toString().trim();
                    if (newText.isEmpty()) {
                        db.collection("users")
                                .document(auth.getCurrentUser().getUid())
                                .collection("memos")
                                .document(memoId)
                                .delete();
                    } else {
                        db.collection("users")
                                .document(auth.getCurrentUser().getUid())
                                .collection("memos")
                                .document(memoId)
                                .update("text", newText);
                    }
                    // 키보드 숨기기
                    InputMethodManager imm = (InputMethodManager)
                            ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        // Delete버튼 클릭 시 삭제
        btnDelete.setOnClickListener(v -> {
            db.collection("users")
                    .document(auth.getCurrentUser().getUid())
                    .collection("memos")
                    .document(memoId)
                    .delete();
        });

        boolean visible = filterSwitch.isChecked() ? done : !done;
        row.setVisibility(visible ? View.VISIBLE : View.GONE);

        String userUid = auth.getCurrentUser().getUid();
        cb.setOnCheckedChangeListener((buttonView, isChecked) ->
                db.collection("users")
                        .document(userUid)
                        .collection("memos")
                        .document(memoId)
                        .update("done", isChecked)
        );

        et.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String newText = et.getText().toString().trim();
                if (newText.isEmpty()) {
                    db.collection("users")
                            .document(userUid)
                            .collection("memos")
                            .document(memoId)
                            .delete();
                } else {
                    db.collection("users")
                            .document(userUid)
                            .collection("memos")
                            .document(memoId)
                            .update("text", newText);
                }
            }
        });

        container.addView(row);
    }

    private static class Memo {
        String text;
        boolean done;
        Object timestamp = FieldValue.serverTimestamp();

        Memo(String text, boolean done) {
            this.text = text;
            this.done = done;
        }
    }

    private View createRecipeCard(Context context, HomeRecipe recipe) {
        // item_home_recipe.xml을 inflate
        View cardView = LayoutInflater.from(context)
                .inflate(R.layout.item_home_recipe, null, false);

        // 내부 뷰 참조
        ImageView image = cardView.findViewById(R.id.home_recipe_image);
        TextView title = cardView.findViewById(R.id.home_recipe_title);
        TextView time = cardView.findViewById(R.id.home_recipe_time);
        TextView difficulty = cardView.findViewById(R.id.home_recipe_difficulty);
        TextView usedIngredient = cardView.findViewById(R.id.home_recipe_ingredient_used);
        // 데이터 바인딩
        Glide.with(context)
                .load(recipe.getImage_url())
                .placeholder(R.drawable.test)
                .into(image);

        String fullTitle = recipe.getTitle();
        if (fullTitle.length() > 17) {
            String shortPart = fullTitle.substring(0, 17);
            String more = "..더보기";

            SpannableString spannable = new SpannableString(shortPart + more);

            // "더보기"만 스타일 지정
            spannable.setSpan(
                    new ForegroundColorSpan(Color.GRAY),              // 색
                    shortPart.length(),                               // 시작 위치
                    shortPart.length() + more.length(),               // 끝 위치
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            spannable.setSpan(
                    new RelativeSizeSpan(0.8f),                       // 글자 크기 80%
                    shortPart.length(),
                    shortPart.length() + more.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            title.setText(spannable);
        } else {
            title.setText(fullTitle);
        }

        time.setText(recipe.getTime());
        difficulty.setText("난이도 : " + recipe.getDifficulty());
        usedIngredient.setText("임박 사용재료 : " + recipe.getSelectedIngredient());

        cardView.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_recipe_detail, null);
            builder.setView(dialogView);

            // 제목, 썸네일 추가
            TextView titleView = dialogView.findViewById(R.id.dialog_recipe_title);
            ImageView thumbnailView = dialogView.findViewById(R.id.dialog_recipe_image);

            titleView.setText(recipe.getTitle());  // HomeRecipe 객체에서 제목 가져오기
            Glide.with(context).load(recipe.getImage_url()).into(thumbnailView);  // 썸네일 이미지 로딩

            // 재료 목록을 FlexboxLayout에 추가
            FlexboxLayout tagContainer = dialogView.findViewById(R.id.ingredient_tag_container);
            //String[] ingredients = recipe.getIngredient().split(",");  // 쉼표로 구분된 재료 목록 분리
            // [재료] 제거 + |로 나누고 \u0007 제거
            String raw = recipe.getIngredient();
            raw = raw.replaceAll("\\[.*?\\]", "");  // "[재료]" 같은 거 제거
            String[] rawItems = raw.split("\\|");  // '|' 기준으로 재료 나누기

            // 각 재료를 TextView로 만들어서 FlexboxLayout에 추가
            for (String item : rawItems) {
                String cleaned = item.replaceAll("\\u0007", "").trim();  // \u0007 제거

                TextView tag = new TextView(context);
                tag.setText(cleaned);
                tag.setTextSize(12);
                tag.setTextColor(Color.BLACK);
                tag.setPadding(20, 10, 20, 10);
                tag.setBackgroundResource(R.drawable.bg_tag);

                ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(8, 8, 8, 8);
                tag.setLayoutParams(params);

                tagContainer.addView(tag);  // FlexboxLayout에 추가
            }
            AlertDialog dialog = builder.create();
            Button btnOpenUrl = dialogView.findViewById(R.id.btn_open_recipe_url);
            btnOpenUrl.setOnClickListener(view -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(recipe.getUrl()));
                context.startActivity(browserIntent);
                dialog.dismiss();
            });
            dialog.show();
        });
        return cardView;
    }
    private void sendRecipeRequest(String selectedIngredient) {
        HorizontalScrollView scrollView = requireView().findViewById(R.id.recipe_scroll);
        LinearLayout recipeContainer = (LinearLayout) scrollView.getChildAt(0);
        recipeContainer.removeAllViews();  // 중복 방지

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Map<String, String> request = new HashMap<>();
        request.put("ingredient", selectedIngredient);

        apiService.getRecommendedRecipes(request).enqueue(new Callback<List<HomeRecipe>>() {
            @Override
            public void onResponse(Call<List<HomeRecipe>> call, Response<List<HomeRecipe>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (HomeRecipe recipe : response.body()) {
                        recipe.setSelectedIngredient(selectedIngredient);  // !!!
                        View card = createRecipeCard(requireContext(), recipe);

                        // 여백 설정 추가
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        );
                        params.setMargins(16, 0, 16, 0);  // ← 카드 간 좌우 여백
                        card.setLayoutParams(params);

                        recipeContainer.addView(card);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<HomeRecipe>> call, Throwable t) {
                Log.e("HomeFragment", "추천 레시피 호출 실패", t);
            }
        });
    }

}

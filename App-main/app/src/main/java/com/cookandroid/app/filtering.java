package com.cookandroid.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class filtering extends AppCompatActivity {

    private TextView textType, textSituation, textingredient, textmethod;
    private CardView cardType, cardSituation, cardIngredient, cardMethod;
    private FlexboxLayout layoutType, layoutSituation, layoutingredient, layoutmethod;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filtering);

        // 🔙 뒤로가기 툴바 설정
        MaterialToolbar toolbar = findViewById(R.id.filtering_toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // 이전 화면으로 돌아감
            }
        });

        // 📌 뷰 찾기
        cardType = findViewById(R.id.card_type);
        cardSituation = findViewById(R.id.card_situation);
        cardIngredient = findViewById(R.id.card_ingredient);
        cardMethod = findViewById(R.id.card_method);

        layoutType = findViewById(R.id.layout_type);
        layoutSituation = findViewById(R.id.layout_situation);
        layoutingredient = findViewById(R.id.layout_ingredient);
        layoutmethod = findViewById(R.id.layout_method);

        // 🔽 필터 버튼들 클릭 이벤트 자동 설정
        setupFilterButtons(layoutType);
        setupFilterButtons(layoutSituation);
        setupFilterButtons(layoutingredient);
        setupFilterButtons(layoutmethod);


        // 🔽 종류별 펼침 토글 (애니메이션 포함)
        cardType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutSituation.setVisibility(View.GONE);
                layoutingredient.setVisibility(View.GONE);
                layoutmethod.setVisibility(View.GONE);
                toggleLayoutWithFade(layoutType);
            }
        });
        // 상황별
        cardSituation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutType.setVisibility(View.GONE);
                layoutingredient.setVisibility(View.GONE);
                layoutmethod.setVisibility(View.GONE);
                toggleLayoutWithFade(layoutSituation);
            }
        });
        // 재료별
        cardIngredient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutSituation.setVisibility(View.GONE);
                layoutType.setVisibility(View.GONE);
                layoutmethod.setVisibility(View.GONE);
                toggleLayoutWithFade(layoutingredient);
            }
        });

        // 방법별
        cardMethod.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutSituation.setVisibility(View.GONE);
                layoutType.setVisibility(View.GONE);
                layoutingredient.setVisibility(View.GONE);
                toggleLayoutWithFade(layoutmethod);
            }
        });
    }

    // 버튼 클릭 시 키워드 RecipeFragment로 전달후 레시피추천화면으로 돌아감
    private void setupFilterButtons(FlexboxLayout layout) {
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child instanceof MaterialButton) {
                MaterialButton button = (MaterialButton) child;
                button.setOnClickListener(v -> {
                    String keyword = button.getText().toString();
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("categoryKeyword", keyword);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                });
            }
        }
    }
    // ✅ 부드러운 fade-in/out 애니메이션 메서드
    private void toggleLayoutWithFade(View target) {
        if (target.getVisibility() == View.VISIBLE) {
            AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
            fadeOut.setDuration(100);
            fadeOut.setFillAfter(true);
            target.startAnimation(fadeOut);
            target.setVisibility(View.GONE);
        } else {
            target.setVisibility(View.VISIBLE);
            AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
            fadeIn.setDuration(100);
            target.startAnimation(fadeIn);
        }
    }
}

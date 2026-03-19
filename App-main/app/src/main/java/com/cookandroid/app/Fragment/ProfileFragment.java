package com.cookandroid.app.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cookandroid.app.LoginActivity;
import com.cookandroid.app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {
    private TextView tvUserName, tvUserEmail, tvUserPassword, logout_Text;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Firebase 인스턴스 초기화
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 뷰 초기화
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvUserEmail = view.findViewById(R.id.tv_user_email);
        tvUserPassword = view.findViewById(R.id.tv_user_password);
        logout_Text = view.findViewById(R.id.logout_Text);

        View card_recipe = view.findViewById(R.id.card_recipe);
        View card_shopping = view.findViewById(R.id.card_shopping);
        View card_myfood = view.findViewById(R.id.card_myfood);

        card_recipe.setOnClickListener(v -> {
            // 저장한 레시피 프래그먼트로 전환
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new FavoriteFragment())
                    .addToBackStack(null)
                    .commit();
        });

        card_shopping.setOnClickListener(v -> {
            // 장바구니 프래그먼트로 전환
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ListFragment())
                    .addToBackStack(null)
                    .commit();
        });

        card_myfood.setOnClickListener(v -> {
            // 나의 식단 프래그먼트로 전환
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new DietFragment())
                    .addToBackStack(null)
                    .commit();
        });

        loadUserInfo();

        // 버튼 클릭 리스너에 연결
        logout_Text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });
    }

    private void loadUserInfo() {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        if (uid == null) {
            Log.e("ProfileFragment", "로그인된 사용자 없음");
            return;
        }

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String email = documentSnapshot.getString("email");

                        tvUserName.setText(name != null ? name : "이름 없음");
                        tvUserEmail.setText(email != null ? email : "이메일 없음");
                        tvUserPassword.setText("******");
                    } else {
                        Log.e("ProfileFragment", "사용자 문서가 존재하지 않습니다.");
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("ProfileFragment", "사용자 정보 불러오기 실패: " + e.getMessage()));
    }

    // 로그아웃 함수
    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivity(intent);
        requireActivity().finish();
    }
}

package com.cookandroid.app.adapter;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cookandroid.app.R;
import com.cookandroid.app.model.Food;

import java.util.ArrayList;
import java.util.List;

public class LikeFoodAdapter extends RecyclerView.Adapter<LikeFoodAdapter.ViewHolder> {
    private Context context;
    private List<Food> foodList;
    private List<Food> selectedFoods = new ArrayList<>();
    private OnItemClickListener listener;
// 주석 추가
    public interface OnItemClickListener {
        void onAddClick(Food food);
    }

    public LikeFoodAdapter(Context context, List<Food> foodList, OnItemClickListener listener) {
        this.context = context;
        this.foodList = foodList;
        this.listener = listener;
    }

    public void setSelectedFoods(List<Food> selectedFoods) {
        this.selectedFoods = selectedFoods;
        notifyDataSetChanged(); // 선택 상태 UI 업데이트
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.recipe_food, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Food food = foodList.get(position);

        int resId = context.getResources().getIdentifier(food.getImageName(), "drawable", context.getPackageName());
        Log.d("이미지디버깅", "imageName=" + food.getImageName() + ", resId=" + resId);
        holder.foodName.setText(food.getName());
        holder.foodInfo.setText(food.getDaysLeft() + " | " + food.getQuantity() + "개");
        // imageUrl이 비어 있으면 imageResId로 로컬 이미지 사용
//        if (food.getImageUrl() != null && !food.getImageUrl().trim().isEmpty()) {
//            Glide.with(context)
//                    .load(food.getImageUrl())
//                    .placeholder(R.drawable.user)
//                    .error(R.drawable.check)
//                    .into(holder.foodImage);
//        } else {
//            holder.foodImage.setImageResource(resId);
//        }
        holder.foodImage.setImageResource(resId);

        // 배경색으로 선택 표시
        if (selectedFoods.contains(food)) {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#E0F2F1")); // 선택 시 연한 초록
        } else {
            holder.cardView.setCardBackgroundColor(Color.WHITE); // 기본 흰색
        }

        // 카드 클릭 시 선택/해제 토글
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddClick(food);
            }
            notifyItemChanged(holder.getAdapterPosition());
        });
    }


    @Override
    public int getItemCount() {
        return foodList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView foodName, foodInfo;
        ImageView foodImage;
        CardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            foodName = itemView.findViewById(R.id.food_name);
            foodInfo = itemView.findViewById(R.id.food_info);
            foodImage = itemView.findViewById(R.id.food_image);
            cardView = (CardView) itemView; // 전체 카드뷰 자체 클릭 감지용

            // 선택버튼 에서 카드뷰 선택으로 변경
            //addButton = itemView.findViewById(R.id.add_button); // 필요 시 숨김 가능
            //addButton.setVisibility(View.GONE); // 선택만 하고 버튼은 사용 안 함
        }
    }
}

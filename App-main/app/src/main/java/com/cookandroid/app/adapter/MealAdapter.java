package com.cookandroid.app.adapter;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.cookandroid.app.Fragment.DietFragment;
import com.cookandroid.app.R;
import com.cookandroid.app.model.Meal;
import com.cookandroid.app.model.MealItem;
import com.google.android.flexbox.FlexboxLayout;

import java.util.ArrayList;
import java.util.List;

public class MealAdapter extends RecyclerView.Adapter<MealAdapter.MealViewHolder> {
    private List<MealItem> mealList = new ArrayList<>();

    public void setMealList(List<MealItem> list) {
        mealList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_meal, parent, false);
        return new MealViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MealViewHolder holder, int position) {
        MealItem meal = mealList.get(position);
        holder.tvType.setText(meal.getType());
        holder.tvName.setText("메뉴명: " + meal.getTitle());
        holder.tvIngredientsText.setText("사용된 재료: " + TextUtils.join(", ", meal.getUsedIngredients()));
        // 이미지 로딩 - Glide 예시
        Glide.with(holder.itemView.getContext()).load(meal.getImageUrl())
                .transform(new CenterCrop(), new RoundedCorners(15)).into(holder.imgMeal);
        // 카드뷰 클릭 시에
        holder.itemView.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
            View dialogView = LayoutInflater.from(holder.itemView.getContext()).inflate(R.layout.dialog_recipe_detail, null);
            builder.setView(dialogView);

            TextView titleView = dialogView.findViewById(R.id.dialog_recipe_title);
            ImageView thumbnailView = dialogView.findViewById(R.id.dialog_recipe_image);

            titleView.setText(meal.getTitle());
            Glide.with(holder.itemView.getContext()).load(meal.getImageUrl()).into(thumbnailView);

            FlexboxLayout tagContainer = dialogView.findViewById(R.id.ingredient_tag_container);
            String[] ingredients = meal.getDesc().replaceAll("\\[.*?\\]", "").split("\\|");

            for (String ing : ingredients) {
                TextView tag = new TextView(holder.itemView.getContext());
                tag.setText(ing);
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
                tagContainer.addView(tag);
            }
            AlertDialog dialog = builder.create();
            Button btnOpenUrl = dialogView.findViewById(R.id.btn_open_recipe_url);
            btnOpenUrl.setOnClickListener(view -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(meal.getLink()));
                holder.itemView.getContext().startActivity(browserIntent);
                dialog.dismiss();
            });
            dialog.show();
        });

    }

    @Override
    public int getItemCount() {
        return mealList.size();
    }

    public static class MealViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvName, tvIngredientsText;
        ImageView imgMeal;
        View viewStatus;
        public MealViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.tvType);
            tvName = itemView.findViewById(R.id.tvName);
            tvIngredientsText = itemView.findViewById(R.id.tvIngredientsText);
            imgMeal = itemView.findViewById(R.id.imgMeal);
            viewStatus = itemView.findViewById(R.id.viewStatus);
        }
    }
}


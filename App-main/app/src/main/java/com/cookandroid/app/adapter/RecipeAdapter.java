package com.cookandroid.app.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cookandroid.app.R;
import com.cookandroid.app.model.YoutubeRecipe;
import java.util.Map;
import java.util.HashMap;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.auth.FirebaseAuth;
import android.widget.Toast;

import android.util.Log;

import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.ViewHolder> {

    private List<YoutubeRecipe> recipeList;
    private Context context;

    public RecipeAdapter(List<YoutubeRecipe> recipeList, Context context) {
        this.recipeList = recipeList;
        this.context = context;
    }

    public void setRecipes(List<YoutubeRecipe> recipes) {
        this.recipeList = recipes;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecipeAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recipe, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeAdapter.ViewHolder holder, int position) {
        YoutubeRecipe recipe = recipeList.get(position);

        holder.titleText.setText(recipe.getTitle());
        holder.channelText.setText("YouTube");

        Glide.with(context)
                .load(recipe.getThumbnail_url())
                .placeholder(R.drawable.sample)
                .into(holder.thumbnailImage);

        // 썸네일 클릭 시 링크 열기
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(recipe.getVideo_url()));
            context.startActivity(intent);
        });

        // 설명 텍스트 및 토글 처리
        holder.descriptionText.setText(recipe.getDescription());
        holder.descriptionText.setVisibility(View.GONE);
        holder.toggleButton.setText("더보기 ▸");

        holder.toggleButton.setOnClickListener(v -> {
            if (holder.descriptionText.getVisibility() == View.GONE) {
                holder.descriptionText.setVisibility(View.VISIBLE);
                holder.toggleButton.setText("접기 ▾");
            } else {
                holder.descriptionText.setVisibility(View.GONE);
                holder.toggleButton.setText("더보기 ▸");
            }
        });

        // 즐찾 버튼 처리
        holder.favoriteButton.setOnClickListener(v -> {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            String videoUrl = recipe.getVideo_url();
            String videoId = videoUrl.substring(videoUrl.indexOf("v=") + 2);

            Map<String, Object> recipeMap = new HashMap<>();
            recipeMap.put("title", recipe.getTitle());
            recipeMap.put("video_url", recipe.getVideo_url());
            recipeMap.put("thumbnail_url", recipe.getThumbnail_url());
            recipeMap.put("description", recipe.getDescription());
            recipeMap.put("saved_at", FieldValue.serverTimestamp());

            db.collection("fridges")
                    .document(userId)
                    .collection("recipe")
                    .document(videoId)
                    .set(recipeMap)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "즐겨찾기에 저장했어요!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    @Override
    public int getItemCount() {
        return recipeList != null ? recipeList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnailImage;
        TextView titleText;
        TextView channelText;
        //Button ingredientButton;
        TextView descriptionText;
        Button toggleButton;
        public Button favoriteButton; // 즐찾버튼
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnailImage = itemView.findViewById(R.id.recipe_image);
            titleText = itemView.findViewById(R.id.recipe_title);
            channelText = itemView.findViewById(R.id.recipe_time);
            //ingredientButton = itemView.findViewById(R.id.btn_ingredients);

            descriptionText = itemView.findViewById(R.id.recipe_description);
            toggleButton = itemView.findViewById(R.id.btn_toggle_description);
            favoriteButton = itemView.findViewById(R.id.btn_favorite);
        }
    }
}

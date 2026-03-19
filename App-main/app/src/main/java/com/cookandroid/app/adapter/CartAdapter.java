package com.cookandroid.app.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.cookandroid.app.R;
import com.cookandroid.app.model.CartItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;


// Cart어댑터
// CartItem을 어떻게 보여줄 지 나타내는 중간 연결자 역할임
public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder>{
    private List<CartItem> cartList;
    private final Context context;
    private FirebaseFirestore db;

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkbox;
        ImageView foodImage, delImage;
        TextView name;
        TextView btnMinus, btnPlus, txtCount;
        Button btn_add_to_inventory;

        public CartViewHolder(View itemView) {
            super(itemView);
            checkbox = itemView.findViewById(R.id.checkbox_select);
            foodImage = itemView.findViewById(R.id.image_food);
            name = itemView.findViewById(R.id.text_food_name);
            btnMinus = itemView.findViewById(R.id.btn_minus);
            btnPlus = itemView.findViewById(R.id.btn_plus);
            txtCount = itemView.findViewById(R.id.txt_count);
            delImage = itemView.findViewById(R.id.btn_delete);
            btn_add_to_inventory = itemView.findViewById(R.id.btn_add_to_inventory);
        }
    }

    public CartAdapter(Context context, List<CartItem> cartList) {
        this.context = context;
        this.cartList = cartList;
        db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.cart_item, parent, false);
        return new CartViewHolder(v);
    }

    @Override
    public void onBindViewHolder(CartViewHolder holder, int position) {
        CartItem item = cartList.get(position);
        Log.d("CartAdapter", "cartList.size()=" + cartList.size());
        // 아이템 카드에 데이터 매핑
        holder.name.setText(item.getName());
        holder.txtCount.setText(item.getQuantity() + "개");
        holder.delImage.setImageResource(R.drawable.trashcan);
        Log.d("CartAdapter", "onBind name=" + item.getName());

        int resId = context.getResources().getIdentifier(item.getImageName(), "drawable", context.getPackageName());
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String docID = item.getDocumentId();

        CollectionReference cartRef = db.collection("cart")
                .document(userID).collection("cart_items");

        Log.d("CartAdapter", "resId=" + resId);
        if (resId != 0) {
            holder.foodImage.setImageResource(resId);
        } else {
            // 예비 이미지
            holder.foodImage.setImageResource(R.drawable.refrigerator);
        }

        // 수량 증가/감소 이벤트 처리
        holder.btnMinus.setOnClickListener(v -> {
            if (item.getQuantity() > 1) {
                int quantity = item.getQuantity();
                item.setQuantity(--quantity);
                cartRef.document(docID).update("quantity", item.getQuantity());
                notifyItemChanged(position);
            }
        });
        holder.btnPlus.setOnClickListener(v -> {
            int quantity = item.getQuantity();
            item.setQuantity(++quantity);
            cartRef.document(docID).update("quantity", item.getQuantity());
            notifyItemChanged(position);
        });

        holder.checkbox.setChecked(item.isChecked());
        holder.checkbox.setButtonTintList(ContextCompat.getColorStateList(context, R.color.checkbox_selector));
        // 체크박스 상태 변경
        holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.setChecked(isChecked);
        });

        // 삭제 버튼 클릭 시
        holder.delImage.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && pos < cartList.size()) {
                cartRef.document(docID).delete().addOnSuccessListener(aVoid -> {
                    cartList.remove(pos);
                    if (cartList.isEmpty()) {
                        notifyDataSetChanged();
                    } else {
                        notifyItemRemoved(pos);
                    }
                    Toast.makeText(context, "삭제되었습니다!", Toast.LENGTH_SHORT).show();
                });
            }
        });

    }

    @Override
    public int getItemCount() {
        return cartList.size();
    }
}

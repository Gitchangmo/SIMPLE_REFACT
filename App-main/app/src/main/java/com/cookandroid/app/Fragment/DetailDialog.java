package com.cookandroid.app.Fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.cookandroid.app.Fragment.ValueFragment;
import com.cookandroid.app.R;

public class DetailDialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_food_detail, null);

        Button completeButton = view.findViewById(R.id.complete_button);
        completeButton.setOnClickListener(v -> {
            // 부모 Fragment가 ValueFragment이면 Firestore 다시 로드
            if (getParentFragment() instanceof ValueFragment) {
                ((ValueFragment) getParentFragment()).loadFridgeData();
            }
            dismiss();
        });


        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_Material_Light_Dialog_Alert);
        dialog.setContentView(view);
        return dialog;
    }
}


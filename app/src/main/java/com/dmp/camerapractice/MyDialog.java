package com.dmp.camerapractice;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * Created by rtd1p on 12/1/2017.
 */

public class MyDialog extends DialogFragment {

    private int layout;
    private static CameraActivity.DialogListener listener;

    public static MyDialog newInstance(int layout, CameraActivity.DialogListener l) {
        MyDialog dialog = new MyDialog();
        listener = l;

        Bundle bundle = new Bundle();
        bundle.putInt("layout",layout);
        dialog.setArguments(bundle);

        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        layout = getArguments().getInt("layout");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(layout, container, false);

        Button replace = (Button)v.findViewById(R.id.replace_image_button);
        Button select = (Button)v.findViewById(R.id.gallery_image_button);

        replace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onPositiveClick();
                    dismiss();
                }
            }
        });

        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onNegativeClick();
                    dismiss();
                }
            }
        });

        return v;
    }
}

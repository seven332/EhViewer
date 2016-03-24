package com.hippo.ehviewer.ui;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.hippo.ehviewer.R;
import com.hippo.yorozuya.Say;

public class DictImportActivity extends AppCompatActivity {

    private final static String TAG = DictImportActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, getIntent().getData().toString());
        setContentView(R.layout.activity_dict_improt);
        Button importBtn = (Button) findViewById(R.id.btn_import);
        importBtn.setText(importBtn.getText() + getIntent().getData().toString());
        importBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }
}

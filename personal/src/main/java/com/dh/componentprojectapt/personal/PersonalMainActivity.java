package com.dh.componentprojectapt.personal;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.dh.annotation.ARouter;
import com.dh.annotation_api.RouterManager;
import com.dh.common_library.RouterClassConstants;

@ARouter(path = "/personal/PersonalMainActivity")
public class PersonalMainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button mBtApp;
    private Button mBtOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.personal_activity_main);

        mBtApp = findViewById(R.id.btApp);
        mBtOrder = findViewById(R.id.btOrder);

        mBtApp.setOnClickListener(this);
        mBtOrder.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btOrder) {
            RouterManager.getInstance()
                    .build(RouterClassConstants.ORDER_OrderMainActivity)
                    .navigation(this);
        } else if (id == R.id.btApp) {
            RouterManager.getInstance()
                    .build(RouterClassConstants.APP_MainActivity)
                    .navigation(this);
        }
    }

    private void jumpActivity(Class<?> clazz) {
        Intent intent = new Intent(this, clazz);
        intent.putExtra("key", "value");
        startActivity(intent);
    }
}

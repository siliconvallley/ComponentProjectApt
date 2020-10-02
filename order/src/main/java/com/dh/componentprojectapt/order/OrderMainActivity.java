package com.dh.componentprojectapt.order;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.dh.annotation.ARouter;
import com.dh.annotation.Parameter;
import com.dh.annotation_api.RouterManager;
import com.dh.common_library.RouterClassConstants;


@ARouter(path = "/order/OrderMainActivity")
public class OrderMainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "OrderMainActivity";
    private Button mBtApp;
    private Button mBtPersonal;

    @Parameter
    String name;
    @Parameter
    int age;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.order_activity_main);
        initParams();

        mBtApp = findViewById(R.id.btApp);
        mBtPersonal = findViewById(R.id.btPersonal);

        mBtApp.setOnClickListener(this);
        mBtPersonal.setOnClickListener(this);
    }

    private void initParams() {
        OrderMainActivity$$Parameter parameter = new OrderMainActivity$$Parameter();
        parameter.loadParameter(this);

        Log.d(TAG, "name:: " + name + "   age:: " + age);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btPersonal) {
            RouterManager.getInstance()
                    .build(RouterClassConstants.PERSONAL_PersonalMainActivity)
                    .navigation(this);
        } else if (id == R.id.btApp) {
            RouterManager.getInstance()
                    .build(RouterClassConstants.APP_MainActivity)
                    .navigation(this);
            //jumpActivity(targetClass);
        }
    }

    private void jumpActivity(Class<?> clazz) {
        Intent intent = new Intent(this, clazz);
        intent.putExtra("key", "value");
        startActivity(intent);
    }
}

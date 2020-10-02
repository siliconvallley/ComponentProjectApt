package com.dh.componentprojectapt;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.dh.annotation.ARouter;
import com.dh.annotation.model.RouterBean;
import com.dh.annotation_api.core.ARouterLoadPath;
import com.dh.componentprojectapt.apt.ARouter$$Group$$order;
import com.dh.componentprojectapt.apt.ARouter$$Group$$personal;
import com.dh.componentprojectapt.apt.ARouter$$Path$$order;
import com.dh.componentprojectapt.apt.ARouter$$Path$$personal;

import java.util.Map;

@ARouter(path = "/app/MainActivity")
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button btOrder;
    private Button btPersonal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btOrder = findViewById(R.id.btOrder);
        btPersonal = findViewById(R.id.btPersonal);

        btOrder.setOnClickListener(this);
        btPersonal.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btOrder:
                jumpOrder();
                break;
            case R.id.btPersonal:
                jumpPersonal();
                break;
        }
    }

    private void jumpOrder() {
        // 最终集成化模式，所有子模块app/order/personal通过APT生成的类文件都会打包到apk里面，
        // 不用担心找不到
        ARouter$$Group$$order group$$order = new ARouter$$Group$$order();
        Map<String, Class<? extends ARouterLoadPath>> groupMap = group$$order.loadGroup();

        // 通过order组名获取对应路由路径对象
        Class<? extends ARouterLoadPath> orderClazz = groupMap.get("order");

        if (orderClazz == null) return;
        // 类加载动态加载路由路径对象
        try {
            ARouter$$Path$$order path$$order = (ARouter$$Path$$order) orderClazz.newInstance();
            Map<String, RouterBean> pathMap = path$$order.loadPath();
            RouterBean bean = pathMap.get("/order/OrderMainActivity");
            if (bean == null) return;
            Class<?> targetClass = bean.getClazz();
            jumpActivity(targetClass);
        } catch (IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
    }

    private void jumpPersonal() {
        // 最终集成化模式，所有子模块app/order/personal通过APT生成的类文件都会打包到apk里面，
        // 不用担心找不到
        ARouter$$Group$$personal group$$order = new ARouter$$Group$$personal();
        Map<String, Class<? extends ARouterLoadPath>> groupMap = group$$order.loadGroup();

        // 通过order组名获取对应路由路径对象
        Class<? extends ARouterLoadPath> orderClazz = groupMap.get("personal");

        if (orderClazz == null) return;
        // 类加载动态加载路由路径对象
        try {
            ARouter$$Path$$personal path$$order = (ARouter$$Path$$personal) orderClazz.newInstance();
            Map<String, RouterBean> pathMap = path$$order.loadPath();
            RouterBean bean = pathMap.get("/personal/PersonalMainActivity");
            if (bean == null) return;
            Class<?> targetClass = bean.getClazz();
            jumpActivity(targetClass);
        } catch (IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
    }

    private void jumpActivity(Class<?> clazz) {
        Intent intent = new Intent(this, clazz);
        intent.putExtra("name", "张三");
        intent.putExtra("age", 100);
        startActivity(intent);
    }
}
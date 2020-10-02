package com.dh.componentprojectapt.test;

import com.dh.annotation.model.RouterBean;
import com.dh.annotation_api.core.ARouterLoadPath;
import com.dh.componentprojectapt.order.OrderMainActivity;

import java.util.HashMap;
import java.util.Map;

/**
 * 模拟ARouter路由器的组文件，对应的路径文件
 */
public class ARouter$$Path$$order implements ARouterLoadPath {

    @Override
    public Map<String, RouterBean> loadPath() {
        Map<String, RouterBean> pathMap = new HashMap<>();
        pathMap.put("/order/OrderMainActivity",
                RouterBean.create(RouterBean.Type.ACTIVITY, OrderMainActivity.class,
                        "/order/OrderMainActivity", "order"));
        return pathMap;
    }
}

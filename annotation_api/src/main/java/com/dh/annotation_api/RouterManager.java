package com.dh.annotation_api;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;

import com.dh.annotation.model.RouterBean;
import com.dh.annotation_api.core.ARouterLoadGroup;
import com.dh.annotation_api.core.ARouterLoadPath;

import java.util.Map;

/**
 * @author 86351
 * @date 2020/9/27
 * @description 路由管理类
 */
public class RouterManager {
    private static final String TAG = "RouterManager";
    // 缓存
    private LruCache<String, ARouterLoadGroup> groupLruCache;
    private LruCache<String, ARouterLoadPath> pathLruCache;
    // 为了拼接提取公共部分，例如：ARouter$$Group$$app
    private final static String FILE_GROUP_NAME = "ARouter$$Group$$";
    private volatile static RouterManager routerManager;
    // 路由的组名  app，order，personal
    private String group;
    // 路由的路径  /app/MainActivity
    private String path;

    private RouterManager() {
        this.groupLruCache = new LruCache<>(100);
        this.pathLruCache = new LruCache<>(100);
    }

    public static RouterManager getInstance() {
        if (null == routerManager) {
            synchronized (RouterManager.class) {
                if (null == routerManager) {
                    routerManager = new RouterManager();
                }
            }
        }
        return routerManager;
    }

    /**
     * 构建BundleManager对象，并且处理group和path
     *
     * @param path 需要跳转的path
     * @return
     */
    public BundleManager build(String path) {
        if (TextUtils.isEmpty(path) || !path.startsWith("/")) {
            throw new IllegalArgumentException("@ARouter注解未按规范书写, 如: /app/MainActivity! " +
                    "error path:: " + path);
        }
        // 比如开发者代码为：path = "/MainActivity"，最后一个 / 符号必然在字符串第1位
        if (path.lastIndexOf("/") == 0) {
            throw new IllegalArgumentException("@ARouter注解未按规范书写, 如: /app/MainActivity! " +
                    "error path:: " + path);
        }
        // 从第一个 / 到第二个 / 中间截取，如：/app/MainActivity 截取出 app 作为group
        String resultGroup = path.substring(1, path.indexOf("/", 1));
        // 比如开发者/MainActivity/MainActivity
        if (TextUtils.isEmpty(resultGroup) || resultGroup.contains("/")) {
            throw new IllegalArgumentException("@ARouter注解未按规范书写, 如: /app/MainActivity! " +
                    "error path:: " + path);
        }
        this.path = path;
        this.group = resultGroup;
        return new BundleManager();
    }

    /**
     * 用来处理模块间跳转
     *
     * @param context       上下文
     * @param bundleManager 参数管理类
     * @return
     */
    public Object navigation(Context context, BundleManager bundleManager) {
        if (TextUtils.isEmpty(group) || TextUtils.isEmpty(path)) {
            throw new IllegalArgumentException("必须先调用build方法！");
        }
        // 查找APT生成的类跳转类：例如：ARouter$$Group$$app
        // 先获取应用的包名
        String groupClassName = context.getPackageName() + ".apt." + FILE_GROUP_NAME + group;
        Log.d(TAG, "groupClassName:: " + groupClassName);
        try {
            // 读取路由组 Group 类文件
            ARouterLoadGroup loadGroup = groupLruCache.get(group);
            // 缓存没有内容
            if (null == loadGroup) {
                // 加载APT路由组Group类文件 例如：ARouter$$Group$$app
                Class<?> aClass = Class.forName(groupClassName);
                // 初始化类文件
                loadGroup = (ARouterLoadGroup) aClass.newInstance();
                // 保存到缓存里面去
                groupLruCache.put(group, loadGroup);
            }

            Map<String, Class<? extends ARouterLoadPath>> groupMap = loadGroup.loadGroup();
            if (groupMap == null || groupMap.isEmpty()) {
                throw new RuntimeException("需要跳转的path未获取到对应路由Group表");
            }

            /*------------------------走到此处，我们已经获取到路由Group表----------------------*/
            // ARouter$$Path$$app
            // 读取路由 path 文件
            ARouterLoadPath loadPath = pathLruCache.get(path);
            if (null == loadPath) {
                Class<? extends ARouterLoadPath> clazz = groupMap.get(group);
                if (clazz == null) {
                    throw new RuntimeException("在需要跳转的Group表中未找到对应path所指的path表");
                }
                loadPath = clazz.newInstance();
                // 保存到缓存里面去
                pathLruCache.put(path, loadPath);
            }

            Map<String, RouterBean> pathMap = loadPath.loadPath();
            if (pathMap == null || pathMap.isEmpty()) {
                throw new RuntimeException("需要跳转的path未获取到对应路由path表");
            }
            RouterBean routerBean = pathMap.get(path);
            if (routerBean != null) {
                switch (routerBean.getType()) {
                    case ACTIVITY:
                        jumpActivity(context, routerBean, bundleManager);
                        break;
                }
            }
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void jumpActivity(Context context, RouterBean routerBean, BundleManager bundleManager) {
        Intent intent = new Intent(context, routerBean.getClazz());
        intent.putExtras(bundleManager.getBundle());
        context.startActivity(intent);
    }
}

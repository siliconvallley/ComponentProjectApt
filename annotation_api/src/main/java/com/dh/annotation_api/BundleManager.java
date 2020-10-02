package com.dh.annotation_api;

import android.content.Context;
import android.os.Bundle;

import java.io.Serializable;

/**
 * @author 86351
 * @date 2020/9/28
 * @description 处理参数
 */
public class BundleManager {
    private Bundle bundle = new Bundle();

    public Bundle getBundle() {
        return bundle;
    }

    public BundleManager putBundle(Bundle bundle) {
        this.bundle = bundle;
        return this;
    }

    public BundleManager putString(String key, String value) {
        bundle.putString(key, value);
        return this;
    }

    public BundleManager putInt(String key, int value) {
        bundle.putInt(key, value);
        return this;
    }

    public BundleManager putLong(String key, long value) {
        bundle.putLong(key, value);
        return this;
    }

    public BundleManager putFloat(String key, float value) {
        bundle.putFloat(key, value);
        return this;
    }

    public BundleManager putBoolean(String key, boolean value) {
        bundle.putBoolean(key, value);
        return this;
    }

    public BundleManager putSerializable(String key, Serializable value) {
        bundle.putSerializable(key, value);
        return this;
    }

    /**
     * 用来处模块间跳转 模块一 和 模块二 交换
     * @param context 上下文
     * @return
     */
    public Object navigation(Context context) {
        // 应该在这里面些导航吗？  不属于此类职责（单一职责）
        return RouterManager.getInstance().navigation(context, this);
    }
}

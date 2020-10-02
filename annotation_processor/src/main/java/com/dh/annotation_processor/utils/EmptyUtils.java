package com.dh.annotation_processor.utils;

import java.util.Collection;
import java.util.Map;

/**
 * @author 86351
 * @date 2020/9/24
 * @description
 */
public class EmptyUtils {

    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }
}

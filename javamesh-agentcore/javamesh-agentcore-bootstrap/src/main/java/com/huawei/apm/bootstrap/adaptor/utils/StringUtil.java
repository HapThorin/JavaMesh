/*
 * Copyright (C) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 */

package com.huawei.apm.bootstrap.adaptor.utils;

/**
 * 字符串工具类
 *
 * @author h30007557
 * @version 1.0.0
 * @since 2021/10/18
 */
public class StringUtil {
    /**
     * 是否通配符匹配，支持*和?，*匹配任意字符，?匹配一个字符
     *
     * @param str 字符串
     * @param wc  通配符匹配格式
     * @return 是否匹配成功
     */
    public static boolean isWildcardMatch(String str, String wc) {
        final char[] strArr = str.toCharArray();
        final char[] wcArr = wc.toCharArray();
        int wcCursor = 0;
        for (int strCursor = 0, starIdx = -1, starCursor = 0; strCursor < strArr.length; ) {
            if (wcCursor < wcArr.length && wcArr[wcCursor] != '*' &&
                    (wcArr[wcCursor] == '?' || strArr[strCursor] == wcArr[wcCursor])) {
                strCursor++;
                wcCursor++;
            } else if (wcCursor < wcArr.length && wcArr[wcCursor] == '*') {
                starIdx = wcCursor;
                starCursor = strCursor;
                wcCursor++;
            } else if (starIdx >= 0) {
                starCursor++;
                wcCursor = starIdx + 1;
                strCursor = starCursor;
            } else {
                return false;
            }
        }
        for (; wcCursor < wcArr.length; wcCursor++) {
            if (wcArr[wcCursor] != '*') {
                return false;
            }
        }
        return true;
    }
}

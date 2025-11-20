package com.wb.rules.common.result;

import com.wb.rules.common.enums.RuleResultCode;

import java.io.Serial;
import java.io.Serializable;

public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int code;
    private String msg;
    private T data;
    private R(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // 成功响应方法
    public static <T> R<T> success() {
        return new R<>(RuleResultCode.SUCCESS.getCode(), RuleResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> R<T> success(T data) {
        return new R<>(RuleResultCode.SUCCESS.getCode(), RuleResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> R<T> success(String message, T data) {
        return new R<>(RuleResultCode.SUCCESS.getCode(), message, data);
    }

    // 错误响应方法
    public static <T> R<T> error(RuleResultCode RuleResultCode) {
        return new R<>(RuleResultCode.getCode(), RuleResultCode.getMessage(), null);
    }

    public static <T> R<T> error(int code, String message) {
        return new R<>(code, message, null);
    }

    public static <T> R<T> error(String message) {
        return new R<>(RuleResultCode.BUSINESS_ERROR.getCode(), message, null);
    }

    // 便捷判断方法
    public boolean isSuccess() {
        return this.code == RuleResultCode.SUCCESS.getCode();
    }
}

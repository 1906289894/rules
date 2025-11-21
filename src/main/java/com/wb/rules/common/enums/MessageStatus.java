package com.wb.rules.common.enums;

public enum MessageStatus {

    PROCESSING(0, "投递中"),
    SUCCESS(1, "发送成功"),
    FAILED(2, "失败");

    private final int code;
    private final String message;

    MessageStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public int getCode() {
        return code;
    }
}

// common/enums/ResultCode.java
package com.wb.rules.common.enums;

import lombok.Getter;

@Getter
public enum RuleResultCode {
    // 成功状态
    SUCCESS(200, "操作成功"),
    
    // 客户端错误
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    
    // 服务端错误
    SYSTEM_ERROR(500, "系统错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),
    
    // 业务错误
    BUSINESS_ERROR(1000, "业务异常"),
    USER_NOT_FOUND(1001, "用户不存在"),
    DATA_DUPLICATE(1002, "数据重复");

    private final int code;
    private final String message;

    RuleResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
package com.wb.rules.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RuleMessageSendResult {
    private Boolean sendStatus;
    private String msgId;
    private String msgKey;
    private String errorMsg;
}

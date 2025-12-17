package com.mq.mqaiagent.exception;

import com.mq.mqaiagent.common.ErrorCode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BusinessExceptionTest {

    @Test
    void ctor_codeMessage_setsFields() {
        BusinessException e = new BusinessException(123, "msg");
        assertEquals(123, e.getCode());
        assertEquals("msg", e.getMessage());
    }

    @Test
    void ctor_errorCode_setsCodeAndMessage() {
        BusinessException e = new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        assertEquals(ErrorCode.NOT_LOGIN_ERROR.getCode(), e.getCode());
        assertEquals(ErrorCode.NOT_LOGIN_ERROR.getMessage(), e.getMessage());
    }

    @Test
    void ctor_errorCodeWithMessage_overridesMessageKeepsCode() {
        BusinessException e = new BusinessException(ErrorCode.NO_AUTH_ERROR, "deny");
        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), e.getCode());
        assertEquals("deny", e.getMessage());
    }
}


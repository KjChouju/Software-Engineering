package com.mq.mqaiagent.exception;

import com.mq.mqaiagent.common.ErrorCode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ThrowUtilsTest {

    @Test
    void throwIf_runtime_true_throws() {
        RuntimeException ex = new RuntimeException("bad");
        assertThrows(RuntimeException.class, () -> ThrowUtils.throwIf(true, ex));
    }

    @Test
    void throwIf_runtime_false_notThrow() {
        RuntimeException ex = new RuntimeException("bad");
        assertDoesNotThrow(() -> ThrowUtils.throwIf(false, ex));
    }

    @Test
    void throwIf_errorCode_true_throwsBusiness() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), e.getCode());
        assertEquals(ErrorCode.PARAMS_ERROR.getMessage(), e.getMessage());
    }

    @Test
    void throwIf_errorCode_false_notThrow() {
        assertDoesNotThrow(() -> ThrowUtils.throwIf(false, ErrorCode.PARAMS_ERROR));
    }

    @Test
    void throwIf_errorCodeMessage_true_throwsBusinessWithMessage() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> ThrowUtils.throwIf(true, ErrorCode.SYSTEM_ERROR, "系统失败"));
        assertEquals(ErrorCode.SYSTEM_ERROR.getCode(), e.getCode());
        assertEquals("系统失败", e.getMessage());
    }

    @Test
    void throwIf_errorCodeMessage_false_notThrow() {
        assertDoesNotThrow(() -> ThrowUtils.throwIf(false, ErrorCode.SYSTEM_ERROR, "系统失败"));
    }
}


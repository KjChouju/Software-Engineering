package com.mq.mqaiagent.exception;

import com.mq.mqaiagent.common.BaseResponse;
import com.mq.mqaiagent.common.ErrorCode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    @Test
    void businessExceptionHandler_returnsCodeAndMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        BusinessException ex = new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        BaseResponse<?> res = handler.businessExceptionHandler(ex);
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), res.getCode());
        assertEquals("参数错误", res.getMessage());
    }

    @Test
    void runtimeExceptionHandler_returnsSystemError() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        RuntimeException ex = new RuntimeException("boom");
        BaseResponse<?> res = handler.runtimeExceptionHandler(ex);
        assertEquals(ErrorCode.SYSTEM_ERROR.getCode(), res.getCode());
        assertEquals("系统错误", res.getMessage());
        assertNull(res.getData());
    }
}


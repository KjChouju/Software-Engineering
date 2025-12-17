package com.mq.mqaiagent.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ErrorCodeTest {

    @Test
    void values_haveNonNullCodeAndMessage() {
        for (ErrorCode ec : ErrorCode.values()) {
            assertNotNull(ec.getMessage());
            assertTrue(ec.getCode() >= 0);
        }
    }

    @Test
    void specificValues_matchExpected() {
        assertEquals(0, ErrorCode.SUCCESS.getCode());
        assertEquals("ok", ErrorCode.SUCCESS.getMessage());
        assertEquals(50000, ErrorCode.SYSTEM_ERROR.getCode());
        assertEquals("系统内部异常", ErrorCode.SYSTEM_ERROR.getMessage());
        assertEquals(40400, ErrorCode.TOO_MANY_REQUESTS.getCode());
        assertEquals("请求过于频繁", ErrorCode.TOO_MANY_REQUESTS.getMessage());
    }
}


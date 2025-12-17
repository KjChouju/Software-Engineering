package com.mq.mqaiagent.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BaseResponseTest {

    @Test
    void constructor_allArgs_setsFields() {
        BaseResponse<String> res = new BaseResponse<>(201, "ok", "created");
        assertEquals(201, res.getCode());
        assertEquals("ok", res.getData());
        assertEquals("created", res.getMessage());
    }

    @Test
    void constructor_twoArgs_setsEmptyMessage() {
        BaseResponse<String> res = new BaseResponse<>(200, "ok");
        assertEquals(200, res.getCode());
        assertEquals("ok", res.getData());
        assertEquals("", res.getMessage());
    }

    @Test
    void constructor_errorCode_setsCodeAndMessage() {
        BaseResponse<?> res = new BaseResponse<>(ErrorCode.NO_AUTH_ERROR);
        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), res.getCode());
        assertNull(res.getData());
        assertEquals(ErrorCode.NO_AUTH_ERROR.getMessage(), res.getMessage());
    }
}


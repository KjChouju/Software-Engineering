package com.mq.mqaiagent.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResultUtilsTest {

    @Test
    void success_returnsOk() {
        BaseResponse<String> res = ResultUtils.success("data");
        assertEquals(0, res.getCode());
        assertEquals("data", res.getData());
        assertEquals("ok", res.getMessage());
    }

    @Test
    void error_withErrorCode() {
        BaseResponse<?> res = ResultUtils.error(ErrorCode.PARAMS_ERROR);
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), res.getCode());
        assertNull(res.getData());
        assertEquals(ErrorCode.PARAMS_ERROR.getMessage(), res.getMessage());
    }

    @Test
    void error_withCodeMessage() {
        BaseResponse<?> res = ResultUtils.error(123, "oops");
        assertEquals(123, res.getCode());
        assertNull(res.getData());
        assertEquals("oops", res.getMessage());
    }

    @Test
    void error_withErrorCodeAndMessage() {
        BaseResponse<?> res = ResultUtils.error(ErrorCode.SYSTEM_ERROR, "自定义错误");
        assertEquals(ErrorCode.SYSTEM_ERROR.getCode(), res.getCode());
        assertNull(res.getData());
        assertEquals("自定义错误", res.getMessage());
    }
}


package com.mq.mqaiagent.tools;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.*;

class GoogleWebSearchToolTest {

    @Test
    void googleSearch_invalidKey_returnsFailureMessage() {
        GoogleWebSearchTool tool = new GoogleWebSearchTool("invalid_key");
        String result = tool.googleSearch("test query");
        assertNotNull(result);
        assertTrue(
                "请求失败或无返回内容".equals(result) ||
                result.contains("未找到相关结果") ||
                result.contains("请求失败")
        );
    }

    @Test
    void googleSearch_status200_parsesResults() {
        GoogleWebSearchTool tool = new GoogleWebSearchTool("any_key");
        HttpResponse mockResp = Mockito.mock(HttpResponse.class);
        Mockito.when(mockResp.getStatus()).thenReturn(200);
        String body = """
        {"organic_results":[
          {"title":"T1","link":"L1","snippet":"S1"},
          {"title":"T2","link":"L2","snippet":"S2"}
        ]}
        """;
        Mockito.when(mockResp.body()).thenReturn(body);
        HttpRequest mockReq = Mockito.mock(HttpRequest.class);
        Mockito.when(mockReq.execute()).thenReturn(mockResp);
        try (MockedStatic<HttpRequest> mocked = Mockito.mockStatic(HttpRequest.class)) {
            mocked.when(() -> HttpRequest.get(Mockito.anyString())).thenReturn(mockReq);
            String result = tool.googleSearch("query");
            assertNotNull(result);
            assertTrue(result.contains("标题"));
            assertTrue(result.contains("链接"));
            assertTrue(result.contains("摘要"));
        }
    }

    @Test
    void googleSearch_httpThrows_throwsRuntimeException() {
        GoogleWebSearchTool tool = new GoogleWebSearchTool("any_key");
        try (MockedStatic<HttpRequest> mocked = Mockito.mockStatic(HttpRequest.class)) {
            mocked.when(() -> HttpRequest.get(Mockito.anyString()))
                    .thenThrow(new RuntimeException("net error"));
            assertThrows(RuntimeException.class, () -> tool.googleSearch("q"));
        }
    }
}

package com.mq.mqaiagent.tools;

import cn.hutool.http.HttpUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ClassName：WebSearchToolTest
 * Package:com.mq.mqaiagent.tools
 * Description: 网页搜索工具测试类
 */
@SpringBootTest
class WebSearchToolTest {

    @Value("${search-api.api-key}")
    private String searchApiKey;

    @Test
    void searchWeb_success_orReturnsNonNull() {
        WebSearchTool webSearchTool = new WebSearchTool(searchApiKey);
        String query = "小林coding";
        String result = webSearchTool.searchWeb(query);
        assertNotNull(result);
    }

    @Test
    void searchWeb_error_returnsMessage() {
        WebSearchTool webSearchTool = new WebSearchTool("invalid_key");
        String result = webSearchTool.searchWeb("test");
        assertNotNull(result);
    }

    @Test
    void searchWeb_staticMock_parsesAndFiltersFields() {
        WebSearchTool webSearchTool = new WebSearchTool("any_key");
        String body = """
        {
          "organic_results": [
            {"title":"T1","link":"L1","description":"D1"},
            {"title":"T2","link":"L2","description":"D2"},
            {"title":"T3","link":"L3","description":"D3"},
            {"title":"T4","link":"L4","description":"D4"},
            {"title":"T5","link":"L5","description":"D5"}
          ]
        }
        """;
        try (MockedStatic<HttpUtil> mocked = Mockito.mockStatic(HttpUtil.class)) {
            mocked.when(() -> HttpUtil.get(Mockito.anyString(), Mockito.anyMap()))
                    .thenReturn(body);
            String result = webSearchTool.searchWeb("query");
            assertNotNull(result);
            assertFalse(result.contains("T1"));
            assertFalse(result.contains("L1"));
            assertFalse(result.contains("D1"));
            assertTrue(result.contains("\"description\":\"\""));
            assertTrue(result.contains("\"title\":\"\""));
            assertTrue(result.contains("\"link\":\"\""));
        }
    }

    @Test
    void searchWeb_staticMock_throws_returnsErrorMessage() {
        WebSearchTool webSearchTool = new WebSearchTool("any_key");
        try (MockedStatic<HttpUtil> mocked = Mockito.mockStatic(HttpUtil.class)) {
            mocked.when(() -> HttpUtil.get(Mockito.anyString(), Mockito.anyMap()))
                    .thenThrow(new RuntimeException("boom"));
            String result = webSearchTool.searchWeb("q");
            assertTrue(result.startsWith("Error searching Baidu: "));
        }
    }
}

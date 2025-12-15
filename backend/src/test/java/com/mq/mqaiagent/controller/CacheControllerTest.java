package com.mq.mqaiagent.controller;

import com.mq.mqaiagent.service.AiResponseCacheService;
import com.mq.mqaiagent.service.CacheMetricsService;
import com.mq.mqaiagent.service.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.mq.mqaiagent.exception.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CacheControllerTest {

    private MockMvc mockMvc;
    private CacheController cacheController;
    private CacheService cacheService;
    private AiResponseCacheService aiResponseCacheService;
    private CacheMetricsService cacheMetricsService;

    @BeforeEach
    void setUp() {
        cacheController = new CacheController();
        cacheService = Mockito.mock(CacheService.class);
        aiResponseCacheService = Mockito.mock(AiResponseCacheService.class);
        cacheMetricsService = Mockito.mock(CacheMetricsService.class);
        setPrivateField(cacheController, "cacheService", cacheService);
        setPrivateField(cacheController, "aiResponseCacheService", aiResponseCacheService);
        setPrivateField(cacheController, "cacheMetricsService", cacheMetricsService);
        mockMvc = MockMvcBuilders.standaloneSetup(cacheController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetPerformanceReport_success() throws Exception {
        CacheMetricsService.CachePerformanceReport report =
                new CacheMetricsService.CachePerformanceReport(1, 1, 1.0, 1, 1, 1.0, 2, 3, true);
        when(cacheMetricsService.getPerformanceReport()).thenReturn(report);
        mockMvc.perform(get("/cache/performance").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testGetPerformanceReport_error() throws Exception {
        when(cacheMetricsService.getPerformanceReport()).thenThrow(new RuntimeException("x"));
        mockMvc.perform(get("/cache/performance").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("获取缓存性能报告失败")));
    }

    @Test
    void testGetCacheStatus_success() throws Exception {
        when(cacheService.isRedisAvailable()).thenReturn(true);
        when(cacheMetricsService.getChatMemoryCacheHitRate()).thenReturn(0.9);
        when(cacheMetricsService.getAiResponseCacheHitRate()).thenReturn(0.8);
        when(cacheMetricsService.getSavedApiCalls()).thenReturn(100L);
        AiResponseCacheService.CacheStats stats = new AiResponseCacheService.CacheStats(5, 10, 0.5);
        when(aiResponseCacheService.getCacheStats()).thenReturn(stats);
        mockMvc.perform(get("/cache/status").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testGetCacheStatus_error() throws Exception {
        when(cacheService.isRedisAvailable()).thenThrow(new RuntimeException("x"));
        mockMvc.perform(get("/cache/status").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("获取缓存状态失败")));
    }

    @Test
    void testClearChatMemoryCache_success_true() throws Exception {
        when(cacheService.generateChatMemoryKey("c1", 1L)).thenReturn("k");
        when(cacheService.delete("k")).thenReturn(true);
        mockMvc.perform(delete("/cache/chat-memory")
                        .param("conversationId", "c1")
                        .param("userId", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("缓存清除成功")));
    }

    @Test
    void testClearChatMemoryCache_success_false() throws Exception {
        when(cacheService.generateChatMemoryKey("c2", null)).thenReturn("k2");
        when(cacheService.delete("k2")).thenReturn(false);
        mockMvc.perform(delete("/cache/chat-memory")
                        .param("conversationId", "c2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("缓存不存在或已清除")));
    }

    @Test
    void testClearChatMemoryCache_error() throws Exception {
        when(cacheService.generateChatMemoryKey(any(), any())).thenThrow(new RuntimeException("x"));
        mockMvc.perform(delete("/cache/chat-memory")
                        .param("conversationId", "c3")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("清除缓存失败")));
    }

    @Test
    void testClearAiResponseCache_success_true() throws Exception {
        when(aiResponseCacheService.clearCache("q1", 1L)).thenReturn(true);
        mockMvc.perform(delete("/cache/ai-response")
                        .param("question", "q1")
                        .param("userId", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("缓存清除成功")));
    }

    @Test
    void testClearAiResponseCache_success_false() throws Exception {
        when(aiResponseCacheService.clearCache("q2", null)).thenReturn(false);
        mockMvc.perform(delete("/cache/ai-response")
                        .param("question", "q2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("缓存不存在或已清除")));
    }

    @Test
    void testClearAiResponseCache_error() throws Exception {
        when(aiResponseCacheService.clearCache(any(), any())).thenThrow(new RuntimeException("x"));
        mockMvc.perform(delete("/cache/ai-response")
                        .param("question", "q3")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("清除缓存失败")));
    }

    @Test
    void testResetMetrics_success() throws Exception {
        mockMvc.perform(post("/cache/reset-metrics").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("性能计数器重置成功")));
    }

    @Test
    void testResetMetrics_error() throws Exception {
        Mockito.doThrow(new RuntimeException("x")).when(cacheMetricsService).resetCounters();
        mockMvc.perform(post("/cache/reset-metrics").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("重置失败")));
    }

    @Test
    void testWarmupCache_success() throws Exception {
        mockMvc.perform(post("/cache/warmup").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("缓存预热完成")));
    }

    @Test
    void testGetCacheConfig_success() throws Exception {
        when(cacheService.isRedisAvailable()).thenReturn(true);
        mockMvc.perform(get("/cache/config").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("chatMemoryCachePrefix")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("aiResponseCachePrefix")));
    }

    @Test
    void testGetCacheConfig_error() throws Exception {
        when(cacheService.isRedisAvailable()).thenThrow(new RuntimeException("x"));
        mockMvc.perform(get("/cache/config").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("获取缓存配置失败")));
    }

    @Test
    void testHealthCheck_up_healthy() throws Exception {
        when(cacheService.isRedisAvailable()).thenReturn(true);
        when(cacheService.set(any(), any(), Mockito.anyLong())).thenReturn(true);
        when(cacheService.get(any())).thenReturn("test");
        when(cacheService.delete(any())).thenReturn(true);
        mockMvc.perform(get("/cache/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("HEALTHY")));
    }

    @Test
    void testHealthCheck_up_degraded() throws Exception {
        when(cacheService.isRedisAvailable()).thenReturn(true);
        when(cacheService.set(any(), any(), Mockito.anyLong())).thenReturn(true);
        when(cacheService.get(any())).thenReturn("bad");
        when(cacheService.delete(any())).thenReturn(true);
        mockMvc.perform(get("/cache/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("DEGRADED")));
    }

    @Test
    void testHealthCheck_down() throws Exception {
        when(cacheService.isRedisAvailable()).thenReturn(false);
        mockMvc.perform(get("/cache/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("UNHEALTHY")));
    }

    @Test
    void testHealthCheck_error() throws Exception {
        when(cacheService.isRedisAvailable()).thenThrow(new RuntimeException("x"));
        mockMvc.perform(get("/cache/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ERROR")));
    }

    private void setPrivateField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set private field: " + fieldName, e);
        }
    }
}

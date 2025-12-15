package com.mq.mqaiagent.controller;

import com.mq.mqaiagent.pool.ChatClientPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.mq.mqaiagent.exception.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatClientPoolControllerTest {

    private MockMvc mockMvc;
    private ChatClientPoolController chatClientPoolController;
    private ChatClientPool chatClientPool;

    @BeforeEach
    void setUp() {
        chatClientPoolController = new ChatClientPoolController();
        chatClientPool = Mockito.mock(ChatClientPool.class);
        setPrivateField(chatClientPoolController, "chatClientPool", chatClientPool);
        mockMvc = MockMvcBuilders.standaloneSetup(chatClientPoolController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetCacheStats_success() throws Exception {
        ChatClientPool.CacheStats stats = new ChatClientPool.CacheStats(10, 5, 7, 20, 0.66);
        when(chatClientPool.getCacheStats()).thenReturn(stats);
        mockMvc.perform(get("/pool/stats").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testGetCacheStats_error() throws Exception {
        when(chatClientPool.getCacheStats()).thenThrow(new RuntimeException("x"));
        mockMvc.perform(get("/pool/stats").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("获取统计信息失败")));
    }

    @Test
    void testManualCleanup_success() throws Exception {
        ChatClientPool.CacheStats before = new ChatClientPool.CacheStats(10, 5, 7, 20, 0.66);
        ChatClientPool.CacheStats after = new ChatClientPool.CacheStats(12, 6, 8, 17, 0.67);
        when(chatClientPool.getCacheStats()).thenReturn(before, after);
        mockMvc.perform(post("/pool/cleanup").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("清理前缓存大小: 20")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("清理后缓存大小: 17")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("清理数量: 3")));
    }

    @Test
    void testManualCleanup_error() throws Exception {
        when(chatClientPool.getCacheStats()).thenThrow(new RuntimeException("x"));
        mockMvc.perform(post("/pool/cleanup").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("清理失败")));
    }

    @Test
    void testClearAll_success() throws Exception {
        ChatClientPool.CacheStats before = new ChatClientPool.CacheStats(1, 1, 1, 5, 0.5);
        when(chatClientPool.getCacheStats()).thenReturn(before);
        mockMvc.perform(post("/pool/clear-all").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("清空所有缓存")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("清理数量: 5")));
    }

    @Test
    void testClearAll_error() throws Exception {
        ChatClientPool.CacheStats before = new ChatClientPool.CacheStats(1, 1, 1, 5, 0.5);
        when(chatClientPool.getCacheStats()).thenReturn(before);
        doThrow(new RuntimeException("x")).when(chatClientPool).clearAll();
        mockMvc.perform(post("/pool/clear-all").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("清空缓存失败")));
    }

    @Test
    void testGetPerformanceReport_success() throws Exception {
        ChatClientPool.CacheStats stats = new ChatClientPool.CacheStats(10, 5, 7, 50, 0.8);
        when(chatClientPool.getCacheStats()).thenReturn(stats);
        mockMvc.perform(get("/pool/performance-report").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("良好")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("继续保持")));
    }

    @Test
    void testGetPerformanceReport_error() throws Exception {
        when(chatClientPool.getCacheStats()).thenThrow(new RuntimeException("x"));
        mockMvc.perform(get("/pool/performance-report").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("获取性能报告失败")));
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

package com.mq.mqaiagent.service.impl;

import com.mq.mqaiagent.service.CacheService;
import com.mq.mqaiagent.service.TextSimilarityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiResponseCacheServiceImplTest {

    @Mock
    private CacheService cacheService;

    @Mock
    private TextSimilarityService textSimilarityService;

    @InjectMocks
    private AiResponseCacheServiceImpl service;

    @Test
    void testCacheResponse_storeAndIndex_ok() {
        String question = "我想增肌，请给我建议";
        String response = "好的，这里是建议";
        when(textSimilarityService.generateTextHash(question)).thenReturn("h1");
        when(cacheService.generateAiResponseKey("h1")).thenReturn("mq:ai:agent:ai:response:h1");
        when(cacheService.set(anyString(), eq(response), anyLong())).thenReturn(true);
        when(cacheService.get(eq("mq:ai:agent:question:index"), eq(List.class))).thenReturn(new ArrayList<>());

        boolean result = service.cacheResponse(question, response, null);
        assertTrue(result);
        verify(cacheService, times(1)).set(eq("mq:ai:agent:ai:response:h1"), eq(response), eq(12L));
        verify(cacheService, times(1)).set(eq("mq:ai:agent:question:index"), any(List.class), eq(24L));
    }

    @Test
    void testGetCachedResponse_exactHit_ok() {
        String question = "我想增肌";
        when(textSimilarityService.generateTextHash(question)).thenReturn("h1");
        when(cacheService.generateAiResponseKey("h1")).thenReturn("mq:ai:agent:ai:response:h1");
        when(cacheService.get(eq("mq:ai:agent:ai:response:h1"), eq(String.class))).thenReturn("cached");

        String resp = service.getCachedResponse(question, null);
        assertEquals("cached", resp);
        verify(cacheService, times(1)).get(eq("mq:ai:agent:ai:response:h1"), eq(String.class));
    }

    @Test
    void testGetCachedResponse_similarHit_ok() {
        String question = "我想增肌";
        String candidate = "我想增肌，请给我建议";
        when(textSimilarityService.generateTextHash(question)).thenReturn("h1");
        when(cacheService.generateAiResponseKey("h1")).thenReturn("mq:ai:agent:ai:response:h1");
        when(cacheService.get(eq("mq:ai:agent:ai:response:h1"), eq(String.class))).thenReturn(null);

        when(cacheService.get(eq("mq:ai:agent:question:index"), eq(List.class)))
                .thenReturn(Arrays.asList(candidate));
        TextSimilarityService.SimilarityResult sr = new TextSimilarityService.SimilarityResult(candidate, 0.9);
        when(textSimilarityService.calculateSimilarities(eq(question), anyList()))
                .thenReturn(Arrays.asList(sr));
        when(textSimilarityService.getSimilarityThreshold()).thenReturn(0.5);

        when(textSimilarityService.generateTextHash(candidate)).thenReturn("h2");
        when(cacheService.generateAiResponseKey("h2")).thenReturn("mq:ai:agent:ai:response:h2");
        when(cacheService.get(eq("mq:ai:agent:ai:response:h2"), eq(String.class))).thenReturn("resp2");

        String resp = service.getCachedResponse(question, null);
        assertEquals("resp2", resp);
        verify(cacheService, times(1)).set(eq("mq:ai:agent:ai:response:h1"), eq("resp2"), eq(12L));
    }

    @Test
    void testClearCache_removeIndex_ok() {
        String question = "我想增肌";
        when(textSimilarityService.generateTextHash(question)).thenReturn("h1");
        when(cacheService.generateAiResponseKey("h1")).thenReturn("mq:ai:agent:ai:response:h1");
        when(cacheService.delete(eq("mq:ai:agent:ai:response:h1"))).thenReturn(true);
        List<String> idx = new ArrayList<>();
        idx.add(question);
        when(cacheService.get(eq("mq:ai:agent:question:index"), eq(List.class))).thenReturn(idx);

        boolean result = service.clearCache(question, null);
        assertTrue(result);
        verify(cacheService, times(1)).set(eq("mq:ai:agent:question:index"), any(List.class), eq(24L));
    }

    @Test
    void testGetCacheStats_ok() {
        when(cacheService.get(eq("mq:ai:agent:question:index"), eq(List.class)))
                .thenReturn(Arrays.asList("q1", "q2"));
        var stats = service.getCacheStats();
        assertEquals(2, stats.getTotalCachedQuestions());
        assertEquals(0, stats.getTotalRequests());
        assertEquals(0.0, stats.getHitRate());
    }

    @Test
    void testGetCachedResponse_blankQuestion_returnsNull() {
        assertNull(service.getCachedResponse("", null));
        assertNull(service.getCachedResponse("   ", 1L));
    }

    @Test
    void testGetCachedResponse_exactGetThrows_returnsNull() {
        String q = "问题";
        when(textSimilarityService.generateTextHash(q)).thenReturn("h");
        when(cacheService.generateAiResponseKey("h")).thenReturn("mq:ai:agent:ai:response:h");
        when(cacheService.get(eq("mq:ai:agent:ai:response:h"), eq(String.class)))
                .thenThrow(new RuntimeException("redis error"));
        assertNull(service.getCachedResponse(q, null));
    }

    @Test
    void testGetCachedResponse_similarBelowThreshold_returnsNull() {
        String q = "问题A";
        when(textSimilarityService.generateTextHash(q)).thenReturn("ha");
        when(cacheService.generateAiResponseKey("ha")).thenReturn("mq:ai:agent:ai:response:ha");
        when(cacheService.get(eq("mq:ai:agent:ai:response:ha"), eq(String.class))).thenReturn(null);
        when(cacheService.get(eq("mq:ai:agent:question:index"), eq(List.class)))
                .thenReturn(Arrays.asList("问题B"));
        when(textSimilarityService.calculateSimilarities(eq(q), anyList()))
                .thenReturn(Arrays.asList(new TextSimilarityService.SimilarityResult("问题B", 0.2)));
        when(textSimilarityService.getSimilarityThreshold()).thenReturn(0.75);
        assertNull(service.getCachedResponse(q, null));
    }

    @Test
    void testCacheResponse_blankInputs_returnsFalse() {
        assertFalse(service.cacheResponse("", "resp", null));
        assertFalse(service.cacheResponse("q", "", null));
    }

    @Test
    void testCacheResponse_setThrows_returnsFalse() {
        String q = "问题";
        when(textSimilarityService.generateTextHash(q)).thenReturn("h");
        when(cacheService.generateAiResponseKey("h")).thenReturn("mq:ai:agent:ai:response:h");
        when(cacheService.set(anyString(), anyString(), anyLong())).thenThrow(new RuntimeException("fail"));
        assertFalse(service.cacheResponse(q, "resp", null));
    }

    @Test
    void testClearCache_blankQuestion_returnsFalse() {
        assertFalse(service.clearCache("", null));
    }

    @Test
    void testClearCache_deleteThrows_returnsFalse() {
        String q = "问题";
        when(textSimilarityService.generateTextHash(q)).thenReturn("h");
        when(cacheService.generateAiResponseKey("h")).thenReturn("mq:ai:agent:ai:response:h");
        when(cacheService.delete(eq("mq:ai:agent:ai:response:h"))).thenThrow(new RuntimeException("fail"));
        assertFalse(service.clearCache(q, null));
    }
}

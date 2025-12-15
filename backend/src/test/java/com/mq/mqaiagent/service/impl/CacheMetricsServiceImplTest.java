package com.mq.mqaiagent.service.impl;

import com.mq.mqaiagent.service.AiResponseCacheService;
import com.mq.mqaiagent.service.CacheMetricsService;
import com.mq.mqaiagent.service.CacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheMetricsServiceImplTest {

    @Mock
    private CacheService cacheService;

    @Mock
    private AiResponseCacheService aiResponseCacheService;

    @InjectMocks
    private CacheMetricsServiceImpl service;

    @Test
    void testRecordAndRates_ok() {
        service.recordChatMemoryCacheHit();
        service.recordChatMemoryCacheHit();
        service.recordChatMemoryCacheMiss();
        assertEquals(2.0 / 3.0, service.getChatMemoryCacheHitRate());

        service.recordAiResponseCacheHit();
        service.recordAiResponseCacheHit();
        service.recordAiResponseCacheHit();
        service.recordAiResponseCacheMiss();
        assertEquals(3.0 / 4.0, service.getAiResponseCacheHitRate());
        assertEquals(3, service.getSavedApiCalls());
    }

    @Test
    void testGetPerformanceReport_ok() {
        when(aiResponseCacheService.getCacheStats()).thenReturn(new AiResponseCacheService.CacheStats(5, 0, 0.0));
        when(cacheService.isRedisAvailable()).thenReturn(true);
        service.recordChatMemoryCacheHit();
        service.recordAiResponseCacheHit();
        CacheMetricsService.CachePerformanceReport report = service.getPerformanceReport();
        assertEquals(1, report.getChatMemoryCacheHits());
        assertEquals(1, report.getAiResponseCacheHits());
        assertEquals(5, report.getTotalCachedQuestions());
        assertTrue(report.isRedisAvailable());
    }

    @Test
    void testPersistMetrics_andLoad_ok() {
        when(aiResponseCacheService.getCacheStats()).thenReturn(new AiResponseCacheService.CacheStats(2, 0, 0.0));
        when(cacheService.isRedisAvailable()).thenReturn(true);
        service.recordAiResponseCacheHit();
        CacheMetricsService.CachePerformanceReport before = service.getPerformanceReport();
        verify(cacheService, times(1)).isRedisAvailable();
        service.persistMetrics();
        verify(cacheService, times(1)).set(eq("mq:ai:agent:metrics:performance_report"), any(), eq(24L));

        when(cacheService.get(eq("mq:ai:agent:metrics:performance_report"), eq(CacheMetricsService.CachePerformanceReport.class)))
                .thenReturn(before);
        CacheMetricsService.CachePerformanceReport loaded = service.loadPersistedMetrics();
        assertEquals(before.getSavedApiCalls(), loaded.getSavedApiCalls());
        assertEquals(before.getAiResponseCacheHits(), loaded.getAiResponseCacheHits());
    }

    @Test
    void testPersistMetrics_whenRedisUnavailable_skip() {
        when(cacheService.isRedisAvailable()).thenReturn(false);
        service.persistMetrics();
        verify(cacheService, never()).set(anyString(), any(), anyLong());
    }

    @Test
    void testResetCounters_ok() {
        service.recordAiResponseCacheHit();
        service.recordChatMemoryCacheMiss();
        service.resetCounters();
        assertEquals(0.0, service.getAiResponseCacheHitRate());
        assertEquals(0.0, service.getChatMemoryCacheHitRate());
        assertEquals(0, service.getSavedApiCalls());
    }

    @Test
    void testLoadPersistedMetrics_redisUnavailable_returnsCurrent() {
        when(cacheService.isRedisAvailable()).thenReturn(false);
        service.recordChatMemoryCacheHit();
        service.recordAiResponseCacheMiss();
        CacheMetricsService.CachePerformanceReport report = service.loadPersistedMetrics();
        assertEquals(1, report.getChatMemoryCacheHits());
        assertEquals(0, report.getAiResponseCacheHits());
    }

    @Test
    void testLoadPersistedMetrics_nullReport_fallback() {
        when(cacheService.isRedisAvailable()).thenReturn(true);
        when(cacheService.get(eq("mq:ai:agent:metrics:performance_report"), eq(CacheMetricsService.CachePerformanceReport.class)))
                .thenReturn(null);
        service.recordAiResponseCacheHit();
        CacheMetricsService.CachePerformanceReport report = service.loadPersistedMetrics();
        assertEquals(1, report.getAiResponseCacheHits());
    }
}

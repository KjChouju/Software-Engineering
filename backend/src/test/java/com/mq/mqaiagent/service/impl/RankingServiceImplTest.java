package com.mq.mqaiagent.service.impl;

import com.mq.mqaiagent.model.dto.ranking.ExerciseStats;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.HashOperations;
import com.mq.mqaiagent.mapper.ExerciseLogMapper;
import com.mq.mqaiagent.model.dto.ranking.MyRankingResponse;
import com.mq.mqaiagent.model.dto.ranking.RankingListResponse;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.*;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RankingServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ZSetOperations<String, Object> zSetOps;
    @Mock
    private HashOperations<String, Object, Object> hashOps;
    @Mock
    private ExerciseLogMapper exerciseLogMapper;

    @InjectMocks
    private RankingServiceImpl service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        // Mock mapper methods
        when(exerciseLogMapper.countByUserIdAndWeek(anyLong(), any())).thenReturn(5);
        when(exerciseLogMapper.sumStatsByUserIdAndWeek(anyLong(), any())).thenReturn(new ExerciseStats());
    }

    @Test
    void testRefreshRanking_week_success() {
        when(redisTemplate.delete(anyString())).thenReturn(true);
        boolean ok = service.refreshRanking("week");
        assertTrue(ok);
        verify(redisTemplate, atLeastOnce()).delete(startsWith("ranking:week:"));
    }

    @Test
    void testGetRankingList_empty_returnsEmptyResult() {
        LocalDate startDate = LocalDate.now().minusDays(29);
        when(zSetOps.reverseRangeWithScores(anyString(), anyLong(), anyLong()))
                .thenReturn(Collections.emptySet());
        when(zSetOps.zCard(anyString())).thenReturn(0L);

        RankingListResponse resp = service.getRankingList("month", 1, 20);
        assertNotNull(resp);
        assertEquals(0L, resp.getTotal());
        assertEquals(0, resp.getRecords().size());
        assertEquals(0, resp.getPages());
    }

    @Test
    void testGetMyRanking_notOnBoard() {
        when(zSetOps.score(anyString(), any())).thenReturn(null);
        when(zSetOps.zCard(anyString())).thenReturn(5L);
        MyRankingResponse resp = service.getMyRanking(123L, "week");
        assertNotNull(resp);
        assertFalse(resp.getIsOnBoard());
        assertEquals(5, resp.getTotalUsers());
    }

    @Test
    void testUpdateRankingAfterAdd_updatesRedis() {
        when(hashOps.hasKey(anyString(), anyString())).thenReturn(false);
        when(hashOps.get(anyString(), anyString())).thenReturn(null);
        when(zSetOps.add(anyString(), any(), anyDouble())).thenReturn(true);

        service.updateRankingAfterAdd(1L, new Date(1730000000000L),
                LocalDate.of(2025, 12, 8), LocalDate.of(2025, 11, 17));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(hashOps, atLeastOnce()).put(keyCaptor.capture(), anyString(), anyString());
        List<String> keys = keyCaptor.getAllValues();
        assertTrue(keys.stream().anyMatch(k -> k.startsWith("ranking:first:week:")));
        assertTrue(keys.stream().anyMatch(k -> k.startsWith("ranking:first:month:")));

        verify(zSetOps, atLeastOnce()).add(startsWith("ranking:week:"), eq("1"), anyDouble());
        verify(zSetOps, atLeastOnce()).add(startsWith("ranking:month:"), eq("1"), anyDouble());
        verify(redisTemplate, atLeast(2)).expire(startsWith("ranking:"), anyLong(), any());
        verify(redisTemplate, atLeast(2)).expire(startsWith("ranking:first:"), anyLong(), any());
    }
}


package com.mq.mqaiagent.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CacheServiceImplTest {

    private CacheServiceImpl service;
    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOps;

    @BeforeEach
    void setup() {
        service = new CacheServiceImpl();
        redisTemplate = mock(RedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        ReflectionTestUtils.setField(service, "redisTemplate", redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void testSetAndGet_ok() {
        doNothing().when(valueOps).set(eq("k1"), eq("v1"), eq(1L), eq(TimeUnit.HOURS));
        when(valueOps.get(eq("k1"))).thenReturn("v1");
        boolean set = service.set("k1", "v1", 1L);
        assertTrue(set);
        Object val = service.get("k1");
        assertEquals("v1", val);
    }

    @Test
    void testGetTyped_ok() {
        when(valueOps.get(eq("k2"))).thenReturn("v2");
        String val = service.get("k2", String.class);
        assertEquals("v2", val);
        Integer bad = service.get("k2", Integer.class);
        assertNull(bad);
    }

    @Test
    void testDeleteExistsExpire_ok() {
        when(redisTemplate.delete(eq("k3"))).thenReturn(true);
        assertTrue(service.delete("k3"));
        when(redisTemplate.hasKey(eq("k4"))).thenReturn(true);
        assertTrue(service.exists("k4"));
        when(redisTemplate.expire(eq("k5"), eq(2L), eq(TimeUnit.HOURS))).thenReturn(true);
        assertTrue(service.expire("k5", 2L));
    }

    @Test
    void testGenerateKeys_ok() {
        String cm1 = service.generateChatMemoryKey("cid", 123L);
        String cm2 = service.generateChatMemoryKey("cid", null);
        assertEquals("mq:ai:agent:chat:memory:123:cid", cm1);
        assertEquals("mq:ai:agent:chat:memory:cid", cm2);
        String ai = service.generateAiResponseKey("h");
        assertEquals("mq:ai:agent:ai:response:h", ai);
    }

    @Test
    void testRedisAvailable_ok() {
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        RedisConnection conn = mock(RedisConnection.class);
        when(redisTemplate.getConnectionFactory()).thenReturn(factory);
        when(factory.getConnection()).thenReturn(conn);
        when(conn.ping()).thenReturn("PONG");
        assertTrue(service.isRedisAvailable());
    }

    @Test
    void testSet_exception_returnsFalse() {
        doThrow(new RuntimeException("fail")).when(valueOps).set(anyString(), any(), anyLong(), any());
        assertFalse(service.set("k", "v", 1L));
    }

    @Test
    void testGet_exception_returnsNull() {
        when(valueOps.get(eq("k"))).thenThrow(new RuntimeException("fail"));
        assertNull(service.get("k"));
    }

    @Test
    void testDelete_exception_returnsFalse() {
        when(redisTemplate.delete(eq("k"))).thenThrow(new RuntimeException("fail"));
        assertFalse(service.delete("k"));
    }

    @Test
    void testExists_exception_returnsFalse() {
        when(redisTemplate.hasKey(eq("k"))).thenThrow(new RuntimeException("fail"));
        assertFalse(service.exists("k"));
    }

    @Test
    void testExpire_exception_returnsFalse() {
        when(redisTemplate.expire(eq("k"), anyLong(), any())).thenThrow(new RuntimeException("fail"));
        assertFalse(service.expire("k", 1L));
    }

    @Test
    void testIsRedisAvailable_pingThrows_returnsFalse() {
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        when(redisTemplate.getConnectionFactory()).thenReturn(factory);
        when(factory.getConnection()).thenThrow(new RuntimeException("conn fail"));
        assertFalse(service.isRedisAvailable());
    }
}

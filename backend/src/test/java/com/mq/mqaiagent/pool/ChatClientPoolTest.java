package com.mq.mqaiagent.pool;

import com.mq.mqaiagent.mapper.KeepReportMapper;
import com.mq.mqaiagent.service.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.lang.reflect.Constructor;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class ChatClientPoolTest {

    private ChatClientPool pool;
    private ChatModel chatModel;
    private KeepReportMapper keepReportMapper;
    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        pool = new ChatClientPool();
        chatModel = Mockito.mock(ChatModel.class);
        keepReportMapper = Mockito.mock(KeepReportMapper.class);
        cacheService = Mockito.mock(CacheService.class);
        ReflectionTestUtils.setField(pool, "dashscopeChatModel", chatModel);
        ReflectionTestUtils.setField(pool, "keepReportMapper", keepReportMapper);
        ReflectionTestUtils.setField(pool, "cacheService", cacheService);
        pool.clearAll();
    }

    @Test
    void getKeepAppClient_cacheMissThenHit_returnsSameInstance() {
        ChatClient c1 = pool.getKeepAppClient("sys");
        ChatClient c2 = pool.getKeepAppClient("sys");
        assertNotNull(c1);
        assertSame(c1, c2);
        ChatClientPool.CacheStats stats = pool.getCacheStats();
        assertEquals(1, stats.cacheMisses());
        assertEquals(1, stats.cacheHits());
        assertTrue(stats.hitRate() > 0.0);
    }

    @Test
    void getKeepAppClientWithMemory_differentUserId_returnsDifferentInstances() {
        ChatClient a1 = pool.getKeepAppClientWithMemory(1L, "sys");
        ChatClient a2 = pool.getKeepAppClientWithMemory(2L, "sys");
        assertNotNull(a1);
        assertNotNull(a2);
        assertNotSame(a1, a2);
    }

    @Test
    void getMqManusClient_andKeepAppClient_areDifferent() {
        ChatClient k = pool.getKeepAppClient("sys");
        ChatClient m = pool.getMqManusClient("sys");
        assertNotSame(k, m);
    }

    @Test
    void getMqManusClientWithMemory_cacheMissThenHit_andDifferentUsersDifferentInstances() {
        ChatClient u11a = pool.getMqManusClientWithMemory(11L, "sys");
        ChatClient u11b = pool.getMqManusClientWithMemory(11L, "sys");
        ChatClient u12 = pool.getMqManusClientWithMemory(12L, "sys");
        assertNotNull(u11a);
        assertSame(u11a, u11b);
        assertNotSame(u11a, u12);
    }

    @Test
    void createMqManusClientWithMemory_reflection_returnsNewInstances() {
        try {
            var m = ChatClientPool.class.getDeclaredMethod("createMqManusClientWithMemory", Long.class, String.class);
            m.setAccessible(true);
            ChatClient c1 = (ChatClient) m.invoke(pool, 20L, "sys");
            ChatClient c2 = (ChatClient) m.invoke(pool, 21L, "sys");
            assertNotNull(c1);
            assertNotNull(c2);
            assertNotSame(c1, c2);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void cleanupExpiredClients_removesExpiredAndInactive() {
        ConcurrentHashMap<String, Object> map =
                (ConcurrentHashMap<String, Object>) ReflectionTestUtils.getField(pool, "clientCache");
        ChatClient c1 = Mockito.mock(ChatClient.class);
        ChatClient c2 = Mockito.mock(ChatClient.class);
        ChatClient c3 = Mockito.mock(ChatClient.class);
        try {
            Class<?> cachedClass = Class.forName("com.mq.mqaiagent.pool.ChatClientPool$CachedChatClient");
            Constructor<?> ctor = cachedClass.getDeclaredConstructor(ChatClient.class);
            ctor.setAccessible(true);
            Object cached1 = ctor.newInstance(c1);
            Object cached2 = ctor.newInstance(c2);
            Object cached3 = ctor.newInstance(c3);
        map.put("expired", cached1);
        map.put("inactive", cached2);
        map.put("active", cached3);
        ReflectionTestUtils.setField(cached1, "createdTime", LocalDateTime.now().minusMinutes(120));
        ReflectionTestUtils.setField(cached2, "lastAccessedTime", LocalDateTime.now().minusMinutes(120));
        pool.cleanupExpiredClients();
        assertFalse(map.containsKey("expired"));
        assertFalse(map.containsKey("inactive"));
        assertTrue(map.containsKey("active"));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void clearAll_emptiesCache() {
        pool.getKeepAppClient("s1");
        pool.getKeepAppClientWithMemory(10L, "s2");
        assertTrue(pool.getCacheStats().currentCacheSize() >= 2);
        pool.clearAll();
        assertEquals(0, pool.getCacheStats().currentCacheSize());
    }
}

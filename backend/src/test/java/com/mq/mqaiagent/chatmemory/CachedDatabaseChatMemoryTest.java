package com.mq.mqaiagent.chatmemory;

import com.mq.mqaiagent.mapper.KeepReportMapper;
import com.mq.mqaiagent.model.dto.keepReport.KeepReport;
import com.mq.mqaiagent.service.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CachedDatabaseChatMemoryTest {

    KeepReportMapper mapper;
    CacheService cache;
    CachedDatabaseChatMemory memory;

    @BeforeEach
    void setUp() {
        mapper = mock(KeepReportMapper.class);
        cache = mock(CacheService.class);
        memory = new CachedDatabaseChatMemory(mapper, cache);
        memory.setCurrentUserId(11L);
    }

    @Test
    void testAdd_updatesCache() {
        when(cache.isRedisAvailable()).thenReturn(true);
        when(mapper.selectOne(any())).thenReturn(null, null);
        when(mapper.insert(any(KeepReport.class))).thenReturn(1);
        // super.get(conversationId, userId, Integer.MAX_VALUE) used in updateCache
        KeepReport stored = KeepReport.builder()
                .chatId("chat_conv1").userId(11L)
                .messages("[{\"messageType\":\"USER\",\"message\":\"x\"}]").build();
        when(mapper.selectOne(any())).thenReturn(stored);

        memory.add("conv1", List.of(new UserMessage("x")));

        verify(cache, atLeastOnce()).set(eq(cache.generateChatMemoryKey("conv1", 11L)), anyList(), anyLong());
    }

    @Test
    void testGet_cacheHit_returnsLastN() {
        when(cache.isRedisAvailable()).thenReturn(true);
        when(cache.get(anyString(), eq(List.class))).thenReturn(List.of(
                new UserMessage("a"), new UserMessage("b"), new UserMessage("c")
        ));

        List<Message> last2 = memory.get("conv2", 2);
        assertEquals(2, last2.size());
        assertEquals("b", last2.get(0).getText());
        assertEquals("c", last2.get(1).getText());
        verify(cache, never()).set(anyString(), any(), anyLong());
    }

    @Test
    void testGet_cacheMiss_dbFallback_andWriteBack() {
        when(cache.isRedisAvailable()).thenReturn(true);
        when(cache.get(anyString(), eq(List.class))).thenReturn(null);
        KeepReport stored = KeepReport.builder()
                .chatId("chat_conv3").userId(11L)
                .messages("[{\"messageType\":\"USER\",\"message\":\"x\"},{\"messageType\":\"USER\",\"message\":\"y\"}]")
                .build();
        when(mapper.selectOne(any())).thenReturn(stored);

        List<Message> last1 = memory.get("conv3", 1);
        assertEquals(1, last1.size());
        assertEquals("y", last1.get(0).getText());
        verify(cache, atLeastOnce()).set(eq(cache.generateChatMemoryKey("conv3", 11L)), anyList(), anyLong());
    }

    @Test
    void testGet_cacheThrows_fallbackToDb() {
        when(cache.isRedisAvailable()).thenReturn(true);
        when(cache.get(anyString(), eq(List.class))).thenThrow(new RuntimeException("cache err"));
        KeepReport stored = KeepReport.builder()
                .chatId("chat_conv4").userId(11L)
                .messages("[{\"messageType\":\"USER\",\"message\":\"m\"}]").build();
        when(mapper.selectOne(any())).thenReturn(stored);

        List<Message> res = memory.get("conv4", 10);
        assertEquals(1, res.size());
        assertEquals("m", res.get(0).getText());
    }

    @Test
    void testGetWithUserId_cacheMiss_dbFallback_andWriteBack() {
        when(cache.isRedisAvailable()).thenReturn(true);
        when(cache.get(anyString(), eq(List.class))).thenReturn(null);
        KeepReport stored = KeepReport.builder()
                .chatId("chat_conv5").userId(12L)
                .messages("[{\"messageType\":\"USER\",\"message\":\"r\"}]").build();
        when(mapper.selectOne(any())).thenReturn(stored);

        List<Message> res = memory.get("conv5", 12L, 1);
        assertEquals(1, res.size());
        assertEquals("r", res.get(0).getText());
        verify(cache, atLeastOnce()).set(eq(cache.generateChatMemoryKey("conv5", 12L)), anyList(), anyLong());
    }

    @Test
    void testClearCache_success_and_exception() {
        doNothing().when(cache).delete(anyString());
        assertDoesNotThrow(() -> memory.clearCache("conv6", 13L));
        doThrow(new RuntimeException("del err")).when(cache).delete(anyString());
        assertDoesNotThrow(() -> memory.clearCache("conv6", 13L));
    }

    @Test
    void testAdd_cacheUpdateFails_stillSucceeds() {
        when(cache.isRedisAvailable()).thenReturn(true);
        when(mapper.selectOne(any())).thenReturn(null, null);
        when(mapper.insert(any(KeepReport.class))).thenReturn(1);
        // 模拟缓存更新失败
        doThrow(new RuntimeException("cache update failed")).when(cache).set(anyString(), anyList(), anyLong());

        KeepReport stored = KeepReport.builder()
                .chatId("chat_conv7").userId(11L)
                .messages("[{\"messageType\":\"USER\",\"message\":\"test\"}]").build();
        when(mapper.selectOne(any())).thenReturn(stored);

        // 即使缓存更新失败，方法也应该成功（不抛出异常）
        assertDoesNotThrow(() -> memory.add("conv7", List.of(new UserMessage("test"))));

        // 验证数据库操作仍然执行了
        verify(mapper, atLeastOnce()).insert(any(KeepReport.class));
    }

    @Test
    void testGetLastNMessages_viaCacheHit() {
        when(cache.isRedisAvailable()).thenReturn(true);
        // 设置缓存包含多条消息，测试 getLastNMessages 的行为
        List<Message> fullMessages = List.of(
                new UserMessage("msg1"),
                new UserMessage("msg2"),
                new UserMessage("msg3"),
                new UserMessage("msg4"),
                new UserMessage("msg5")
        );
        when(cache.get(anyString(), eq(List.class))).thenReturn(fullMessages);

        // 请求最后3条消息
        List<Message> last3 = memory.get("conv8", 3);
        assertEquals(3, last3.size());
        assertEquals("msg3", last3.get(0).getText());
        assertEquals("msg4", last3.get(1).getText());
        assertEquals("msg5", last3.get(2).getText());

        // 请求最后10条消息（超过可用消息数）
        List<Message> last10 = memory.get("conv8", 10);
        assertEquals(5, last10.size()); // 应该返回所有5条消息

        // 请求0条消息
        List<Message> last0 = memory.get("conv8", 0);
        assertEquals(5, last0.size()); // 应该返回所有消息

        verify(cache, never()).set(anyString(), any(), anyLong());
    }

    @Test
    void testGet_cacheMissWithUserId_fallbackException() {
        when(cache.isRedisAvailable()).thenReturn(true);
        when(cache.get(anyString(), eq(List.class))).thenReturn(null);
        // 模拟数据库异常
        when(mapper.selectOne(any())).thenThrow(new RuntimeException("db error"));

        // 应该抛出异常，因为没有缓存且数据库失败
        assertThrows(RuntimeException.class, () -> memory.get("conv9", 14L, 1));
    }

    @Test
    void testUpdateCache_privateMethod_viaAdd() {
        when(cache.isRedisAvailable()).thenReturn(true);
        when(mapper.selectOne(any())).thenReturn(null, null);
        when(mapper.insert(any(KeepReport.class))).thenReturn(1);

        KeepReport stored = KeepReport.builder()
                .chatId("chat_conv10").userId(11L)
                .messages("[{\"messageType\":\"USER\",\"message\":\"update_cache_test\"}]").build();
        when(mapper.selectOne(any())).thenReturn(stored);

        memory.add("conv10", List.of(new UserMessage("update_cache_test")));

        // 验证 updateCache 被调用（通过 set 方法调用验证）
        verify(cache, atLeastOnce()).set(eq(cache.generateChatMemoryKey("conv10", 11L)), anyList(), eq(6L));
    }

}


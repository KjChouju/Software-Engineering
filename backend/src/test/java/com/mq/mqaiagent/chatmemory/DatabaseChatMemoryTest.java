package com.mq.mqaiagent.chatmemory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mq.mqaiagent.mapper.KeepReportMapper;
import com.mq.mqaiagent.model.dto.keepReport.KeepReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DatabaseChatMemoryTest {

    KeepReportMapper mapper;
    DatabaseChatMemory memory;

    @BeforeEach
    void setUp() {
        mapper = mock(KeepReportMapper.class);
        memory = new DatabaseChatMemory(mapper);
    }

    @Test
    void testAdd_withoutUserId_insertNewRecord() {
        List<Message> messages = List.of(
                new UserMessage("u1"),
                new AssistantMessage("{\"k\":\"v\"}"),
                new AssistantMessage("plain")
        );
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null, null);
        when(mapper.insert(any(KeepReport.class))).thenReturn(1);

        memory.add("convA", messages);

        ArgumentCaptor<KeepReport> captor = ArgumentCaptor.forClass(KeepReport.class);
        verify(mapper).insert(captor.capture());
        KeepReport inserted = captor.getValue();
        assertEquals("chat_convA", inserted.getChatId());
        assertNotNull(inserted.getMessages());
        assertTrue(inserted.getMessages().contains("\"messageType\":\"USER\""));
        assertTrue(inserted.getMessages().contains("\"messageType\":\"ASSISTANT\""));
    }

    @Test
    void testAdd_withUserId_updateExistingRecord() {
        memory.setCurrentUserId(100L);
        List<Message> messages = List.of(new UserMessage("hi"));
        KeepReport existing = KeepReport.builder()
                .id(1L).chatId("chat_convB").userId(100L).messages("[]").lastMessage("old").build();
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null, existing);
        when(mapper.updateById(any(KeepReport.class))).thenReturn(1);

        memory.add("convB", messages);

        verify(mapper).updateById(any(KeepReport.class));
    }

    @Test
    void testGet_returnsLastNMessages_withoutUserId() {
        // First insert via add to capture KeepReport
        List<Message> messages = List.of(
                new UserMessage("m1"),
                new AssistantMessage("{\"a\":1}"),
                new AssistantMessage("m3")
        );
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null, null);
        when(mapper.insert(any(KeepReport.class))).then(invocation -> 1);
        memory.add("convC", messages);

        // Capture inserted
        ArgumentCaptor<KeepReport> captor = ArgumentCaptor.forClass(KeepReport.class);
        verify(mapper).insert(captor.capture());
        KeepReport stored = captor.getValue();

        // Now stub selectOne to return stored record when get()
        reset(mapper);
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(stored);

        List<Message> last2 = memory.get("convC", 2);
        assertEquals(2, last2.size());
        assertEquals("m3", last2.get(1).getText());
        assertTrue(last2.get(0).getText().contains("\"a\":1"));
    }

    @Test
    void testGet_withUserId_path() {
        memory.setCurrentUserId(200L);
        List<Message> messages = List.of(new UserMessage("x1"), new UserMessage("x2"));
        // Prepare a stored record with serialized messages
        KeepReport stored = KeepReport.builder()
                .chatId("chat_convD")
                .userId(200L)
                .messages("[{\"messageType\":\"USER\",\"message\":\"x1\"},{\"messageType\":\"USER\",\"message\":\"x2\"}]")
                .build();
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(stored);

        List<Message> last1 = memory.get("convD", 1);
        assertEquals(1, last1.size());
        assertEquals("x2", last1.get(0).getText());
    }

    @Test
    void testClear_successAndNotFoundAndException() {
        // success
        when(mapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        assertDoesNotThrow(() -> memory.clear("convE"));
        // not found
        when(mapper.delete(any(LambdaQueryWrapper.class))).thenReturn(0);
        assertDoesNotThrow(() -> memory.clear("convE"));
        // exception
        when(mapper.delete(any(LambdaQueryWrapper.class))).thenThrow(new RuntimeException("db err"));
        assertDoesNotThrow(() -> memory.clear("convE"));
    }

    @Test
    void testSaveConversation_updateWarnAndInsertErrorBranches() {
        List<Message> messages = new ArrayList<>();
        messages.add(new UserMessage("short"));
        messages.add(new AssistantMessage("a".repeat(300))); // long content for lastMessage trimming path

        // First path: existingReport -> updateById returns 0 triggers warn
        KeepReport existing = KeepReport.builder()
                .id(1L).chatId("chat_convF").messages("[]").lastMessage("old").build();
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
        when(mapper.updateById(any(KeepReport.class))).thenReturn(0);
        memory.add("convF", messages);

        // Second path: selectOne returns null -> insert path, but insert returns 0 -> error log
        reset(mapper);
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null, null);
        when(mapper.insert(any(KeepReport.class))).thenReturn(0);
        memory.add("convF2", messages);
    }

    @Test
    void testGetLastMessageContent_defaultAndTrim() {
        // Use userId path to capture lastMessage
        memory.setCurrentUserId(300L);
        List<Message> empty = List.of();
        // Insert with empty messages -> lastMessage should be default
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null, null);
        when(mapper.insert(any(KeepReport.class))).thenReturn(1);
        memory.add("convG", empty);
        ArgumentCaptor<KeepReport> captor1 = ArgumentCaptor.forClass(KeepReport.class);
        verify(mapper).insert(captor1.capture());
        assertEquals("暂无消息", captor1.getValue().getLastMessage());

        // Insert with long message -> trimmed
        reset(mapper);
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null, null);
        when(mapper.insert(any(KeepReport.class))).thenReturn(1);
        List<Message> longMsg = List.of(new AssistantMessage("x".repeat(250)));
        memory.add("convH", longMsg);
        ArgumentCaptor<KeepReport> captor2 = ArgumentCaptor.forClass(KeepReport.class);
        verify(mapper).insert(captor2.capture());
        String lm = captor2.getValue().getLastMessage();
        assertTrue(lm.length() >= 203);
        assertTrue(lm.endsWith("..."));
    }
}


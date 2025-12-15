package com.mq.mqaiagent.service.impl;

import com.mq.mqaiagent.mapper.KeepReportMapper;
import com.mq.mqaiagent.model.dto.ChatHistoryDetailDTO;
import com.mq.mqaiagent.model.dto.ChatHistoryListDTO;
import com.mq.mqaiagent.model.dto.keepReport.KeepReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatHistoryServiceImplTest {

    @Mock
    private KeepReportMapper keepReportMapper;

    @InjectMocks
    private ChatHistoryServiceImpl service;

    @Test
    void testGetChatHistoryList_ok() {
        List<KeepReport> reports = new ArrayList<>();
        KeepReport r1 = new KeepReport();
        r1.setChatId("c1");
        r1.setLastMessage("m1");
        r1.setCreateTime(new Date());
        r1.setUpdateTime(new Date());
        r1.setIsDelete(0);
        r1.setUserId(1L);
        reports.add(r1);
        KeepReport r2 = new KeepReport();
        r2.setChatId("c2");
        r2.setLastMessage("m2");
        r2.setCreateTime(new Date());
        r2.setUpdateTime(new Date());
        r2.setIsDelete(0);
        r2.setUserId(1L);
        reports.add(r2);
        when(keepReportMapper.selectList(any())).thenReturn(reports);

        List<ChatHistoryListDTO> list = service.getChatHistoryList(1L);
        assertEquals(2, list.size());
        assertEquals("c1", list.get(0).getChatId());
        assertEquals("m1", list.get(0).getLastMessage());
    }

    @Test
    void testGetChatHistoryDetail_ok() {
        KeepReport report = new KeepReport();
        report.setUserId(1L);
        report.setChatId("cid");
        report.setIsDelete(0);
        report.setMessages("[{\"messageType\":\"user\",\"message\":\"hi\"}," +
                "{\"messageType\":\"assistant\",\"message\":{\"content\":\"ok\"}}]");
        when(keepReportMapper.selectOne(any())).thenReturn(report);

        ChatHistoryDetailDTO dto = service.getChatHistoryDetail(1L, "cid");
        assertNotNull(dto);
        assertEquals("cid", dto.getChatId());
        assertEquals(2, dto.getMessages().size());
        assertEquals("user", dto.getMessages().get(0).getMessageType());
        assertTrue(dto.getMessages().get(1).getMessage().contains("content"));
    }

    @Test
    void testDeleteChatHistory_ok() {
        when(keepReportMapper.update(any(), any())).thenReturn(1);
        assertTrue(service.deleteChatHistory(1L, "cid"));
        when(keepReportMapper.update(any(), any())).thenReturn(0);
        assertFalse(service.deleteChatHistory(1L, "cid"));
    }

    @Test
    void testGetChatHistoryList_mapperThrows_returnsEmpty() {
        when(keepReportMapper.selectList(any())).thenThrow(new RuntimeException("db fail"));
        List<?> list = service.getChatHistoryList(1L);
        assertTrue(list.isEmpty());
    }

    @Test
    void testGetChatHistoryDetail_noReport_returnsNull() {
        when(keepReportMapper.selectOne(any())).thenReturn(null);
        assertNull(service.getChatHistoryDetail(1L, "cid"));
    }

    @Test
    void testGetChatHistoryDetail_blankMessages_returnsNull() {
        KeepReport r = new KeepReport();
        r.setMessages("   ");
        when(keepReportMapper.selectOne(any())).thenReturn(r);
        assertNull(service.getChatHistoryDetail(1L, "cid"));
    }

    @Test
    void testGetChatHistoryDetail_invalidJson_returnsEmptyMessages() {
        KeepReport r = new KeepReport();
        r.setMessages("not-json");
        when(keepReportMapper.selectOne(any())).thenReturn(r);
        ChatHistoryDetailDTO dto = service.getChatHistoryDetail(1L, "cid");
        assertNotNull(dto);
        assertTrue(dto.getMessages().isEmpty());
    }

    @Test
    void testDeleteChatHistory_updateThrows_returnsFalse() {
        when(keepReportMapper.update(any(), any())).thenThrow(new RuntimeException("db fail"));
        assertFalse(service.deleteChatHistory(1L, "cid"));
    }
}

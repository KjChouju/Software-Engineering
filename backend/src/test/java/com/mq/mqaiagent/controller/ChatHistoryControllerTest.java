package com.mq.mqaiagent.controller;

import com.mq.mqaiagent.model.dto.ChatHistoryDetailDTO;
import com.mq.mqaiagent.model.dto.ChatHistoryListDTO;
import com.mq.mqaiagent.model.entity.User;
import com.mq.mqaiagent.service.ChatHistoryService;
import com.mq.mqaiagent.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.mq.mqaiagent.exception.GlobalExceptionHandler;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/**
 * ChatHistoryController 行为测试
 */
class ChatHistoryControllerTest {

    private MockMvc mockMvc;
    private ChatHistoryController chatHistoryController;

    private ChatHistoryService chatHistoryService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        chatHistoryController = new ChatHistoryController();
        chatHistoryService = Mockito.mock(ChatHistoryService.class);
        userService = Mockito.mock(UserService.class);

        setPrivateField(chatHistoryController, "chatHistoryService", chatHistoryService);
        setPrivateField(chatHistoryController, "userService", userService);

        mockMvc = MockMvcBuilders.standaloneSetup(chatHistoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetChatHistoryList_success() throws Exception {
        User user = new User();
        user.setId(100L);
        when(userService.getLoginUser(any())).thenReturn(user);

        ChatHistoryListDTO item = new ChatHistoryListDTO();
        item.setChatId("chat-1");
        when(chatHistoryService.getChatHistoryList(eq(100L)))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/chat/history/list")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("chat-1")));
    }

    @Test
    void testGetChatHistoryList_notLoggedIn() throws Exception {
        when(userService.getLoginUser(any())).thenThrow(new com.mq.mqaiagent.exception.BusinessException(com.mq.mqaiagent.common.ErrorCode.NOT_LOGIN_ERROR));

        mockMvc.perform(get("/chat/history/list")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("未登录")));
    }

    @Test
    void testGetChatHistoryList_systemError() throws Exception {
        User user = new User();
        user.setId(100L);
        when(userService.getLoginUser(any())).thenReturn(user);
        when(chatHistoryService.getChatHistoryList(eq(100L))).thenThrow(new RuntimeException("x"));

        mockMvc.perform(get("/chat/history/list")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("系统异常")));
    }

    @Test
    void testGetChatHistoryDetail_success() throws Exception {
        User user = new User();
        user.setId(200L);
        when(userService.getLoginUser(any())).thenReturn(user);

        ChatHistoryDetailDTO detail = new ChatHistoryDetailDTO();
        detail.setChatId("chat-2");
        when(chatHistoryService.getChatHistoryDetail(eq(200L), eq("chat-2")))
                .thenReturn(detail);

        mockMvc.perform(get("/chat/history/detail")
                        .param("chatId", "chat-2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("chat-2")));
    }

    @Test
    void testGetChatHistoryDetail_blankChatId() throws Exception {
        mockMvc.perform(get("/chat/history/detail")
                        .param("chatId", "")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("对话ID不能为空")));
    }

    @Test
    void testGetChatHistoryDetail_notFound() throws Exception {
        User user = new User();
        user.setId(200L);
        when(userService.getLoginUser(any())).thenReturn(user);
        when(chatHistoryService.getChatHistoryDetail(eq(200L), eq("chat-x"))).thenReturn(null);

        mockMvc.perform(get("/chat/history/detail")
                        .param("chatId", "chat-x")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("对话记录不存在")));
    }

    @Test
    void testGetChatHistoryDetail_notLoggedIn() throws Exception {
        when(userService.getLoginUser(any())).thenThrow(new com.mq.mqaiagent.exception.BusinessException(com.mq.mqaiagent.common.ErrorCode.NOT_LOGIN_ERROR));

        mockMvc.perform(get("/chat/history/detail")
                        .param("chatId", "chat-2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("未登录")));
    }

    @Test
    void testGetChatHistoryDetail_systemError() throws Exception {
        User user = new User();
        user.setId(200L);
        when(userService.getLoginUser(any())).thenReturn(user);
        when(chatHistoryService.getChatHistoryDetail(eq(200L), eq("chat-2")))
                .thenThrow(new RuntimeException("x"));

        mockMvc.perform(get("/chat/history/detail")
                        .param("chatId", "chat-2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("系统异常")));
    }

    @Test
    void testDeleteChatHistory_success() throws Exception {
        User user = new User();
        user.setId(300L);
        when(userService.getLoginUser(any())).thenReturn(user);
        when(chatHistoryService.deleteChatHistory(eq(300L), eq("chat-3")))
                .thenReturn(true);

        mockMvc.perform(delete("/chat/history/delete")
                        .param("chatId", "chat-3")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testDeleteChatHistory_blankChatId() throws Exception {
        mockMvc.perform(delete("/chat/history/delete")
                        .param("chatId", "")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("对话ID不能为空")));
    }

    @Test
    void testDeleteChatHistory_notLoggedIn() throws Exception {
        when(userService.getLoginUser(any())).thenThrow(new com.mq.mqaiagent.exception.BusinessException(com.mq.mqaiagent.common.ErrorCode.NOT_LOGIN_ERROR));

        mockMvc.perform(delete("/chat/history/delete")
                        .param("chatId", "chat-3")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("未登录")));
    }

    @Test
    void testDeleteChatHistory_operationError() throws Exception {
        User user = new User();
        user.setId(300L);
        when(userService.getLoginUser(any())).thenReturn(user);
        when(chatHistoryService.deleteChatHistory(eq(300L), eq("chat-3")))
                .thenReturn(false);

        mockMvc.perform(delete("/chat/history/delete")
                        .param("chatId", "chat-3")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("删除失败")));
    }

    @Test
    void testDeleteChatHistory_systemError() throws Exception {
        User user = new User();
        user.setId(300L);
        when(userService.getLoginUser(any())).thenReturn(user);
        when(chatHistoryService.deleteChatHistory(eq(300L), eq("chat-3")))
                .thenThrow(new RuntimeException("x"));

        mockMvc.perform(delete("/chat/history/delete")
                        .param("chatId", "chat-3")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("系统异常")));
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

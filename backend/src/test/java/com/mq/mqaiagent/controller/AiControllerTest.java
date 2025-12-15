package com.mq.mqaiagent.controller;

import com.google.common.util.concurrent.RateLimiter;
import com.mq.mqaiagent.app.KeepApp;
import com.mq.mqaiagent.config.RateLimiterConfig;
import com.mq.mqaiagent.pool.ChatClientPool;
import com.mq.mqaiagent.model.entity.User;
import com.mq.mqaiagent.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.mq.mqaiagent.exception.GlobalExceptionHandler;
import reactor.core.publisher.Flux;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AiController 集成行为测试（流式接口）
 */
class AiControllerTest {

    private MockMvc mockMvc;
    private AiController aiController;

    private UserService userService;
    private RateLimiter aiRateLimiter;
    private RateLimiterConfig.UserRateLimiterManager userRateLimiterManager;
    private KeepApp keepApp;
    private ChatClientPool chatClientPool;
    private org.springframework.ai.tool.ToolCallback[] allTools;

    @BeforeEach
    void setUp() {
        aiController = new AiController();

        userService = Mockito.mock(UserService.class);
        aiRateLimiter = Mockito.mock(RateLimiter.class);
        userRateLimiterManager = Mockito.mock(RateLimiterConfig.UserRateLimiterManager.class);
        keepApp = Mockito.mock(KeepApp.class);
        chatClientPool = Mockito.mock(ChatClientPool.class);
        allTools = new org.springframework.ai.tool.ToolCallback[]{};

        // 通过反射设置依赖
        setPrivateField(aiController, "userService", userService);
        setPrivateField(aiController, "aiRateLimiter", aiRateLimiter);
        setPrivateField(aiController, "userRateLimiterManager", userRateLimiterManager);
        setPrivateField(aiController, "keepApp", keepApp);
        setPrivateField(aiController, "chatClientPool", chatClientPool);
        setPrivateField(aiController, "allTools", allTools);

        mockMvc = MockMvcBuilders.standaloneSetup(aiController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testDoChatWithKeepAppSSEUser_success() throws Exception {
        String message = "hello";
        String chatId = "chat-1";

        User user = new User();
        user.setId(100L);

        when(userService.getLoginUser(any())).thenReturn(user);
        when(userRateLimiterManager.tryAcquire(eq(100L), anyLong(), org.mockito.ArgumentMatchers.any(java.util.concurrent.TimeUnit.class))).thenReturn(true);
        when(aiRateLimiter.tryAcquire()).thenReturn(true);
        when(keepApp.doChatByStream(eq(message), eq(chatId), eq(100L)))
                .thenReturn(Flux.just("chunk1", "chunk2"));

        mockMvc.perform(get("/ai/keep_app/chat/sse/user")
                        .param("message", message)
                        .param("chatId", chatId)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }

    @Test
    void testDoChatWithKeepAppServerSentEvent_success() throws Exception {
        String message = "hi";
        String chatId = "chat-2";

        when(keepApp.doChatByStream(eq(message), eq(chatId)))
                .thenReturn(Flux.just("a", "b", "c"));

        mockMvc.perform(get("/ai/keep_app/chat/server_sent_event")
                        .param("message", message)
                        .param("chatId", chatId)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }

    @Test
    void testDoChatWithKeepAppServerSentEvent_emptyParams() throws Exception {
        when(keepApp.doChatByStream(eq(""), eq("")))
                .thenReturn(Flux.just("p"));

        mockMvc.perform(get("/ai/keep_app/chat/server_sent_event")
                        .param("message", "")
                        .param("chatId", "")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }

    @Test
    void testDoChatWithKeepAppSseEmitter_success() throws Exception {
        String message = "hey";
        String chatId = "chat-3";

        when(keepApp.doChatByStream(eq(message), eq(chatId)))
                .thenReturn(Flux.just("x", "y"));

        mockMvc.perform(get("/ai/keep_app/chat/sse/emitter")
                        .param("message", message)
                        .param("chatId", chatId)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk());
    }

    @Test
    void testDoChatWithKeepAppSSEUser_userRateLimitExceeded() throws Exception {
        String message = "hello";
        String chatId = "chat-1";
        User user = new User();
        user.setId(100L);
        when(userService.getLoginUser(any())).thenReturn(user);
        when(userRateLimiterManager.tryAcquire(eq(100L), anyLong(), org.mockito.ArgumentMatchers.any(java.util.concurrent.TimeUnit.class))).thenReturn(false);
        mockMvc.perform(get("/ai/keep_app/chat/sse/user")
                        .param("message", message)
                        .param("chatId", chatId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("请求过于频繁")));
    }

    @Test
    void testDoChatWithKeepAppSSEUser_aiRateLimitExceeded() throws Exception {
        String message = "hello";
        String chatId = "chat-1";
        User user = new User();
        user.setId(100L);
        when(userService.getLoginUser(any())).thenReturn(user);
        when(userRateLimiterManager.tryAcquire(eq(100L), anyLong(), org.mockito.ArgumentMatchers.any(java.util.concurrent.TimeUnit.class))).thenReturn(true);
        when(aiRateLimiter.tryAcquire()).thenReturn(false);
        mockMvc.perform(get("/ai/keep_app/chat/sse/user")
                        .param("message", message)
                        .param("chatId", chatId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("系统繁忙")));
    }

    @Test
    void testDoChatWithKeepAppSSEUser_notLoggedIn() throws Exception {
        when(userService.getLoginUser(any())).thenThrow(new com.mq.mqaiagent.exception.BusinessException(com.mq.mqaiagent.common.ErrorCode.NOT_LOGIN_ERROR));
        mockMvc.perform(get("/ai/keep_app/chat/sse/user")
                        .param("message", "m")
                        .param("chatId", "c"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("未登录")));
    }
    @Test
    void testDoChatWithKeepAppServerSentEvent_ioCatch() throws Exception {
        when(keepApp.doChatByStream(eq("io"), eq("id")))
                .thenReturn(Flux.just("__IO_ERROR__"));
        mockMvc.perform(get("/ai/keep_app/chat/server_sent_event")
                        .param("message", "io")
                        .param("chatId", "id")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk());
    }

    @Test
    void testDoChatWithKeepAppSseEmitter_ioCatch() throws Exception {
        when(keepApp.doChatByStream(eq("io2"), eq("id2")))
                .thenReturn(Flux.just("__IO_ERROR__"));
        mockMvc.perform(get("/ai/keep_app/chat/sse/emitter")
                        .param("message", "io2")
                        .param("chatId", "id2")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk());
    }

    @Test
    void testDoChatWithManus_success() throws Exception {
        mockMvc.perform(get("/ai/manus/chat")
                        .param("message", "m"))
                .andExpect(status().isOk());
    }

    @Test
    void testDoChatWithManusUser_success() throws Exception {
        User user = new User();
        user.setId(200L);
        when(userService.getLoginUser(any())).thenReturn(user);
        mockMvc.perform(get("/ai/manus/chat/user")
                        .param("message", "m")
                        .param("chatId", "cid"))
                .andExpect(status().isOk());
    }

    @Test
    void testDoChatWithManusUser_notLoggedIn() throws Exception {
        when(userService.getLoginUser(any())).thenThrow(new com.mq.mqaiagent.exception.BusinessException(com.mq.mqaiagent.common.ErrorCode.NOT_LOGIN_ERROR));
        mockMvc.perform(get("/ai/manus/chat/user")
                        .param("message", "m")
                        .param("chatId", "cid"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("未登录")));
    }
    @Test
    void testDoChatWithKeepAppServerSentEvent_error() throws Exception {
        when(keepApp.doChatByStream(eq("err"), eq("iderr")))
                .thenReturn(Flux.error(new RuntimeException("boom")));
        mockMvc.perform(get("/ai/keep_app/chat/server_sent_event")
                        .param("message", "err")
                        .param("chatId", "iderr")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk());
    }

    @Test
    void testDoChatWithKeepAppSseEmitter_error() throws Exception {
        when(keepApp.doChatByStream(eq("err2"), eq("iderr2")))
            .thenReturn(Flux.error(new RuntimeException("boom2")));
        mockMvc.perform(get("/ai/keep_app/chat/sse/emitter")
                        .param("message", "err2")
                        .param("chatId", "iderr2")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk());
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

package com.mq.mqaiagent.app;

import cn.hutool.core.lang.UUID;
import com.mq.mqaiagent.advisor.ForbiddenWordAdvisor;
import org.mockito.Mockito;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.mq.mqaiagent.service.AiResponseCacheService;
import com.mq.mqaiagent.pool.ChatClientPool;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.function.Consumer;


/**
 * ClassName：KeepAppTest
 * Package:com.mq.mqaiagent.app
 * Description: 测试健身助手
 */
@SpringBootTest
class KeepAppTest {

    @Resource
    private KeepApp keepApp;
    @MockBean
    private AiResponseCacheService aiResponseCacheService;
    @MockBean
    private ChatClientPool chatClientPool;
    @MockBean
    private Advisor KeepAppRagCloudAdvisor;
    @MockBean
    private VectorStore keepAppVectorStore;

    private ChatClient deepClient;
    private ChatResponse deepResponse;

    @BeforeEach
    void initClient() {
        deepClient = mock(ChatClient.class, Mockito.RETURNS_DEEP_STUBS);
        deepResponse = mock(ChatResponse.class, Mockito.RETURNS_DEEP_STUBS);
        when(deepResponse.getResult().getOutput().getText()).thenReturn("answer");
        when(deepClient.prompt().user(anyString()).advisors(any(Consumer.class)).call().chatResponse()).thenReturn(deepResponse);
        ReflectionTestUtils.setField(keepApp, "chatClient", deepClient);
    }

    @Test
    void testChat() {
        // 设置缓存服务mock - 总是返回null（缓存未命中）
        when(aiResponseCacheService.getCachedResponse(anyString(), any())).thenReturn(null);

        String chatId = UUID.randomUUID().toString();
        // 第一轮
        String message = "你好，我是LMQ，我最近在健身方面遇到了一些问题，你能帮我吗？";
        String answer = keepApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // 第二轮
        message = "我最近在增肌方面遇到了一些问题，你能帮我吗？";
        answer = keepApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // 第三轮
        message = "我在哪方面遇到了一些问题，你知道吗？";
        answer = keepApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // 包含违禁词输入测试，验证是否抛出异常
        String prohibitedMessage = "自拍";
        Assertions.assertThrows(
                ForbiddenWordAdvisor.ProhibitedWordException.class, () -> keepApp.doChat(prohibitedMessage, chatId),
                "Expected ProhibitedWordException for prohibited message"
        );
    }

    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        String message = "我是LMQ，我想通过健身来增肌，请你给我一些健身的建议";
        String answer = String.valueOf(keepApp.doChatWithReport(message, chatId));
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "如何避免肩部撞击？";
        when(deepClient.prompt().user(anyString()).advisors(any(Consumer.class)).advisors(any(Advisor.class)).advisors(any(Advisor.class)).call().chatResponse())
                .thenReturn(deepResponse);
        String answer = keepApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithTools() {        // 设置mock
        when(deepClient.prompt().user(anyString()).advisors(any(Consumer.class)).advisors(any(Advisor.class)).tools(any(ToolCallback[].class)).call().chatResponse())
                .thenReturn(deepResponse);
        // 测试联网搜索问题的答案
        testMessage("如何避免肩部撞击？");

        // 测试网页抓取：健身案例分析
        testMessage("最近我想通过健身来增肌，请你帮我找找如何科学的增肌？https://xiaolincoding.com/");

        // 测试资源下载：图片下载
        testMessage("帮我下载一张布布一二图片为文件");

        // 测试文件操作：保存用户档案
        testMessage("保存我的健身计划为文件");

        // 测试 PDF 生成
        testMessage("生成一份‘增肌健身计划’PDF，包含健身项目、健身动作和健身方式");
    }

    private void testMessage(String message) {
        String chatId = UUID.randomUUID().toString();
        String answer = keepApp.doChatWithTools(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChat_cachedBranch() {
        String chatId = UUID.randomUUID().toString();
        String message = "缓存命中";
        when(aiResponseCacheService.getCachedResponse(eq(message), isNull())).thenReturn("cached");
        String answer = keepApp.doChat(message, chatId);
        Assertions.assertEquals("cached", answer);
        verify(aiResponseCacheService, never()).cacheResponse(anyString(), anyString(), any());
    }

    @Test
    void doChat_defaultClient_branch() {
        String chatId = UUID.randomUUID().toString();
        String message = "正常对话";
        when(aiResponseCacheService.getCachedResponse(eq(message), isNull())).thenReturn(null);
        when(deepClient.prompt().user(anyString()).advisors(any(Consumer.class)).call().chatResponse()).thenReturn(deepResponse);
        String answer = keepApp.doChat(message, chatId);
        Assertions.assertEquals("answer", answer);
        verify(aiResponseCacheService, times(1)).cacheResponse(eq(message), eq("answer"), isNull());
    }

    @Test
    void doChatByStream_default() {
        String chatId = UUID.randomUUID().toString();
        String message = "流式";
        when(deepClient.prompt().user(anyString()).advisors(any(Consumer.class)).stream().content())
                .thenReturn(Flux.just("c1", "c2"));
        Flux<String> flux = keepApp.doChatByStream(message, chatId);
        Assertions.assertEquals(2, flux.collectList().block().size());
    }

    @Test
    void doChatByStream_withUserId() {
        String chatId = UUID.randomUUID().toString();
        String message = "流式用户";
        ChatClient userClient = mock(ChatClient.class, Mockito.RETURNS_DEEP_STUBS);
        when(chatClientPool.getKeepAppClientWithMemory(anyLong(), anyString())).thenReturn(userClient);
        when(userClient.prompt().user(anyString()).advisors(any(Consumer.class)).stream().content())
                .thenReturn(Flux.just("u1", "u2", "u3"));
        Flux<String> flux = keepApp.doChatByStream(message, chatId, 1L);
        Assertions.assertEquals(3, flux.collectList().block().size());
    }
}

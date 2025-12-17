package com.mq.mqaiagent.agent;

import com.mq.mqaiagent.agent.model.AgentState;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ToolCallAgentTest {

    private ChatClient mockChatClientWithAssistant(AssistantMessage assistantMessage, ChatResponse chatResponse) {
        ChatClient chatClient = mock(ChatClient.class, Mockito.RETURNS_DEEP_STUBS);
        when(chatClient.prompt(any(Prompt.class)).system(anyString()).tools(any(ToolCallback[].class)).call().chatResponse())
                .thenReturn(chatResponse);
        when(chatResponse.getResult().getOutput()).thenReturn(assistantMessage);
        when(chatResponse.hasToolCalls()).thenReturn(!assistantMessage.getToolCalls().isEmpty());
        return chatClient;
    }

    @Test
    void testThink_noToolCalls_recordsAssistantAndReturnsFalse() {
        ToolCallAgent agent = new ToolCallAgent(new ToolCallback[]{});
        agent.setSystemPrompt("sys");
        agent.setNextStepPrompt("next");
        AssistantMessage assistantMessage = mock(AssistantMessage.class, Mockito.RETURNS_DEEP_STUBS);
        when(assistantMessage.getText()).thenReturn("no tools");
        when(assistantMessage.getToolCalls()).thenReturn(List.of());
        ChatResponse chatResponse = mock(ChatResponse.class, Mockito.RETURNS_DEEP_STUBS);
        ChatClient chatClient = mockChatClientWithAssistant(assistantMessage, chatResponse);
        agent.setChatClient(chatClient);

        boolean shouldAct = agent.think();
        assertFalse(shouldAct);
        assertEquals(chatResponse, ReflectionTestUtils.getField(agent, "toolCallChatResponse"));
        // assistant message added to context
        assertTrue(agent.getMessageList().stream().anyMatch(m -> m == assistantMessage));
        // next step prompt was appended
        assertTrue(agent.getMessageList().get(0).getText().contains("next"));
    }

    @Test
    void testThink_withToolCalls_returnsTrue_withoutRecordingAssistant() {
        ToolCallAgent agent = new ToolCallAgent(new ToolCallback[]{});
        agent.setSystemPrompt("sys");
        agent.setNextStepPrompt("go");
        AssistantMessage.ToolCall toolCall = mock(AssistantMessage.ToolCall.class);
        when(toolCall.name()).thenReturn("toolA");
        when(toolCall.arguments()).thenReturn("{\"a\":1}");
        AssistantMessage assistantMessage = mock(AssistantMessage.class, Mockito.RETURNS_DEEP_STUBS);
        when(assistantMessage.getText()).thenReturn("use tools");
        when(assistantMessage.getToolCalls()).thenReturn(List.of(toolCall));
        ChatResponse chatResponse = mock(ChatResponse.class, Mockito.RETURNS_DEEP_STUBS);
        ChatClient chatClient = mockChatClientWithAssistant(assistantMessage, chatResponse);
        agent.setChatClient(chatClient);

        boolean shouldAct = agent.think();
        assertTrue(shouldAct);
        // assistant message should NOT be appended when tools present
        assertFalse(agent.getMessageList().stream().anyMatch(m -> m == assistantMessage));
    }

    @Test
    void testThink_exception_addsAssistantErrorAndReturnsFalse() {
        ToolCallAgent agent = new ToolCallAgent(new ToolCallback[]{});
        agent.setSystemPrompt("sys");
        agent.setNextStepPrompt("x");
        ChatClient chatClient = mock(ChatClient.class, Mockito.RETURNS_DEEP_STUBS);
        when(chatClient.prompt(any(Prompt.class)).system(anyString()).tools(any(ToolCallback[].class)).call().chatResponse())
                .thenThrow(new RuntimeException("boom"));
        agent.setChatClient(chatClient);

        boolean shouldAct = agent.think();
        assertFalse(shouldAct);
        assertTrue(agent.getMessageList().stream().anyMatch(m -> m.getText().startsWith("处理时遇到错误: ")));
    }

    @Test
    void testAct_noToolCalls_returnsMessage() {
        ToolCallAgent agent = new ToolCallAgent(new ToolCallback[]{});
        ChatResponse chatResponse = mock(ChatResponse.class);
        when(chatResponse.hasToolCalls()).thenReturn(false);
        ReflectionTestUtils.setField(agent, "toolCallChatResponse", chatResponse);
        String res = agent.act();
        assertEquals("没有工具需要调用", res);
    }

    @Test
    void testAct_withToolCalls_setsFinishedAndReturnsAggregatedResults() {
        ToolCallAgent agent = new ToolCallAgent(new ToolCallback[]{});
        ChatResponse chatResponse = mock(ChatResponse.class);
        when(chatResponse.hasToolCalls()).thenReturn(true);
        ReflectionTestUtils.setField(agent, "toolCallChatResponse", chatResponse);

        ToolCallingManager manager = mock(ToolCallingManager.class);
        ReflectionTestUtils.setField(agent, "toolCallingManager", manager);

        ToolExecutionResult execResult = mock(ToolExecutionResult.class);
        ToolResponseMessage toolRespMsg = mock(ToolResponseMessage.class);
        List<Object> history = new ArrayList<>();
        history.add(toolRespMsg);
        when(execResult.conversationHistory()).thenReturn((List) history);

        ToolResponseMessage.ToolResponse r1 = new ToolResponseMessage.ToolResponse("id1", "doTerminate", "done");
        ToolResponseMessage.ToolResponse r2 = new ToolResponseMessage.ToolResponse("id2", "crawl", "<html/>");
        when(toolRespMsg.getResponses()).thenReturn(List.of(r1, r2));

        when(manager.executeToolCalls(any(Prompt.class), eq(chatResponse))).thenReturn(execResult);

        String res = agent.act();
        assertTrue(res.contains("工具 doTerminate 返回的结果：done"));
        assertTrue(res.contains("工具 crawl 返回的结果：<html/>"));
        assertEquals(AgentState.FINISHED, agent.getState());
    }
}


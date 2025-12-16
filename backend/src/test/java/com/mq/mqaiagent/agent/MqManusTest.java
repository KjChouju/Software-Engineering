package com.mq.mqaiagent.agent;

import com.mq.mqaiagent.chatmemory.DatabaseChatMemory;
import com.mq.mqaiagent.pool.ChatClientPool;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MqManusTest {

    @Test
    void testConstructor_withChatModel_setsFieldsAndClient() {
        ToolCallback[] tools = new ToolCallback[]{};
        ChatModel model = mock(ChatModel.class);
        MqManus agent = new MqManus(tools, model);
        assertEquals("mqmanus", agent.getName());
        assertNotNull(agent.getSystemPrompt());
        assertNotNull(agent.getNextStepPrompt());
        assertEquals(20, agent.getMaxSteps());
        assertNotNull(agent.getChatClient());
    }

    @Test
    void testConstructor_withChatMemory_setsFieldsAndClient() {
        ToolCallback[] tools = new ToolCallback[]{};
        ChatModel model = mock(ChatModel.class);
        DatabaseChatMemory memory = mock(DatabaseChatMemory.class);
        MqManus agent = new MqManus(tools, model, memory);
        assertEquals("mqmanus", agent.getName());
        assertNotNull(agent.getChatClient());
    }

    @Test
    void testConstructor_withPool_noMemory_callsPoolAndSetsClient() {
        ToolCallback[] tools = new ToolCallback[]{};
        ChatClientPool pool = mock(ChatClientPool.class);
        ChatClient client = mock(ChatClient.class);
        when(pool.getMqManusClient(anyString())).thenReturn(client);

        MqManus agent = new MqManus(tools, pool);
        assertEquals("mqmanus", agent.getName());
        assertEquals(client, agent.getChatClient());
        verify(pool, times(1)).getMqManusClient(anyString());
    }

    @Test
    void testConstructor_withPool_andUserMemory_callsPoolWithUserId() {
        ToolCallback[] tools = new ToolCallback[]{};
        ChatClientPool pool = mock(ChatClientPool.class);
        ChatClient client = mock(ChatClient.class);
        when(pool.getMqManusClientWithMemory(eq(99L), anyString())).thenReturn(client);

        MqManus agent = new MqManus(tools, pool, 99L);
        assertEquals(client, agent.getChatClient());
        verify(pool, times(1)).getMqManusClientWithMemory(eq(99L), anyString());
    }
}


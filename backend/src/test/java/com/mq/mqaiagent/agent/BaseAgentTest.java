package com.mq.mqaiagent.agent;

import com.mq.mqaiagent.agent.model.AgentState;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BaseAgentTest {

    static class TestAgent extends BaseAgent {
        private final AtomicInteger cleaned = new AtomicInteger(0);
        private int c = 0;
        @Override
        public String step() {
            c++;
            return "ok-" + c;
        }
        @Override
        protected void cleanup() {
            cleaned.incrementAndGet();
        }
        int cleanedCount() { return cleaned.get(); }
    }

    static class ErrorAgent extends BaseAgent {
        @Override
        public String step() {
            throw new RuntimeException("step failed");
        }
    }

    @Test
    void testRun_success_collectsStepsAndFinishes() {
        TestAgent agent = new TestAgent();
        agent.setMaxSteps(3);
        String result = agent.run("prompt");
        assertTrue(result.contains("Step 1: ok-1"));
        assertTrue(result.contains("Step 2: ok-2"));
        assertTrue(result.contains("Step 3: ok-3"));
        assertTrue(result.contains("Terminated: Reached max steps (3)"));
        assertEquals(AgentState.FINISHED, agent.getState());
        assertEquals(1, agent.cleanedCount());
        assertEquals(1, agent.getMessageList().size());
    }

    @Test
    void testRun_error_setsErrorAndReturnsMessage() {
        ErrorAgent agent = new ErrorAgent();
        agent.setMaxSteps(1);
        String result = agent.run("x");
        assertTrue(result.startsWith("执行错误"));
        assertEquals(AgentState.ERROR, agent.getState());
    }

    @Test
    void testRun_nonIdle_throws() {
        TestAgent agent = new TestAgent();
        agent.setState(AgentState.RUNNING);
        assertThrows(RuntimeException.class, () -> agent.run("x"));
    }

    @Test
    void testRun_blankPrompt_throws() {
        TestAgent agent = new TestAgent();
        assertThrows(RuntimeException.class, () -> agent.run(""));
    }

    @Test
    void testRunStream_success_completesAndStateFinished() {
        TestAgent agent = new TestAgent();
        agent.setMaxSteps(1);
        agent.runStream("p");
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline && agent.getState() != AgentState.FINISHED) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {}
        }
        assertEquals(AgentState.FINISHED, agent.getState());
    }

    @Test
    void testRunStream_nonIdle_emitsErrorAndFinishes() {
        TestAgent agent = new TestAgent();
        agent.setState(AgentState.RUNNING);
        agent.runStream("p");
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline && agent.getState() != AgentState.FINISHED) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {}
        }
        assertEquals(AgentState.FINISHED, agent.getState());
    }

    @Test
    void testRunStream_stepThrows_setsErrorAndCompletes() {
        ErrorAgent agent = new ErrorAgent();
        agent.setMaxSteps(1);
        agent.runStream("p");
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline && agent.getState() != AgentState.ERROR) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {}
        }
        assertEquals(AgentState.ERROR, agent.getState());
    }
}

package com.mq.mqaiagent.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReActAgentTest {

    static class NoActAgent extends ReActAgent {
        @Override
        public boolean think() { return false; }
        @Override
        public String act() { return "act"; }
    }

    static class ThrowingThinkAgent extends ReActAgent {
        @Override
        public boolean think() { throw new RuntimeException("think fail"); }
        @Override
        public String act() { return "act"; }
    }

    @Test
    void testStep_noActBranch() {
        NoActAgent agent = new NoActAgent();
        String res = agent.step();
        assertEquals("思考完成 - 无需行动", res);
    }

    @Test
    void testStep_exceptionBranch() {
        ThrowingThinkAgent agent = new ThrowingThinkAgent();
        String res = agent.step();
        assertEquals("步骤执行失败: think fail", res);
    }
}


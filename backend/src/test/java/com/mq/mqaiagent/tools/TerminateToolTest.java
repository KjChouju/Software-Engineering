package com.mq.mqaiagent.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TerminateToolTest {

    @Test
    void testDoTerminate_returnsMessage() {
        TerminateTool tool = new TerminateTool();
        assertEquals("任务结束", tool.doTerminate());
    }
}


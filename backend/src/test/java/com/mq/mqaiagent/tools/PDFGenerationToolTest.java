package com.mq.mqaiagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ClassName：PDFGenerationToolTest
 * Package:com.mq.mqaiagent.tools
 * Description: PDF 生成工具测试类
 */
@SpringBootTest
class PDFGenerationToolTest {

    @Test
    void generatePDF_success() {
        PDFGenerationTool tool = new PDFGenerationTool();
        String fileName = "LMQICU.pdf";
        String content = "LMQICU RAG知识库构建、Tool Calling工具调用、MCP多模态服务";
        String result = tool.generatePDF(fileName, content);
        assertNotNull(result);
    }

    @Test
    void generatePDF_error_invalidFileName_returnsMessage() {
        PDFGenerationTool tool = new PDFGenerationTool();
        String result = tool.generatePDF("BAD:NAME.pdf", "content");
        assertNotNull(result);
        assertTrue(result.startsWith("Error generating PDF: "));
    }
}

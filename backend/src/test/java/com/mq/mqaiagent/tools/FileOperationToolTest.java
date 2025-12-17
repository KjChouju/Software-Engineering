package com.mq.mqaiagent.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ClassName：FileOperationToolTest
 * Package:com.mq.mqaiagent.tools
 * Description: 文件操作工具测试类
 */
class FileOperationToolTest {

    @Test
    public void testReadFile_error_nonExisting_returnsErrorMessage() {
        FileOperationTool tool = new FileOperationTool();
        String result = tool.readFile("__not_exists__.txt");
        assertNotNull(result);
        assertTrue(result.startsWith("Error reading file: "));
    }

    @Test
    public void testWriteFile_success_andReadBack() {
        FileOperationTool tool = new FileOperationTool();
        String fileName = "LMQICU.txt";
        String content = "https://xxx.online/ 云图库";
        String result = tool.writeFile(fileName, content);
        assertNotNull(result);
        String read = tool.readFile(fileName);
        assertTrue(read.contains("云图库"));
    }

    @Test
    public void testWriteFile_error_nullContent_returnsErrorMessage() {
        FileOperationTool tool = new FileOperationTool();
        String result = tool.writeFile("bad.txt", null);
        assertNotNull(result);
        assertTrue(result.startsWith("Error writing to file: "));
    }
}

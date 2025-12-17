package com.mq.mqaiagent.chatmemory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FileBasedChatMemoryTest {

    File baseDir;
    FileBasedChatMemory memory;

    @BeforeEach
    void setUp() {
        baseDir = new File(System.getProperty("java.io.tmpdir"), "fbcm-" + UUID.randomUUID());
        memory = new FileBasedChatMemory(baseDir.getAbsolutePath());
    }

    @AfterEach
    void tearDown() {
        if (baseDir != null && baseDir.exists()) {
            for (File f : baseDir.listFiles()) {
                f.delete();
            }
            baseDir.delete();
        }
    }

    @Test
    void testAddAndGet_success() {
        memory.add("c1", List.of(new UserMessage("a"), new UserMessage("b")));
        List<Message> last1 = memory.get("c1", 1);
        assertEquals(1, last1.size());
        assertEquals("b", last1.get(0).getText());
        List<Message> last10 = memory.get("c1", 10);
        assertEquals(2, last10.size());
    }

    @Test
    void testClear_removesFile() {
        memory.add("c2", List.of(new UserMessage("x")));
        File file = new File(baseDir, "c2.kryo");
        assertTrue(file.exists());
        memory.clear("c2");
        assertFalse(file.exists());
    }

    @Test
    void testGetOrCreate_tryCatch_readIOException() throws Exception {
        // Create a corrupted file that is not a Kryo stream
        File file = new File(baseDir, "badconv.kryo");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write("not-kryo-content".getBytes(StandardCharsets.UTF_8));
        }
        List<Message> res = memory.get("badconv", 10);
        assertNotNull(res);
    }

    @Test
    void testSaveConversation_tryCatch_writeFails() {
        // Create a directory named "bad.kryo" to cause FileOutputStream failure
        File badDir = new File(baseDir, "bad.kryo");
        assertTrue(badDir.mkdir());
        assertDoesNotThrow(() -> memory.add("bad", List.of(new UserMessage("will-fail"))));
    }
}


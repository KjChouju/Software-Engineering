package com.mq.mqaiagent.manager;

import com.mq.mqaiagent.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CosManagerTest {

    private CosManager cosManager;
    private COSClient cosClient;
    private CosClientConfig cosClientConfig;
    private File tempFile;

    @BeforeEach
    void setUp() throws Exception {
        cosManager = new CosManager();
        cosClient = Mockito.mock(COSClient.class);
        cosClientConfig = new CosClientConfig();
        cosClientConfig.setBucket("test-bucket");
        ReflectionTestUtils.setField(cosManager, "cosClient", cosClient);
        ReflectionTestUtils.setField(cosManager, "cosClientConfig", cosClientConfig);
        tempFile = File.createTempFile("cos-test", ".txt");
        tempFile.deleteOnExit();
    }

    @AfterEach
    void tearDown() {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    @Test
    void putObject_withPath_success() {
        PutObjectResult result = new PutObjectResult();
        when(cosClient.putObject(any())).thenReturn(result);
        PutObjectResult actual = cosManager.putObject("k1", tempFile.getAbsolutePath());
        assertSame(result, actual);
        verify(cosClient, times(1)).putObject(any());
    }

    @Test
    void putObject_withFile_success() {
        PutObjectResult result = new PutObjectResult();
        when(cosClient.putObject(any())).thenReturn(result);
        PutObjectResult actual = cosManager.putObject("k2", tempFile);
        assertSame(result, actual);
        verify(cosClient, times(1)).putObject(any());
    }

    @Test
    void putObject_withPath_clientThrows_propagates() {
        when(cosClient.putObject(any())).thenThrow(new RuntimeException("cos error"));
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> cosManager.putObject("k3", tempFile.getAbsolutePath()));
        assertEquals("cos error", e.getMessage());
        verify(cosClient, times(1)).putObject(any());
    }

    @Test
    void putObject_withFile_clientThrows_propagates() {
        when(cosClient.putObject(any())).thenThrow(new RuntimeException("cos error"));
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> cosManager.putObject("k4", tempFile));
        assertEquals("cos error", e.getMessage());
        verify(cosClient, times(1)).putObject(any());
    }
}


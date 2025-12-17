package com.mq.mqaiagent.advisor;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.Mockito;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisorChain;

class MyLoggerAdvisorTest {

    @Test
    void testNameAndOrder() {
        MyLoggerAdvisor advisor = new MyLoggerAdvisor();
        assertEquals("MyLoggerAdvisor", advisor.getName());
        assertEquals(0, advisor.getOrder());
    }

    @Test
    void testAroundCall_logsAndPasses() {
        MyLoggerAdvisor advisor = new MyLoggerAdvisor();
        AdvisedRequest request = mock(AdvisedRequest.class);
        when(request.userText()).thenReturn("hello");
        CallAroundAdvisorChain chain = mock(CallAroundAdvisorChain.class);
        AdvisedResponse response = mock(AdvisedResponse.class, Mockito.RETURNS_DEEP_STUBS);
        when(response.response().getResult().getOutput().getText()).thenReturn("world");
        when(chain.nextAroundCall(any())).thenReturn(response);

        AdvisedResponse actual = advisor.aroundCall(request, chain);
        assertSame(response, actual);
        ArgumentCaptor<AdvisedRequest> captor = ArgumentCaptor.forClass(AdvisedRequest.class);
        verify(chain).nextAroundCall(captor.capture());
        assertEquals("hello", captor.getValue().userText());
    }

    @Test
    void testAroundStream_logsAndAggregates() {
        MyLoggerAdvisor advisor = new MyLoggerAdvisor();
        AdvisedRequest request = mock(AdvisedRequest.class);
        when(request.userText()).thenReturn("hello");
        StreamAroundAdvisorChain chain = mock(StreamAroundAdvisorChain.class);
        AdvisedResponse response = mock(AdvisedResponse.class, Mockito.RETURNS_DEEP_STUBS);
        when(response.response().getResult().getOutput().getText()).thenReturn("world");
        when(chain.nextAroundStream(any())).thenReturn(Flux.just(response));

        Flux<AdvisedResponse> flux = advisor.aroundStream(request, chain);
        assertNotNull(flux);
        assertEquals(1, flux.collectList().block().size());
    }
}


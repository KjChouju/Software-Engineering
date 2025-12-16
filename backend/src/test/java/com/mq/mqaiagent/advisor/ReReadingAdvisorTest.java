package com.mq.mqaiagent.advisor;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisorChain;

class ReReadingAdvisorTest {

    @Test
    void testNameAndOrder() {
        ReReadingAdvisor advisor = new ReReadingAdvisor();
        assertEquals("ReReadingAdvisor", advisor.getName());
        assertEquals(0, advisor.getOrder());
    }

    @Test
    void testAroundCall_injectsParamsAndText() {
        ReReadingAdvisor advisor = new ReReadingAdvisor();
        AdvisedRequest request = mock(AdvisedRequest.class);
        when(request.userText()).thenReturn("Q?");
        when(request.userParams()).thenReturn(Map.of("a", 1));

        CallAroundAdvisorChain chain = mock(CallAroundAdvisorChain.class);
        AdvisedResponse response = mock(AdvisedResponse.class);
        when(chain.nextAroundCall(any())).thenReturn(response);

        AdvisedResponse actual = advisor.aroundCall(request, chain);
        assertSame(response, actual);

        ArgumentCaptor<AdvisedRequest> captor = ArgumentCaptor.forClass(AdvisedRequest.class);
        verify(chain).nextAroundCall(captor.capture());
        AdvisedRequest passed = captor.getValue();
        assertTrue(passed.userText().contains("Read the question again"));
        assertEquals("Q?", passed.userParams().get("re2_input_query"));
    }

    @Test
    void testAroundStream_injectsParamsAndText() {
        ReReadingAdvisor advisor = new ReReadingAdvisor();
        AdvisedRequest request = mock(AdvisedRequest.class);
        when(request.userText()).thenReturn("Hello");
        when(request.userParams()).thenReturn(Map.of());

        StreamAroundAdvisorChain chain = mock(StreamAroundAdvisorChain.class);
        AdvisedResponse response = mock(AdvisedResponse.class);
        when(chain.nextAroundStream(any())).thenReturn(Flux.just(response));

        Flux<AdvisedResponse> flux = advisor.aroundStream(request, chain);
        assertEquals(1, flux.collectList().block().size());

        ArgumentCaptor<AdvisedRequest> captor = ArgumentCaptor.forClass(AdvisedRequest.class);
        verify(chain).nextAroundStream(captor.capture());
        AdvisedRequest passed = captor.getValue();
        assertTrue(passed.userText().contains("{re2_input_query}"));
        assertEquals("Hello", passed.userParams().get("re2_input_query"));
    }
}


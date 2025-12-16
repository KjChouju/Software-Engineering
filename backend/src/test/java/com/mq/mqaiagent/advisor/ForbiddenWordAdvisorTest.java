package com.mq.mqaiagent.advisor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisorChain;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForbiddenWordAdvisorTest {

    @Test
    void testGetNameAndOrder() {
        ForbiddenWordAdvisor advisor = new ForbiddenWordAdvisor();
        assertEquals("ForbiddenWordAdvisor", advisor.getName());
        assertEquals(-100, advisor.getOrder());
    }

    @Test
    void testAroundCall_withoutProhibitedWord_callsChain() {
        ForbiddenWordAdvisor advisor = new ForbiddenWordAdvisor();
        AdvisedRequest request = mock(AdvisedRequest.class);
        when(request.userText()).thenReturn("hello world");
        CallAroundAdvisorChain chain = mock(CallAroundAdvisorChain.class);
        AdvisedResponse response = mock(AdvisedResponse.class);
        when(chain.nextAroundCall(any())).thenReturn(response);

        AdvisedResponse actual = advisor.aroundCall(request, chain);

        assertSame(response, actual);
        verify(chain, times(1)).nextAroundCall(request);
    }

    @Test
    void testAroundCall_withProhibitedWord_throws() {
        ForbiddenWordAdvisor advisor = new ForbiddenWordAdvisor();
        AdvisedRequest request = mock(AdvisedRequest.class);
        when(request.userText()).thenReturn("this contains badword text");
        CallAroundAdvisorChain chain = mock(CallAroundAdvisorChain.class);

        assertThrows(ForbiddenWordAdvisor.ProhibitedWordException.class,
                () -> advisor.aroundCall(request, chain));
        verify(chain, never()).nextAroundCall(any());
    }

    @Test
    void testAroundStream_withoutProhibitedWord_callsChain() {
        ForbiddenWordAdvisor advisor = new ForbiddenWordAdvisor();
        AdvisedRequest request = mock(AdvisedRequest.class);
        when(request.userText()).thenReturn("safe content");
        StreamAroundAdvisorChain chain = mock(StreamAroundAdvisorChain.class);
        AdvisedResponse response = mock(AdvisedResponse.class);
        when(chain.nextAroundStream(any())).thenReturn(Flux.just(response));

        Flux<AdvisedResponse> flux = advisor.aroundStream(request, chain);
        assertNotNull(flux);
        verify(chain, times(1)).nextAroundStream(request);
    }

    @Test
    void testAroundCall_emptyText_passes() {
        ForbiddenWordAdvisor advisor = new ForbiddenWordAdvisor();
        AdvisedRequest request = mock(AdvisedRequest.class);
        when(request.userText()).thenReturn("");
        CallAroundAdvisorChain chain = mock(CallAroundAdvisorChain.class);
        AdvisedResponse response = mock(AdvisedResponse.class);
        when(chain.nextAroundCall(any())).thenReturn(response);

        AdvisedResponse actual = advisor.aroundCall(request, chain);
        assertSame(response, actual);
    }

    @Test
    void testConstructor_withMissingFile_loadsEmptyList() {
        ForbiddenWordAdvisor advisor = new ForbiddenWordAdvisor("tmp/not-exist.txt");
        AdvisedRequest request = mock(AdvisedRequest.class);
        when(request.userText()).thenReturn("contains badword but list empty");
        CallAroundAdvisorChain chain = mock(CallAroundAdvisorChain.class);
        AdvisedResponse response = mock(AdvisedResponse.class);
        when(chain.nextAroundCall(any())).thenReturn(response);

        AdvisedResponse actual = advisor.aroundCall(request, chain);
        assertSame(response, actual);
    }
}


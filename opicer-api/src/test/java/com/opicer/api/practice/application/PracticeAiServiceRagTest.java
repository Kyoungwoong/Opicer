package com.opicer.api.practice.application;

import com.opicer.api.ai.application.AiTranscriptionService;
import com.opicer.api.config.AiProperties;
import com.opicer.api.goodanswer.application.GoodAnswerSampleQueryService;
import com.opicer.api.goodanswer.application.ReferenceSample;
import com.opicer.api.prompt.application.PromptVersionQueryService;
import com.opicer.api.shared.domain.OpicLevel;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PracticeAiServiceRagTest {

	private final GoodAnswerSampleQueryService queryService = mock(GoodAnswerSampleQueryService.class);
	private final PracticeAiService service = new PracticeAiService(
		new AiProperties(), mock(PromptVersionQueryService.class), queryService, mock(AiTranscriptionService.class));
	private final UUID topicId = UUID.randomUUID();

	@Test
	void retrievalFailuresDegradeToNoSamples() {
		for (ErrorCode code : List.of(ErrorCode.AI_NOT_CONFIGURED, ErrorCode.AI_EMBEDDING_FAILED, ErrorCode.AI_RAG_FAILED)) {
			doThrow(new ApiException(code)).when(queryService).findReferenceSamples(topicId, "answer");

			assertThat(service.referenceSamplesOrEmpty(topicId, "answer")).isEmpty();
		}
	}

	@Test
	void passesSamplesThroughWhenRetrievalSucceeds() {
		ReferenceSample sample = new ReferenceSample(UUID.randomUUID(), OpicLevel.IH, null, List.of(), "text", 0.8);
		when(queryService.findReferenceSamples(topicId, "answer")).thenReturn(List.of(sample));

		assertThat(service.referenceSamplesOrEmpty(topicId, "answer")).containsExactly(sample);
	}
}

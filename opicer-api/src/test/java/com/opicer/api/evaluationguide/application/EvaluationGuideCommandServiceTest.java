package com.opicer.api.evaluationguide.application;

import com.opicer.api.evaluation.application.EvaluationGuideNote;
import com.opicer.api.evaluation.domain.EvaluationGuideKind;
import com.opicer.api.evaluationguide.domain.EvaluationGuide;
import com.opicer.api.evaluationguide.infrastructure.EvaluationGuideRepository;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Versioning and the one-active-per-kind rule, on real Postgres (the database enforces it too). */
@SpringBootTest
class EvaluationGuideCommandServiceTest {

	@Autowired
	private EvaluationGuideCommandService commandService;

	@Autowired
	private EvaluationGuideQueryService queryService;

	@Autowired
	private EvaluationGuideRepository repository;

	@BeforeEach
	void setUp() {
		repository.deleteAll();
	}

	@Test
	void createNumbersVersionsPerKind() {
		EvaluationGuide first = commandService.create(EvaluationGuideKind.TIPS, " 팁 ", "- 첫 버전");
		EvaluationGuide second = commandService.create(EvaluationGuideKind.TIPS, "팁", "- 둘째 버전");
		EvaluationGuide other = commandService.create(EvaluationGuideKind.REPORT, "문체", "- 존댓말");

		assertThat(first.getVersion()).isEqualTo(1);
		assertThat(first.getTitle()).isEqualTo("팁");
		assertThat(first.isActive()).isFalse();
		assertThat(second.getVersion()).isEqualTo(2);
		assertThat(other.getVersion()).isEqualTo(1);
	}

	@Test
	void activateSwitchesOnlyTheSameKind() {
		UUID tipsV1 = commandService.create(EvaluationGuideKind.TIPS, "팁", "v1").getId();
		UUID tipsV2 = commandService.create(EvaluationGuideKind.TIPS, "팁", "v2").getId();
		UUID report = commandService.create(EvaluationGuideKind.REPORT, "문체", "r1").getId();
		commandService.activate(tipsV1);
		commandService.activate(report);

		commandService.activate(tipsV2);

		assertThat(repository.findById(tipsV1).orElseThrow().isActive()).isFalse();
		assertThat(repository.findById(tipsV2).orElseThrow().isActive()).isTrue();
		assertThat(repository.findById(report).orElseThrow().isActive()).isTrue();
	}

	@Test
	void activatingTheActiveVersionAgainKeepsItActive() {
		UUID id = commandService.create(EvaluationGuideKind.TIPS, "팁", "v1").getId();
		commandService.activate(id);

		commandService.activate(id);

		assertThat(repository.findById(id).orElseThrow().isActive()).isTrue();
	}

	@Test
	void activeNotesComeOneperKindInKindOrder() {
		commandService.activate(commandService.create(EvaluationGuideKind.REPORT, "문체", "report text").getId());
		commandService.activate(commandService.create(EvaluationGuideKind.SCORING_NOTES, "채점", "scoring text").getId());
		commandService.create(EvaluationGuideKind.TIPS, "팁", "inactive");

		assertThat(queryService.activeNotes()).containsExactly(
			new EvaluationGuideNote(EvaluationGuideKind.SCORING_NOTES, 1, "scoring text"),
			new EvaluationGuideNote(EvaluationGuideKind.REPORT, 1, "report text"));
	}

	@Test
	void deactivateStopsUsingTheKind() {
		UUID id = commandService.create(EvaluationGuideKind.TIPS, "팁", "v1").getId();
		commandService.activate(id);

		commandService.deactivate(id);

		assertThat(queryService.activeNotes()).isEmpty();
	}

	@Test
	void activeVersionIsFrozen() {
		UUID id = commandService.create(EvaluationGuideKind.TIPS, "팁", "v1").getId();
		commandService.activate(id);

		assertThatThrownBy(() -> commandService.update(id, "팁", "changed"))
			.isInstanceOf(ApiException.class)
			.extracting(e -> ((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.EVALUATION_GUIDE_ACTIVE);
		assertThatThrownBy(() -> commandService.delete(id))
			.isInstanceOf(ApiException.class)
			.extracting(e -> ((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.EVALUATION_GUIDE_ACTIVE);
		assertThat(repository.findById(id).orElseThrow().getContentMd()).isEqualTo("v1");
	}

	@Test
	void inactiveVersionCanBeEditedAndDeleted() {
		UUID id = commandService.create(EvaluationGuideKind.TIPS, "팁", "v1").getId();

		commandService.update(id, "새 제목", "changed");
		assertThat(repository.findById(id).orElseThrow().getContentMd()).isEqualTo("changed");

		commandService.delete(id);
		assertThat(repository.findById(id)).isEmpty();
	}

	@Test
	void contentOverTheLimitIsRejected() {
		String tooLong = "a".repeat(EvaluationGuide.MAX_CONTENT_LENGTH + 1);

		assertThatThrownBy(() -> commandService.create(EvaluationGuideKind.TIPS, "팁", tooLong))
			.isInstanceOf(ApiException.class)
			.extracting(e -> ((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
		assertThat(repository.count()).isZero();
	}

	@Test
	void unknownGuideIsNotFound() {
		assertThatThrownBy(() -> commandService.activate(UUID.randomUUID()))
			.isInstanceOf(ApiException.class)
			.extracting(e -> ((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.EVALUATION_GUIDE_NOT_FOUND);
	}

	@Test
	void databaseRejectsASecondActiveVersionOfOneKind() {
		EvaluationGuide first = new EvaluationGuide(EvaluationGuideKind.TIPS, 1, "팁", "v1");
		first.activate();
		repository.saveAndFlush(first);
		EvaluationGuide second = new EvaluationGuide(EvaluationGuideKind.TIPS, 2, "팁", "v2");
		second.activate();

		assertThatThrownBy(() -> repository.saveAndFlush(second)).isInstanceOf(DataIntegrityViolationException.class);
	}
}

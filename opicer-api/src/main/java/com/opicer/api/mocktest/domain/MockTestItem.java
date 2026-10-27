package com.opicer.api.mocktest.domain;

import com.opicer.api.practice.domain.AnswerEvaluationStatus;
import com.opicer.api.shared.domain.OpicLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One question of a {@link MockTest} and the learner's answer. The question is copied in (text, type, topic) so
 * history stays readable if the bank changes. The transcript is STT output; audio is never stored.
 */
@Entity
@Table(
	name = "mock_test_item",
	uniqueConstraints = @UniqueConstraint(name = "uk_mock_test_item_order", columnNames = {"mock_test_id", "item_order"}),
	indexes = @Index(name = "idx_mock_test_item_test", columnList = "mock_test_id")
)
public class MockTestItem {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "mock_test_id", nullable = false)
	private UUID mockTestId;

	/** 1–15, the order the questions were asked in. */
	@Column(name = "item_order", nullable = false)
	private int itemOrder;

	/** 1 before the difficulty re-selection, 2 after it. */
	@Column(nullable = false)
	private int part;

	@Column(name = "question_id", nullable = false)
	private UUID questionId;

	@Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
	private String questionText;

	@Column(name = "question_type", length = 32)
	private String questionType;

	/** Uploaded question audio, if any; otherwise the screen reads the text aloud. */
	@Column(name = "prompt_audio_url", length = 1000)
	private String promptAudioUrl;

	@Column(name = "topic_title")
	private String topicTitle;

	/** The practice topic, when there is one; reference samples are looked up within it. */
	@Column(name = "topic_id")
	private UUID topicId;

	@Column(columnDefinition = "TEXT")
	private String transcript;

	@Column(name = "duration_seconds")
	private Integer durationSeconds;

	@Enumerated(EnumType.STRING)
	@Column(name = "evaluation_status", length = 16)
	private AnswerEvaluationStatus evaluationStatus;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private String evaluation;

	@Enumerated(EnumType.STRING)
	@Column(name = "overall_level", length = 2)
	private OpicLevel overallLevel;

	protected MockTestItem() {
	}

	public MockTestItem(UUID mockTestId, int itemOrder, int part, UUID questionId, String questionText,
		String questionType, String promptAudioUrl, String topicTitle, UUID topicId) {
		this.mockTestId = mockTestId;
		this.itemOrder = itemOrder;
		this.part = part;
		this.questionId = questionId;
		this.questionText = questionText;
		this.questionType = questionType;
		this.promptAudioUrl = promptAudioUrl;
		this.topicTitle = topicTitle;
		this.topicId = topicId;
	}

	/** Records the answer on submit; an unanswered question keeps a null transcript and is skipped later. */
	public void answer(String transcript, Integer durationSeconds) {
		this.transcript = transcript;
		this.durationSeconds = durationSeconds;
		this.evaluationStatus = AnswerEvaluationStatus.PENDING;
	}

	public void markEvaluationPending() {
		this.evaluationStatus = AnswerEvaluationStatus.PENDING;
	}

	public void recordEvaluation(String evaluationJson, OpicLevel overallLevel) {
		this.evaluationStatus = AnswerEvaluationStatus.DONE;
		this.evaluation = evaluationJson;
		this.overallLevel = overallLevel;
	}

	public void markEvaluationFailed() {
		this.evaluationStatus = AnswerEvaluationStatus.FAILED;
	}

	public void skipEvaluation() {
		this.evaluationStatus = AnswerEvaluationStatus.SKIPPED;
	}

	public UUID getId() { return id; }
	public UUID getMockTestId() { return mockTestId; }
	public int getItemOrder() { return itemOrder; }
	public int getPart() { return part; }
	public UUID getQuestionId() { return questionId; }
	public String getQuestionText() { return questionText; }
	public String getQuestionType() { return questionType; }
	public String getPromptAudioUrl() { return promptAudioUrl; }
	public String getTopicTitle() { return topicTitle; }
	public UUID getTopicId() { return topicId; }
	public String getTranscript() { return transcript; }
	public Integer getDurationSeconds() { return durationSeconds; }
	public AnswerEvaluationStatus getEvaluationStatus() { return evaluationStatus; }
	public String getEvaluation() { return evaluation; }
	public OpicLevel getOverallLevel() { return overallLevel; }
}

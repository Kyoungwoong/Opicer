package com.opicer.api.practice.domain;

import jakarta.persistence.Column;
import com.opicer.api.shared.domain.OpicLevel;
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
 * One answered question in a {@link PracticeSession}.
 * The transcript is STT output (an estimate, not ground truth); audio is never stored.
 */
@Entity
@Table(
	name = "practice_answer",
	uniqueConstraints = @UniqueConstraint(name = "uk_practice_answer_session_question", columnNames = {"session_id", "question_id"}),
	indexes = @Index(name = "idx_practice_answer_session", columnList = "session_id")
)
public class PracticeAnswer {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "session_id", nullable = false)
	private UUID sessionId;

	@Column(name = "question_id", nullable = false)
	private UUID questionId;

	@Column(name = "question_order", nullable = false)
	private int questionOrder;

	@Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
	private String questionText;

	@Column(columnDefinition = "TEXT")
	private String transcript;

	@Column(columnDefinition = "TEXT")
	private String analysis;

	@Column(name = "improved_script", columnDefinition = "TEXT")
	private String improvedScript;

	/** Recording length measured by the client; null when unknown. */
	@Column(name = "duration_seconds")
	private Integer durationSeconds;

	@Enumerated(EnumType.STRING)
	@Column(name = "evaluation_status", length = 16)
	private AnswerEvaluationStatus evaluationStatus;

	/** Structured evaluation as JSON (competency levels, corrections, target-level script, expressions). */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private String evaluation;

	@Enumerated(EnumType.STRING)
	@Column(name = "overall_level", length = 2)
	private OpicLevel overallLevel;

	protected PracticeAnswer() {
	}

	public PracticeAnswer(UUID sessionId, UUID questionId, int questionOrder, String questionText, String transcript) {
		this(sessionId, questionId, questionOrder, questionText, transcript, null);
	}

	public PracticeAnswer(UUID sessionId, UUID questionId, int questionOrder, String questionText, String transcript,
		Integer durationSeconds) {
		this.sessionId = sessionId;
		this.questionId = questionId;
		this.questionOrder = questionOrder;
		this.questionText = questionText;
		this.transcript = transcript;
		this.durationSeconds = durationSeconds;
	}

	public void markEvaluationPending() {
		this.evaluationStatus = AnswerEvaluationStatus.PENDING;
	}

	/**
	 * Stores a successful evaluation. {@code analysis}/{@code improvedScript} get a readable summary and the
	 * target-level script, so screens that predate structured evaluation keep working.
	 */
	public void recordEvaluation(String evaluationJson, OpicLevel overallLevel, String analysisSummary, String targetScript) {
		this.evaluationStatus = AnswerEvaluationStatus.DONE;
		this.evaluation = evaluationJson;
		this.overallLevel = overallLevel;
		this.analysis = analysisSummary;
		this.improvedScript = targetScript;
	}

	public void markEvaluationFailed() {
		this.evaluationStatus = AnswerEvaluationStatus.FAILED;
	}

	public void skipEvaluation() {
		this.evaluationStatus = AnswerEvaluationStatus.SKIPPED;
	}

	/** Null arguments leave the stored value unchanged. */
	public void recordAiResult(String analysis, String improvedScript) {
		if (analysis != null) {
			this.analysis = analysis;
		}
		if (improvedScript != null) {
			this.improvedScript = improvedScript;
		}
	}

	public UUID getId() { return id; }
	public UUID getSessionId() { return sessionId; }
	public UUID getQuestionId() { return questionId; }
	public int getQuestionOrder() { return questionOrder; }
	public String getQuestionText() { return questionText; }
	public String getTranscript() { return transcript; }
	public String getAnalysis() { return analysis; }
	public String getImprovedScript() { return improvedScript; }
	public Integer getDurationSeconds() { return durationSeconds; }
	public AnswerEvaluationStatus getEvaluationStatus() { return evaluationStatus; }
	public String getEvaluation() { return evaluation; }
	public OpicLevel getOverallLevel() { return overallLevel; }
}

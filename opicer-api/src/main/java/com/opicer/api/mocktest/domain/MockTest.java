package com.opicer.api.mocktest.domain;

import com.opicer.api.practice.domain.SessionEvaluationStatus;
import com.opicer.api.shared.domain.OpicLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One mock test (실전연습), the aggregate root for its {@link MockTestItem}s. A practice imitation of the OPIc
 * procedure: every level it reports is an estimate, never an official score.
 */
@Entity
@Table(name = "mock_test", indexes = @Index(name = "idx_mock_test_user_submitted", columnList = "user_id, submitted_at"))
public class MockTest {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private MockTestStatus status;

	/** Background Survey selections as JSON: item ids per part code (see {@link SurveyCatalog}). */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb", nullable = false)
	private String survey;

	@Column(name = "self_assessment", nullable = false)
	private int selfAssessment;

	@Enumerated(EnumType.STRING)
	@Column(length = 8)
	private DifficultyAdjustment adjustment;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_level", nullable = false, length = 2)
	private OpicLevel targetLevel;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt;

	@Column(name = "submitted_at")
	private Instant submittedAt;

	/** Exam time the learner used, measured by the client. */
	@Column(name = "exam_seconds")
	private Integer examSeconds;

	@Enumerated(EnumType.STRING)
	@Column(name = "evaluation_status", length = 16)
	private SessionEvaluationStatus evaluationStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "estimated_level", length = 2)
	private OpicLevel estimatedLevel;

	/** Session report as JSON (evaluation.domain.SessionReport, plus counts and the disclaimer). */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private String report;

	/** Feedback beyond the report as JSON (question types, habits, time management). */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private String insights;

	@Column(name = "evaluated_at")
	private Instant evaluatedAt;

	protected MockTest() {
	}

	public MockTest(UUID userId, String surveyJson, int selfAssessment, OpicLevel targetLevel, Instant startedAt) {
		this.userId = userId;
		this.status = MockTestStatus.IN_PROGRESS;
		this.survey = surveyJson;
		this.selfAssessment = selfAssessment;
		this.targetLevel = targetLevel;
		this.startedAt = startedAt;
	}

	public boolean isAdjusted() {
		return adjustment != null;
	}

	/** Callers check {@link #isAdjusted()} and the status first. */
	public void adjust(DifficultyAdjustment adjustment) {
		this.adjustment = adjustment;
	}

	public void submit(Integer examSeconds, Instant now) {
		this.status = MockTestStatus.SUBMITTED;
		this.examSeconds = examSeconds;
		this.submittedAt = now;
		this.evaluationStatus = SessionEvaluationStatus.PENDING;
	}

	/** A learner retry: the evaluation runs again for the items that failed. */
	public void requestEvaluation() {
		this.evaluationStatus = SessionEvaluationStatus.PENDING;
	}

	public void startEvaluation() {
		this.evaluationStatus = SessionEvaluationStatus.RUNNING;
	}

	public void completeEvaluation(SessionEvaluationStatus status, OpicLevel estimatedLevel, String reportJson,
		String insightsJson, Instant now) {
		this.evaluationStatus = status;
		this.estimatedLevel = estimatedLevel;
		this.report = reportJson;
		this.insights = insightsJson;
		this.evaluatedAt = now;
	}

	public UUID getId() { return id; }
	public UUID getUserId() { return userId; }
	public MockTestStatus getStatus() { return status; }
	public String getSurvey() { return survey; }
	public int getSelfAssessment() { return selfAssessment; }
	public DifficultyAdjustment getAdjustment() { return adjustment; }
	public OpicLevel getTargetLevel() { return targetLevel; }
	public Instant getStartedAt() { return startedAt; }
	public Instant getSubmittedAt() { return submittedAt; }
	public Integer getExamSeconds() { return examSeconds; }
	public SessionEvaluationStatus getEvaluationStatus() { return evaluationStatus; }
	public OpicLevel getEstimatedLevel() { return estimatedLevel; }
	public String getReport() { return report; }
	public String getInsights() { return insights; }
	public Instant getEvaluatedAt() { return evaluatedAt; }
}

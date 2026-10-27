package com.opicer.api.aijob.infrastructure;

import com.opicer.api.aijob.application.AiJobCommandService;
import com.opicer.api.aijob.domain.AiJobType;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A database created before MOCK_TEST_EVALUATION existed still accepts mock test jobs after a start. */
@SpringBootTest
class AiJobTypeConstraintHardeningTest {

	@Autowired
	private AiJobTypeConstraintHardening hardening;

	@Autowired
	private AiJobCommandService jobs;

	@Autowired
	private AiJobRepository repository;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void widensAStaleTypeConstraint() throws Exception {
		repository.deleteAll();
		jdbc.execute("ALTER TABLE ai_job DROP CONSTRAINT IF EXISTS ai_job_type_check");
		jdbc.execute("ALTER TABLE ai_job ADD CONSTRAINT ai_job_type_check CHECK (type IN ('SESSION_EVALUATION'))");
		assertThatThrownBy(() -> jobs.enqueue(AiJobType.MOCK_TEST_EVALUATION, UUID.randomUUID()))
			.isInstanceOf(RuntimeException.class);

		hardening.run(new DefaultApplicationArguments());

		assertThat(jobs.enqueue(AiJobType.MOCK_TEST_EVALUATION, UUID.randomUUID())).isTrue();
		repository.deleteAll();
	}
}

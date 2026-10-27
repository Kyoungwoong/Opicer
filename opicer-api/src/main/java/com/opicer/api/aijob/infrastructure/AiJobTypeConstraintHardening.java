package com.opicer.api.aijob.infrastructure;

import com.opicer.api.aijob.domain.AiJobType;
import java.sql.Connection;
import java.util.Arrays;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Hibernate puts a CHECK constraint on enum columns when it creates a table, and {@code ddl-auto: update} never
 * widens it. Without this, a database created before a new {@link AiJobType} existed rejects jobs of that type.
 * Rebuilds {@code ai_job_type_check} from the current enum on every PostgreSQL start; a no-op elsewhere.
 */
@Component
public class AiJobTypeConstraintHardening implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AiJobTypeConstraintHardening.class);

	static final String CONSTRAINT = "ai_job_type_check";

	private final DataSource dataSource;
	private final JdbcTemplate jdbcTemplate;

	public AiJobTypeConstraintHardening(DataSource dataSource, JdbcTemplate jdbcTemplate) {
		this.dataSource = dataSource;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			if (!"PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName())) {
				return;
			}
		}
		// Enum names are code constants, so building the list into SQL is safe.
		String allowed = Arrays.stream(AiJobType.values())
			.map(type -> "'" + type.name() + "'")
			.collect(Collectors.joining(", "));
		jdbcTemplate.execute("""
			DO $$
			BEGIN
			  IF to_regclass('ai_job') IS NOT NULL THEN
			    ALTER TABLE ai_job DROP CONSTRAINT IF EXISTS %s;
			    ALTER TABLE ai_job ADD CONSTRAINT %s CHECK (type IN (%s));
			  END IF;
			END
			$$;
			""".formatted(CONSTRAINT, CONSTRAINT, allowed));
		log.info("[AiJob] type constraint allows {}", allowed);
	}
}

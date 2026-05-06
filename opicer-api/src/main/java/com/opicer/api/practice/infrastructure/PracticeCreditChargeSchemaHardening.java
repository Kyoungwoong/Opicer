package com.opicer.api.practice.infrastructure;

import java.sql.Connection;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PracticeCreditChargeSchemaHardening implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(PracticeCreditChargeSchemaHardening.class);

	private final DataSource dataSource;
	private final JdbcTemplate jdbcTemplate;

	public PracticeCreditChargeSchemaHardening(DataSource dataSource, JdbcTemplate jdbcTemplate) {
		this.dataSource = dataSource;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			String databaseProductName = connection.getMetaData().getDatabaseProductName();
			if (!"PostgreSQL".equalsIgnoreCase(databaseProductName)) {
				return;
			}
		}

		log.info("Ensuring practice_credit_charge table and unique constraint");
		jdbcTemplate.execute("""
			CREATE TABLE IF NOT EXISTS practice_credit_charge (
			  id UUID PRIMARY KEY,
			  user_id UUID NOT NULL,
			  topic_selection_id UUID NOT NULL,
			  amount INTEGER NOT NULL,
			  charged_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
			);
			""");
		jdbcTemplate.execute("""
			DO $$
			BEGIN
			  IF NOT EXISTS (
			    SELECT 1
			    FROM pg_constraint
			    WHERE conname = 'uk_practice_credit_charge_topic_selection'
			  ) THEN
			    ALTER TABLE practice_credit_charge
			      ADD CONSTRAINT uk_practice_credit_charge_topic_selection UNIQUE (topic_selection_id);
			  END IF;
			END
			$$;
			""");
	}
}

package com.opicer.api.credit.infrastructure;

import java.sql.Connection;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class CreditPaymentSchemaHardening implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(CreditPaymentSchemaHardening.class);

	private final DataSource dataSource;
	private final JdbcTemplate jdbcTemplate;

	public CreditPaymentSchemaHardening(DataSource dataSource, JdbcTemplate jdbcTemplate) {
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

		log.info("Ensuring unique constraint exists on credit_payments(order_id)");
		jdbcTemplate.execute("""
			DO $$
			BEGIN
			  IF NOT EXISTS (
			    SELECT 1
			    FROM pg_constraint
			    WHERE conname = 'uk_credit_payments_order'
			  ) THEN
			    ALTER TABLE credit_payments
			      ADD CONSTRAINT uk_credit_payments_order UNIQUE (order_id);
			  END IF;
			END
			$$;
			""");
	}
}

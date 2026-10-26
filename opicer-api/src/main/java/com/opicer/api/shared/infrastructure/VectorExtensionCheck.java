package com.opicer.api.shared.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Reports whether the pgvector extension is installed. It cannot create it: Hibernate's DDL for
 * {@code vector(1536)} columns runs before any runner, so the extension must already exist
 * ({@code CREATE EXTENSION IF NOT EXISTS vector;} once per database). See docs/adr/0001-vector-similarity.md.
 */
@Component
public class VectorExtensionCheck implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(VectorExtensionCheck.class);

	private final JdbcTemplate jdbcTemplate;

	public VectorExtensionCheck(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void run(ApplicationArguments args) {
		try {
			String version = jdbcTemplate.query(
				"select extversion from pg_extension where extname = 'vector'",
				rs -> rs.next() ? rs.getString(1) : null);
			if (version == null) {
				log.error("[pgvector] extension 'vector' is not installed; run CREATE EXTENSION IF NOT EXISTS vector;"
					+ " reference-sample retrieval will fail");
			} else {
				log.info("[pgvector] extension 'vector' {} installed", version);
			}
		} catch (RuntimeException e) {
			log.warn("[pgvector] could not check extension: {}", e.getMessage());
		}
	}
}

package com.opicer.api.goodanswer.application;

import com.opicer.api.shared.domain.OpicLevel;
import java.util.List;
import java.util.UUID;

/** A retrieved good-answer sample, detached from JPA so it can be used outside a transaction. */
public record ReferenceSample(
	UUID id,
	OpicLevel level,
	String summary,
	List<String> keyExpressions,
	String sampleText,
	double similarity
) {}

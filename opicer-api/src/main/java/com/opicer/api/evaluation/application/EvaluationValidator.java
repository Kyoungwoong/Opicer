package com.opicer.api.evaluation.application;

import com.opicer.api.evaluation.application.EvaluationOutput.CorrectionItem;
import com.opicer.api.evaluation.application.EvaluationOutput.DimensionRating;
import com.opicer.api.evaluation.application.EvaluationOutput.KeyExpression;
import com.opicer.api.evaluation.domain.CompetencyDimension;
import com.opicer.api.evaluation.domain.EvaluationMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Server-side checks on model output. The schema guarantees shape; this guarantees meaning: every competency
 * rated exactly once, quoted spans really occur in the transcript, and counts/lengths stay in bounds.
 * Fixable issues are cleaned; structural ones raise a retryable {@link EvaluationFailure}.
 */
public final class EvaluationValidator {

	static final int MAX_CORRECTIONS = 8;
	static final int MAX_STRENGTHS = 2;
	static final int MAX_KEY_EXPRESSIONS = 3;
	static final int MAX_EXPRESSION_WORDS = 8;

	private EvaluationValidator() {
	}

	public static EvaluationOutput validate(EvaluationOutput output, String transcript) {
		return validate(output, transcript, EvaluationMode.FULL);
	}

	/** COACH output carries no ratings or strengths; anything the model sent there anyway is dropped. */
	public static EvaluationOutput validate(EvaluationOutput output, String transcript, EvaluationMode mode) {
		if (output == null) {
			throw EvaluationFailure.retryable("INVALID_OUTPUT", "empty evaluation");
		}
		String haystack = normalize(transcript);
		boolean full = mode == EvaluationMode.FULL;
		return new EvaluationOutput(
			full ? dimensions(output.dimensions(), haystack) : List.of(),
			full ? strengths(output.strengths()) : List.of(),
			corrections(output.corrections(), haystack),
			targetScript(output.targetScript(), transcript),
			keyExpressions(output.keyExpressions())
		);
	}

	private static List<DimensionRating> dimensions(List<DimensionRating> ratings, String haystack) {
		Map<CompetencyDimension, DimensionRating> byCode = new EnumMap<>(CompetencyDimension.class);
		for (DimensionRating rating : ratings == null ? List.<DimensionRating>of() : ratings) {
			if (rating == null || rating.code() == null || rating.level() == null) {
				throw EvaluationFailure.retryable("INVALID_OUTPUT", "competency rating without code or level");
			}
			if (byCode.put(rating.code(), rating) != null) {
				throw EvaluationFailure.retryable("INVALID_OUTPUT", "competency rated twice: " + rating.code());
			}
		}
		if (byCode.size() != CompetencyDimension.values().length) {
			throw EvaluationFailure.retryable("INVALID_OUTPUT", "expected every competency, got " + byCode.keySet());
		}
		List<DimensionRating> result = new ArrayList<>();
		for (DimensionRating rating : byCode.values()) {
			// An invented quote is worse than none.
			String evidence = occursIn(rating.evidence(), haystack) ? rating.evidence().trim() : "";
			result.add(new DimensionRating(rating.code(), rating.level(), evidence, blankToEmpty(rating.commentKo())));
		}
		return List.copyOf(result);
	}

	private static List<CorrectionItem> corrections(List<CorrectionItem> items, String haystack) {
		List<CorrectionItem> kept = new ArrayList<>();
		Set<String> seen = new LinkedHashSet<>();
		for (CorrectionItem item : items == null ? List.<CorrectionItem>of() : items) {
			if (kept.size() == MAX_CORRECTIONS) {
				break;
			}
			if (item == null || item.category() == null || isBlank(item.corrected())
				|| !occursIn(item.original(), haystack)
				|| normalize(item.original()).equals(normalize(item.corrected()))
				|| !seen.add(normalize(item.original()))) {
				continue;
			}
			kept.add(new CorrectionItem(item.original().trim(), item.corrected().trim(),
				blankToEmpty(item.reasonKo()), item.category()));
		}
		return List.copyOf(kept);
	}

	private static List<String> strengths(List<String> strengths) {
		return (strengths == null ? List.<String>of() : strengths).stream()
			.filter(s -> !isBlank(s))
			.map(String::trim)
			.limit(MAX_STRENGTHS)
			.toList();
	}

	private static List<KeyExpression> keyExpressions(List<KeyExpression> expressions) {
		return (expressions == null ? List.<KeyExpression>of() : expressions).stream()
			.filter(e -> e != null && !isBlank(e.expression())
				&& e.expression().trim().split("\\s+").length <= MAX_EXPRESSION_WORDS)
			.map(e -> new KeyExpression(e.expression().trim(), blankToEmpty(e.meaningKo())))
			.limit(MAX_KEY_EXPRESSIONS)
			.toList();
	}

	/** Must exist and stay within 2.5× the answer plus room to develop a short answer. */
	private static String targetScript(String script, String transcript) {
		if (isBlank(script)) {
			throw EvaluationFailure.retryable("INVALID_OUTPUT", "empty target script");
		}
		int limit = (int) (wordCount(transcript) * 2.5) + 150;
		if (wordCount(script) > limit) {
			throw EvaluationFailure.retryable("INVALID_OUTPUT", "target script longer than " + limit + " words");
		}
		return script.trim();
	}

	private static boolean occursIn(String span, String haystack) {
		return !isBlank(span) && haystack.contains(normalize(span));
	}

	/** Lowercase, collapse whitespace and ignore curly-quote differences, so quoting tolerates formatting. */
	static String normalize(String text) {
		if (text == null) {
			return "";
		}
		return text.toLowerCase(Locale.ROOT)
			.replace('’', '\'').replace('‘', '\'').replace('“', '"').replace('”', '"')
			.replaceAll("\\s+", " ")
			.trim();
	}

	private static int wordCount(String text) {
		return isBlank(text) ? 0 : text.trim().split("\\s+").length;
	}

	private static boolean isBlank(String text) {
		return text == null || text.isBlank();
	}

	private static String blankToEmpty(String text) {
		return text == null ? "" : text.trim();
	}
}

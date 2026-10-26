package com.opicer.api.opic.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Parses opic.or.kr's JSON responses (served as text/html) into plain rows.
 * Pure: no I/O, so it is unit-tested against saved fixtures.
 */
@Component
public class OpicSiteParser {

	static final String OPIC_SUBJECT = "OPI001";

	private static final DateTimeFormatter SITE_DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd");

	/** First letter of opic.or.kr's area code (e.g. "C024") → region label. */
	private static final Map<Character, String> REGION_BY_AREA = Map.ofEntries(
		Map.entry('A', "서울"), Map.entry('B', "인천"), Map.entry('C', "경기"), Map.entry('D', "강원"),
		Map.entry('E', "경북"), Map.entry('F', "경남"), Map.entry('G', "광주"), Map.entry('H', "대전"),
		Map.entry('I', "대구"), Map.entry('J', "울산"), Map.entry('K', "부산"), Map.entry('L', "전남"),
		Map.entry('M', "전북"), Map.entry('N', "제주"), Map.entry('O', "충청")
	);

	private final ObjectMapper objectMapper;

	public OpicSiteParser(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/** Only OPIc rounds (subject OPI001); WPT and other exams are skipped. */
	public List<ScheduleRow> parseSchedules(String body) {
		JsonNode list = read(body).path("openList");
		List<ScheduleRow> rows = new ArrayList<>();
		for (JsonNode node : list) {
			String subject = text(node, "d_subj");
			LocalDate examDate = date(text(node, "d_started"));
			String roundId = text(node, "d_grseq");
			if (!OPIC_SUBJECT.equals(subject) || examDate == null || roundId == null) {
				continue;
			}
			rows.add(new ScheduleRow(
				roundId + "-" + subject,
				examDate,
				date(text(node, "d_propstart")),
				date(text(node, "d_propend")),
				date(text(node, "d_openday")),
				orDefault(text(node, "d_grseqnm"), "정기 평가"),
				"Y".equalsIgnoreCase(text(node, "d_early_open_yn"))
			));
		}
		return rows;
	}

	public List<CenterRow> parseCenters(String body) {
		JsonNode root = read(body);
		List<CenterRow> rows = new ArrayList<>();
		for (JsonNode node : root) {
			String name = text(node, "bizname");
			String address = text(node, "addr_doro");
			if (name == null || address == null) {
				continue;
			}
			String areaCode = text(node, "areacd");
			String directions = text(node, "simple_way");
			rows.add(new CenterRow(
				name + "|" + address,
				name,
				address,
				number(text(node, "maplatitude")),
				number(text(node, "maplongitude")),
				text(node, "tel"),
				subwayLabels(text(node, "subway_stat")),
				directions != null ? directions : stripHtml(text(node, "map")),
				areaCode,
				areaCode == null ? "기타" : REGION_BY_AREA.getOrDefault(areaCode.charAt(0), "기타")
			));
		}
		return rows;
	}

	/** "line01@1,line11@경의중앙" → "1호선, 경의중앙". */
	static String subwayLabels(String raw) {
		if (raw == null) {
			return null;
		}
		List<String> labels = new ArrayList<>();
		for (String token : raw.split(",")) {
			int at = token.indexOf('@');
			String label = (at >= 0 ? token.substring(at + 1) : token).trim();
			if (label.isEmpty()) {
				continue;
			}
			String shown = label.chars().allMatch(Character::isDigit) ? label + "호선" : label;
			if (!labels.contains(shown)) {
				labels.add(shown);
			}
		}
		return labels.isEmpty() ? null : String.join(", ", labels);
	}

	private JsonNode read(String body) {
		try {
			return objectMapper.readTree(body == null ? "" : body);
		} catch (IOException e) {
			throw new IllegalArgumentException("Unexpected opic.or.kr response", e);
		}
	}

	private static String text(JsonNode node, String field) {
		JsonNode value = node.get(field);
		if (value == null || value.isNull()) {
			return null;
		}
		String text = value.asText().trim();
		return text.isEmpty() ? null : text;
	}

	/** "2026.11.06(금)" → 2026-11-06. */
	private static LocalDate date(String value) {
		if (value == null || value.length() < 10) {
			return null;
		}
		try {
			return LocalDate.parse(value.substring(0, 10), SITE_DATE);
		} catch (RuntimeException e) {
			return null;
		}
	}

	private static Double number(String value) {
		if (value == null) {
			return null;
		}
		try {
			return Double.valueOf(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String stripHtml(String value) {
		if (value == null) {
			return null;
		}
		String text = value.replaceAll("(?i)<br\\s*/?>", "\n").replaceAll("<[^>]+>", "").trim();
		return text.isEmpty() ? null : text;
	}

	private static String orDefault(String value, String fallback) {
		return value == null ? fallback : value;
	}

	public record ScheduleRow(
		String externalKey,
		LocalDate examDate,
		LocalDate registrationStart,
		LocalDate registrationEnd,
		LocalDate scoreReleaseDate,
		String roundName,
		boolean earlyRelease
	) {}

	public record CenterRow(
		String externalKey,
		String name,
		String address,
		Double latitude,
		Double longitude,
		String phone,
		String subwayLines,
		String directions,
		String regionCode,
		String regionName
	) {}
}

package com.opicer.api.opic.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Fixtures are trimmed copies of real opic.or.kr responses (Nov 2026 schedule, 3 centers). */
class OpicSiteParserTest {

	private final OpicSiteParser parser = new OpicSiteParser(new ObjectMapper());

	@Test
	void parseSchedulesKeepsOnlyOpicRoundsWithParsedDates() throws IOException {
		List<OpicSiteParser.ScheduleRow> rows = parser.parseSchedules(fixture("schedule-202611.json"));

		assertThat(rows).hasSize(2); // the WPT001 round is skipped
		OpicSiteParser.ScheduleRow first = rows.get(0);
		assertThat(first.externalKey()).isEqualTo("6553-OPI001");
		assertThat(first.examDate()).isEqualTo(LocalDate.of(2026, 11, 1));
		assertThat(first.registrationStart()).isEqualTo(LocalDate.of(2026, 9, 30));
		assertThat(first.registrationEnd()).isEqualTo(LocalDate.of(2026, 10, 30));
		assertThat(first.scoreReleaseDate()).isEqualTo(LocalDate.of(2026, 11, 6));
		assertThat(first.roundName()).isEqualTo("261101 정기 평가");
		assertThat(first.earlyRelease()).isFalse();
		assertThat(rows.get(1).examDate()).isEqualTo(LocalDate.of(2026, 11, 3));
	}

	@Test
	void parseCentersMapsRegionSubwayAndDirections() throws IOException {
		List<OpicSiteParser.CenterRow> rows = parser.parseCenters(fixture("centers.json"));

		assertThat(rows).hasSize(3);
		OpicSiteParser.CenterRow cityHall = rows.get(0);
		assertThat(cityHall.name()).isEqualTo("OPIc시청센터");
		assertThat(cityHall.externalKey()).isEqualTo(cityHall.name() + "|" + cityHall.address());
		assertThat(cityHall.regionName()).isEqualTo("서울");
		assertThat(cityHall.subwayLines()).isEqualTo("1호선, 2호선");
		assertThat(cityHall.directions()).isEqualTo("시청역 10번 출구에서 도보 4분");
		assertThat(cityHall.latitude()).isBetween(37.5, 37.6);
		assertThat(cityHall.phone()).isNull();

		OpicSiteParser.CenterRow suwon = rows.get(1);
		assertThat(suwon.regionName()).isEqualTo("경기");
		assertThat(suwon.subwayLines()).isNull();
		assertThat(suwon.directions()).isNotBlank().doesNotContain("<br");

		assertThat(rows.get(2).subwayLines()).isEqualTo("김포골드");
	}

	@Test
	void subwayLabelsDeduplicateAndNameNumberedLines() {
		assertThat(OpicSiteParser.subwayLabels("line11@경의중앙,line35@1,line01@1,")).isEqualTo("경의중앙, 1호선");
		assertThat(OpicSiteParser.subwayLabels("")).isNull();
	}

	@Test
	void malformedBodyIsRejected() {
		assertThatThrownBy(() -> parser.parseSchedules("<html>maintenance</html>"))
			.isInstanceOf(IllegalArgumentException.class);
	}

	private String fixture(String name) throws IOException {
		try (InputStream in = getClass().getResourceAsStream("/opic/" + name)) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}

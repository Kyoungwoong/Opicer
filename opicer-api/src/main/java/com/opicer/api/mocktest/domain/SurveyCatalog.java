package com.opicer.api.mocktest.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The Background Survey of the mock test (docs/opic/mock-test-spec.md §4): its parts, items and selection rules,
 * the single source of truth for both the API and the screen. Which practice topics an item points to is kept in
 * {@link SurveyTopicLink} and edited on the admin screen; the topics listed here only seed an empty link table.
 */
public final class SurveyCatalog {

	/** Part 4 (4-1 to 4-4) needs at least this many selections in total. */
	public static final int PART4_MIN_TOTAL = 12;

	/** The part whose choice decides the fallback combo topics (home and neighbourhood by default). */
	public static final String RESIDENCE = "RESIDENCE";

	public static final String HOME = "집/주거환경";
	public static final String NEIGHBORHOOD = "이웃/동네";

	public static final List<SurveyPart> PARTS = List.of(
		single("OCCUPATION", "Part 1. 현재 귀하는 어느 분야에 종사하고 계십니까?",
			item("occupation.business", "사업/회사"),
			item("occupation.home_office", "재택근무/재택사업"),
			item("occupation.teacher", "교사/교육자"),
			item("occupation.none", "일 경험 없음")),
		single("STUDENT", "Part 2. 현재 당신은 학생입니까?",
			item("student.yes", "예"),
			item("student.no", "아니오(수강 후 5년 이상 지남)")),
		single(RESIDENCE, "Part 3. 현재 귀하는 어디에 살고 계십니까?",
			item("residence.alone", "개인주택이나 아파트에 홀로 거주", HOME, NEIGHBORHOOD),
			item("residence.roommates", "친구나 룸메이트와 함께 주택이나 아파트에 거주", HOME, NEIGHBORHOOD),
			item("residence.family", "가족과 함께 주택이나 아파트에 거주", HOME, NEIGHBORHOOD),
			item("residence.dormitory", "학교 기숙사", HOME, NEIGHBORHOOD),
			item("residence.barracks", "군대 막사", HOME, NEIGHBORHOOD)),
		multiple("LEISURE", "4-1. 귀하는 여가 활동으로 주로 무엇을 하십니까? (2개 이상)", 2,
			item("leisure.movies", "영화 보기", "영화 감상"),
			item("leisure.club", "클럽/나이트클럽 가기"),
			item("leisure.performance", "공연 보기", "공연/콘서트 관람"),
			item("leisure.concert", "콘서트 보기", "공연/콘서트 관람"),
			item("leisure.museum", "박물관 가기"),
			item("leisure.park", "공원 가기", NEIGHBORHOOD),
			item("leisure.camping", "캠핑 하기", "캠핑"),
			item("leisure.beach", "해변 가기"),
			item("leisure.sports_watching", "스포츠 관람"),
			item("leisure.home_improvement", "주거 개선", HOME),
			item("leisure.cooking_shows", "요리 관련 프로그램 시청", "요리"),
			item("leisure.games", "게임 하기"),
			item("leisure.sns", "SNS 글 올리기"),
			item("leisure.bar", "술집/바에 가기"),
			item("leisure.texting", "친구들과 문자하기"),
			item("leisure.billiards", "당구치기"),
			item("leisure.volunteering", "자원 봉사"),
			item("leisure.driving", "차 드라이브 하기"),
			item("leisure.test_prep", "시험 대비 과정 수강"),
			item("leisure.news", "뉴스 보거나 듣기"),
			item("leisure.cafe", "카페/커피 전문점 가기"),
			item("leisure.chess", "체스"),
			item("leisure.tv", "TV 시청", "TV 시청"),
			item("leisure.shopping", "쇼핑하기", "쇼핑"),
			item("leisure.reality_shows", "리얼리티 쇼 보기", "TV 시청"),
			item("leisure.job_hunting", "구직 활동")),
		multiple("HOBBY", "4-2. 귀하의 취미나 관심사는 무엇입니까? (1개 이상)", 1,
			item("hobby.reading_to_children", "아이에게 책 읽어주기", "독서"),
			item("hobby.music", "음악 감상하기", "음악 감상"),
			item("hobby.instrument", "악기 연주하기", "음악 감상"),
			item("hobby.singing", "혼자 노래부르거나 합창하기", "음악 감상"),
			item("hobby.dancing", "춤추기"),
			item("hobby.writing", "글쓰기"),
			item("hobby.drawing", "그림 그리기"),
			item("hobby.travel_media", "여행 관련 잡지나 블로그 글 읽기"),
			item("hobby.photography", "사진 촬영하기"),
			item("hobby.stocks", "주식 투자"),
			item("hobby.newspaper", "신문 읽기"),
			item("hobby.reading", "독서", "독서"),
			item("hobby.cooking", "요리하기", "요리"),
			item("hobby.pets", "애완동물 키우기")),
		multiple("SPORT", "4-3. 귀하는 주로 어떤 운동을 즐기십니까? (1개 이상)", 1,
			item("sport.basketball", "농구"),
			item("sport.baseball", "야구/소프트볼"),
			item("sport.soccer", "축구"),
			item("sport.american_football", "미식축구"),
			item("sport.hockey", "하키"),
			item("sport.cricket", "크리켓"),
			item("sport.golf", "골프"),
			item("sport.volleyball", "배구"),
			item("sport.tennis", "테니스"),
			item("sport.badminton", "배드민턴"),
			item("sport.table_tennis", "탁구"),
			item("sport.taekwondo", "태권도"),
			item("sport.swimming", "수영", "수영"),
			item("sport.cycling", "자전거", "자전거 타기"),
			item("sport.skiing", "스키/스노우보드"),
			item("sport.ice_skating", "아이스 스케이트"),
			item("sport.jogging", "조깅", "조깅/걷기"),
			item("sport.walking", "걷기", "조깅/걷기"),
			item("sport.yoga", "요가", "헬스/운동"),
			item("sport.hiking", "하이킹/트레킹"),
			item("sport.fishing", "낚시"),
			item("sport.gym", "헬스", "헬스/운동"),
			item("sport.classes", "운동 수업 수강하기", "헬스/운동"),
			item("sport.none", "운동을 전혀 하지 않음")),
		multiple("TRAVEL", "4-4. 귀하는 어떤 휴가나 출장을 다녀온 경험이 있습니까? (1개 이상)", 1,
			item("travel.domestic_business", "국내 출장", "출장"),
			item("travel.overseas_business", "해외 출장", "출장"),
			item("travel.staycation", "집에서 보내는 휴가", HOME),
			item("travel.domestic", "국내 여행", "국내 여행"),
			item("travel.overseas", "해외 여행", "해외 여행"))
	);

	private static final Set<String> PART4 = Set.of("LEISURE", "HOBBY", "SPORT", "TRAVEL");
	private static final Map<String, SurveyItem> ITEMS = index();

	private SurveyCatalog() {
	}

	/**
	 * @param selections item ids per part code
	 * @return the problems in Korean, empty when the survey is complete
	 */
	public static List<String> problems(Map<String, List<String>> selections) {
		List<String> problems = new ArrayList<>();
		Map<String, List<String>> given = selections == null ? Map.of() : selections;
		int part4Total = 0;
		for (SurveyPart part : PARTS) {
			List<String> chosen = chosen(given, part.code());
			Set<String> unique = new HashSet<>(chosen);
			if (unique.size() != chosen.size()) {
				problems.add(part.title() + ": 같은 항목을 두 번 골랐어요.");
			}
			for (String id : unique) {
				if (!part.hasItem(id)) {
					problems.add(part.title() + ": 알 수 없는 항목이에요 (" + id + ").");
				}
			}
			if (unique.size() < part.min() || unique.size() > part.max()) {
				problems.add(part.max() == 1
					? part.title() + ": 하나를 골라 주세요."
					: part.title() + ": " + part.min() + "개 이상 골라 주세요.");
			}
			if (PART4.contains(part.code())) {
				part4Total += unique.size();
			}
		}
		for (String code : given.keySet()) {
			if (PARTS.stream().noneMatch(p -> p.code().equals(code))) {
				problems.add("알 수 없는 설문 항목이에요 (" + code + ").");
			}
		}
		if (part4Total < PART4_MIN_TOTAL) {
			problems.add("Part 4에서 모두 " + PART4_MIN_TOTAL + "개 이상 골라 주세요. 지금 " + part4Total + "개예요.");
		}
		return problems;
	}

	public static Optional<SurveyItem> findItem(String id) {
		return Optional.ofNullable(ITEMS.get(id));
	}

	/** Every practice topic title some survey item points to. */
	public static Set<String> linkedTopics() {
		Set<String> topics = new LinkedHashSet<>();
		ITEMS.values().forEach(item -> topics.addAll(item.defaultTopics()));
		return Set.copyOf(topics);
	}

	/** A part sent as {@code null} counts as nothing chosen. */
	private static List<String> chosen(Map<String, List<String>> selections, String code) {
		List<String> chosen = selections.get(code);
		return chosen == null ? List.of() : chosen;
	}

	private static Map<String, SurveyItem> index() {
		Map<String, SurveyItem> items = new LinkedHashMap<>();
		PARTS.forEach(part -> part.items().forEach(item -> items.put(item.id(), item)));
		return Map.copyOf(items);
	}

	private static SurveyPart single(String code, String title, SurveyItem... items) {
		return new SurveyPart(code, title, 1, 1, List.of(items));
	}

	private static SurveyPart multiple(String code, String title, int min, SurveyItem... items) {
		return new SurveyPart(code, title, min, items.length, List.of(items));
	}

	private static SurveyItem item(String id, String label, String... topics) {
		return new SurveyItem(id, label, List.of(topics));
	}

	/** {@code min} to {@code max} items may be chosen in this part. */
	public record SurveyPart(String code, String title, int min, int max, List<SurveyItem> items) {
		boolean hasItem(String id) {
			return items.stream().anyMatch(item -> item.id().equals(id));
		}
	}

	/** @param defaultTopics practice topic titles the link table starts with; not sent to the screen */
	public record SurveyItem(String id, String label, @JsonIgnore List<String> defaultTopics) {}
}

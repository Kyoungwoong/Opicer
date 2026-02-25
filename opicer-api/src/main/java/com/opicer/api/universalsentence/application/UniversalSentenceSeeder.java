package com.opicer.api.universalsentence.application;

import com.opicer.api.universalsentence.domain.UniversalSentence;
import com.opicer.api.universalsentence.domain.UniversalSentenceType;
import com.opicer.api.universalsentence.infrastructure.UniversalSentenceRepository;
import java.util.List;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UniversalSentenceSeeder {

	@Bean
	ApplicationRunner seedUniversalSentences(UniversalSentenceRepository repository) {
		return args -> {
			if (repository.count() > 0) {
				return;
			}
			List<UniversalSentence> seeds = List.of(
				new UniversalSentence(UniversalSentenceType.OPINION, "의견 시작",
					"From my perspective, this topic is quite important because ...",
					List.of("opinion", "starter")),
				new UniversalSentence(UniversalSentenceType.OPINION, "의견 강조",
					"I strongly believe that this is one of the most meaningful choices for me.",
					List.of("opinion", "emphasis")),
				new UniversalSentence(UniversalSentenceType.OPINION, "의견 근거",
					"The main reason I think so is that it affects my daily routine directly.",
					List.of("opinion", "reason")),
				new UniversalSentence(UniversalSentenceType.OPINION, "의견 확장",
					"On top of that, it helps me maintain balance between work and personal life.",
					List.of("opinion", "expand")),
				new UniversalSentence(UniversalSentenceType.OPINION, "의견 비교",
					"Compared to other options, this feels more realistic and sustainable.",
					List.of("opinion", "compare")),
				new UniversalSentence(UniversalSentenceType.OPINION, "의견 양보",
					"While some people may disagree, I still think it is worth trying.",
					List.of("opinion", "concession")),
				new UniversalSentence(UniversalSentenceType.OPINION, "의견 결론",
					"In short, this is the best decision for me at the moment.",
					List.of("opinion", "summary")),
				new UniversalSentence(UniversalSentenceType.OPINION, "의견 사례",
					"For example, last month I experienced a similar situation and it worked out.",
					List.of("opinion", "example")),
				new UniversalSentence(UniversalSentenceType.OPINION, "의견 자신감",
					"I'm confident this approach will lead to better results over time.",
					List.of("opinion", "confidence")),
				new UniversalSentence(UniversalSentenceType.OPINION, "의견 추천",
					"I would recommend it to anyone who wants a practical solution.",
					List.of("opinion", "recommendation")),

				new UniversalSentence(UniversalSentenceType.PAST_EXPERIENCE, "과거 회상",
					"I still remember the time when I had to deal with a similar issue.",
					List.of("past", "memory")),
				new UniversalSentence(UniversalSentenceType.PAST_EXPERIENCE, "과거 시작",
					"A few years ago, I went through an experience that taught me a lot.",
					List.of("past", "intro")),
				new UniversalSentence(UniversalSentenceType.PAST_EXPERIENCE, "과거 배경",
					"Back then, I was in a completely different environment than I am now.",
					List.of("past", "context")),
				new UniversalSentence(UniversalSentenceType.PAST_EXPERIENCE, "과거 문제",
					"At first, I was nervous because I didn't know what to expect.",
					List.of("past", "challenge")),
				new UniversalSentence(UniversalSentenceType.PAST_EXPERIENCE, "과거 해결",
					"Eventually, I managed it by focusing on the basics and staying calm.",
					List.of("past", "solution")),
				new UniversalSentence(UniversalSentenceType.PAST_EXPERIENCE, "과거 교훈",
					"That experience taught me to be more prepared and flexible.",
					List.of("past", "lesson")),
				new UniversalSentence(UniversalSentenceType.PAST_EXPERIENCE, "과거 감정",
					"I felt relieved afterward because everything went better than I expected.",
					List.of("past", "emotion")),
				new UniversalSentence(UniversalSentenceType.PAST_EXPERIENCE, "과거 비교",
					"Looking back, I realize how much I have grown since then.",
					List.of("past", "reflection")),
				new UniversalSentence(UniversalSentenceType.PAST_EXPERIENCE, "과거 디테일",
					"We spent most of the day talking and sharing stories.",
					List.of("past", "detail")),
				new UniversalSentence(UniversalSentenceType.PAST_EXPERIENCE, "과거 마무리",
					"Overall, it was a memorable moment that I still appreciate today.",
					List.of("past", "summary")),

				new UniversalSentence(UniversalSentenceType.COMPARE_CONTRAST, "비교 시작",
					"Compared to the past, things have become much more convenient.",
					List.of("compare", "contrast")),
				new UniversalSentence(UniversalSentenceType.COMPARE_CONTRAST, "비교 장점",
					"The biggest difference is that everything is faster and more efficient now.",
					List.of("compare", "difference")),
				new UniversalSentence(UniversalSentenceType.COMPARE_CONTRAST, "비교 단점",
					"However, it also means people spend less time talking face-to-face.",
					List.of("compare", "downside")),
				new UniversalSentence(UniversalSentenceType.COMPARE_CONTRAST, "비교 중립",
					"In some ways, both options have their own strengths.",
					List.of("compare", "balanced")),
				new UniversalSentence(UniversalSentenceType.COMPARE_CONTRAST, "비교 예시",
					"For instance, studying online is flexible, but it can feel isolating.",
					List.of("compare", "example")),
				new UniversalSentence(UniversalSentenceType.COMPARE_CONTRAST, "비교 변화",
					"These changes have affected the way I plan my daily schedule.",
					List.of("compare", "impact")),
				new UniversalSentence(UniversalSentenceType.COMPARE_CONTRAST, "비교 결론",
					"If I had to choose, I would still prefer the current approach.",
					List.of("compare", "preference")),
				new UniversalSentence(UniversalSentenceType.COMPARE_CONTRAST, "비교 배경",
					"When I was younger, I relied more on traditional methods.",
					List.of("compare", "background")),
				new UniversalSentence(UniversalSentenceType.COMPARE_CONTRAST, "비교 확대",
					"Overall, the gap between the two options is wider than I expected.",
					List.of("compare", "summary")),
				new UniversalSentence(UniversalSentenceType.COMPARE_CONTRAST, "비교 결과",
					"As a result, my habits have shifted toward the newer style.",
					List.of("compare", "result")),

				new UniversalSentence(UniversalSentenceType.UNEXPECTED_SITUATION, "문제 상황",
					"Suddenly, the plan changed and I had to react quickly.",
					List.of("problem", "unexpected")),
				new UniversalSentence(UniversalSentenceType.UNEXPECTED_SITUATION, "문제 해결",
					"In that situation, the first thing I did was to stay calm and assess.",
					List.of("problem", "solution")),
				new UniversalSentence(UniversalSentenceType.UNEXPECTED_SITUATION, "문제 대응",
					"Then I contacted the right people to fix the issue.",
					List.of("problem", "action")),
				new UniversalSentence(UniversalSentenceType.UNEXPECTED_SITUATION, "문제 대안",
					"Since the original option wasn't available, I chose a simple alternative.",
					List.of("problem", "alternative")),
				new UniversalSentence(UniversalSentenceType.UNEXPECTED_SITUATION, "문제 조정",
					"I adjusted my schedule to make sure everything still fit.",
					List.of("problem", "adjustment")),
				new UniversalSentence(UniversalSentenceType.UNEXPECTED_SITUATION, "문제 설명",
					"I explained the situation clearly so others could understand.",
					List.of("problem", "explain")),
				new UniversalSentence(UniversalSentenceType.UNEXPECTED_SITUATION, "문제 결과",
					"Luckily, the situation was resolved faster than expected.",
					List.of("problem", "result")),
				new UniversalSentence(UniversalSentenceType.UNEXPECTED_SITUATION, "문제 교훈",
					"It taught me to always have a backup plan.",
					List.of("problem", "lesson")),
				new UniversalSentence(UniversalSentenceType.UNEXPECTED_SITUATION, "문제 마무리",
					"Afterward, I felt more confident in handling similar issues.",
					List.of("problem", "confidence")),
				new UniversalSentence(UniversalSentenceType.UNEXPECTED_SITUATION, "문제 협력",
					"Working together made the solution much more efficient.",
					List.of("problem", "teamwork"))
			);
			repository.saveAll(seeds);
		};
	}
}

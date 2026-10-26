package com.opicer.api.evaluation.domain;

/**
 * Stable codes for what a correction fixes. They are persisted with each evaluation so a later
 * speech profile can count a learner's recurring mistakes, so do not rename them.
 */
public enum CorrectionCategory {

	TENSE(IssueGroup.GRAMMAR, "시제", "과거 경험은 과거 시제로, 지금의 습관은 현재 시제로 일관되게 말해 보세요."),
	AGREEMENT(IssueGroup.GRAMMAR, "수 일치", "주어가 단수인지 복수인지 먼저 정하고 동사를 맞춰 보세요(he goes / they go)."),
	ARTICLE(IssueGroup.GRAMMAR, "관사", "처음 말하는 대상은 a, 이미 말한 대상은 the를 붙여 보세요."),
	PREPOSITION(IssueGroup.GRAMMAR, "전치사", "자주 쓰는 장소·시간 전치사(at, in, on)를 짝으로 외워 두세요."),
	OTHER_GRAMMAR(IssueGroup.GRAMMAR, "기타 문법", "짧은 문장으로 정확하게 말한 뒤 연결어로 길게 늘려 보세요."),
	WORD_CHOICE(IssueGroup.EXPRESSION, "단어 선택", "같은 뜻의 다른 단어를 두세 개씩 준비해 상황에 맞게 골라 쓰세요."),
	NATURALNESS(IssueGroup.EXPRESSION, "자연스러움", "직역 대신 원어민이 자주 쓰는 덩어리 표현(chunk)으로 바꿔 말해 보세요."),
	CONNECTOR(IssueGroup.STRUCTURE, "연결어", "and만 반복하지 말고 because, so, but then, after that으로 흐름을 만들어 보세요."),
	ORGANIZATION(IssueGroup.STRUCTURE, "구성", "첫 문장에서 답을 먼저 말하고, 예시 두 개, 마무리 한 문장 순서로 말해 보세요.");

	private final IssueGroup group;
	private final String labelKo;
	private final String tipKo;

	CorrectionCategory(IssueGroup group, String labelKo, String tipKo) {
		this.group = group;
		this.labelKo = labelKo;
		this.tipKo = tipKo;
	}

	public IssueGroup group() {
		return group;
	}

	public String labelKo() {
		return labelKo;
	}

	public String tipKo() {
		return tipKo;
	}

	/** The three highlighted-issue groups of the report (Opicer.md §5.4). */
	public enum IssueGroup {
		GRAMMAR("문법"),
		EXPRESSION("표현"),
		STRUCTURE("구성");

		private final String labelKo;

		IssueGroup(String labelKo) {
			this.labelKo = labelKo;
		}

		public String labelKo() {
			return labelKo;
		}
	}
}

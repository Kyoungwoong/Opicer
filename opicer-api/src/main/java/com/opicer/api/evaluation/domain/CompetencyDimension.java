package com.opicer.api.evaluation.domain;

import com.opicer.api.shared.domain.OpicLevel;
import java.util.EnumMap;
import java.util.Map;

/**
 * The competencies an answer is rated on — Opicer's own plain-language rubric, not the official ACTFL text.
 * Fluency is deliberately absent: a transcript cannot show pauses, so it is reported as measured signals only.
 * Descriptors line up with the guide page's LEVEL_GUIDES (opicer-web/src/features/guide/data.ts).
 */
public enum CompetencyDimension {

	TASK("과제 수행", levels(
		"질문의 일부 단어에만 반응하고 질문에 답하지 못해요.",
		"질문에 짧게 답하지만 구체적인 내용이 거의 없어요.",
		"질문에 맞게 답하고 이유나 예시를 하나둘 덧붙여요.",
		"질문의 요구(묘사·경험·비교)를 모두 다루고 구체적인 사례로 뒷받침해요.",
		"질문의 의도를 넘어 관점과 근거를 갖춰 설득력 있게 답해요."
	)),
	DISCOURSE("담화 구성", levels(
		"단어나 외운 구절을 나열해요.",
		"짧은 문장 위주이고 문장끼리 거의 연결되지 않아요.",
		"and, because, so 같은 연결어로 문장을 이어 말해요.",
		"도입-전개-마무리가 있는 문단 수준으로 말하고 시간 순서·비교를 엮어요.",
		"문단 단위로 자연스럽게 전개하고 원인·결과·의견을 매끄럽게 연결해요."
	)),
	ACCURACY("문법·시제", levels(
		"문장 형태를 갖추기 어려워 의미 전달이 자주 끊겨요.",
		"주로 현재 시제이고 기본 문장에서도 오류가 잦아요.",
		"현재·기본 과거 시제를 쓰며 오류가 있어도 의미는 통해요.",
		"과거·현재·미래를 대체로 정확히 쓰고 오류가 가끔 있어요.",
		"복잡한 문장에서도 시제와 문법이 대부분 정확해요."
	)),
	VOCABULARY("어휘·표현", levels(
		"아는 단어가 매우 적어 같은 단어만 반복해요.",
		"일상적인 기본 단어 위주이고 표현이 단조로워요.",
		"주제에 맞는 단어를 쓰지만 같은 표현을 자주 반복해요.",
		"다양한 표현과 연결 구문을 쓰고 상황에 맞게 바꿔 말해요.",
		"관용 표현과 정확한 어휘로 뉘앙스까지 자연스럽게 전달해요."
	));

	private final String labelKo;
	private final Map<OpicLevel, String> descriptors;

	CompetencyDimension(String labelKo, Map<OpicLevel, String> descriptors) {
		this.labelKo = labelKo;
		this.descriptors = descriptors;
	}

	public String labelKo() {
		return labelKo;
	}

	public String descriptor(OpicLevel level) {
		return descriptors.get(level);
	}

	private static Map<OpicLevel, String> levels(String nl, String il, String im, String ih, String al) {
		Map<OpicLevel, String> map = new EnumMap<>(OpicLevel.class);
		map.put(OpicLevel.NL, nl);
		map.put(OpicLevel.IL, il);
		map.put(OpicLevel.IM, im);
		map.put(OpicLevel.IH, ih);
		map.put(OpicLevel.AL, al);
		return Map.copyOf(map);
	}
}

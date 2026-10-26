import type { ExamStep, GuideSection, LevelGuide, TipItem } from "@/features/guide/types";

export const GUIDE_SECTIONS: GuideSection[] = [
  {
    id: "intro",
    title: "오픽 설명",
    summary: "OPIC 시험의 구조와 레벨 기준을 빠르게 이해하세요.",
  },
  {
    id: "schedule",
    title: "오픽 일정",
    summary: "시험 일정과 접수 기간은 opic.or.kr 공식 사이트에서 확인해요.",
  },
  {
    id: "centers",
    title: "시험센터 & 후기",
    summary: "지역별 시험센터 위치와 응시자 후기를 확인하세요.",
  },
  {
    id: "tips",
    title: "오픽 보러 갈 때의 꿀팁",
    summary: "준비물, 시간 관리, 멘탈 케어까지 체크리스트.",
  },
];

export const TIP_ITEMS: TipItem[] = [
  {
    id: "t1",
    title: "시험 전 10분 워밍업",
    detail: "간단한 주제에 대해 1분씩 말하며 입을 풀어보세요.",
  },
  {
    id: "t2",
    title: "답변 구조를 미리 정리",
    detail: "인트로–경험–결론 3단계를 의식하면 안정적입니다.",
  },
  {
    id: "t3",
    title: "시간 압박에 대비",
    detail: "질문을 듣자마자 첫 문장을 바로 말할 수 있게 준비하세요.",
  },
  {
    id: "t4",
    title: "컨디션 관리",
    detail: "시험 전날 수면을 충분히 확보하고 카페인은 과하게 마시지 마세요.",
  },
];

/** OPIc test-day flow. Durations are approximate; the test itself runs about 40 minutes. */
export const EXAM_STEPS: ExamStep[] = [
  {
    id: "orientation",
    label: "오리엔테이션",
    title: "오리엔테이션",
    duration: "약 20분 (설문·자가평가 포함)",
    description: "본 시험 전에 화면 구성, 헤드셋 볼륨, 녹음 방법을 안내받아요. 이 시간은 시험 시간 40분에 포함되지 않아요.",
    tip: "헤드셋 볼륨과 마이크를 이때 꼭 확인하세요. 시험이 시작되면 조정하기 어려워요.",
  },
  {
    id: "survey",
    label: "설문",
    title: "Background Survey",
    duration: "오리엔테이션 중",
    description: "직업, 거주 형태, 여가, 취미, 운동, 여행 등을 고르는 설문이에요. 여기서 고른 항목이 실제 질문 주제가 돼요.",
    tip: "실제로 말하기 쉬운 주제를 고르고, 비슷한 주제(공원·산책·조깅처럼)를 묶어 준비하면 대비 범위가 줄어요.",
  },
  {
    id: "self-assessment",
    label: "자가평가",
    title: "Self Assessment",
    duration: "오리엔테이션 중",
    description: "6단계 예시 답변을 듣고 내 수준에 맞는 난이도를 골라요. 이 선택이 첫 질문 세트의 난이도를 정해요.",
    tip: "목표 레벨보다 너무 낮게 고르면 높은 등급을 받기 어려워요. 목표에 맞는 단계를 미리 정해 두세요.",
  },
  {
    id: "first-half",
    label: "1차 질문",
    title: "1차 질문 세트",
    duration: "약 7문항",
    description: "자기소개로 시작해 설문 주제, 돌발 주제, 롤플레이 질문이 이어져요. 질문은 한 번 더 들을 수 있어요.",
    tip: "질문을 다 듣고 바로 첫 문장을 시작하세요. 침묵이 길면 흐름이 끊겨 보여요.",
  },
  {
    id: "adjust",
    label: "난이도 조정",
    title: "난이도 재조정",
    duration: "짧은 선택",
    description: "1차 세트가 끝나면 2차 질문을 '비슷하게 / 더 쉽게 / 더 어렵게' 중에서 고를 수 있어요.",
    tip: "컨디션이 괜찮다면 '비슷하게' 이상을 고르는 편이 높은 레벨을 노리기에 유리해요.",
  },
  {
    id: "second-half",
    label: "2차 질문",
    title: "2차 질문 세트",
    duration: "나머지 문항 (총 12~15문항)",
    description: "조정한 난이도로 나머지 질문이 나와요. 비교, 과거 경험, 문제 해결처럼 더 긴 답변을 요구하는 질문이 많아요.",
    tip: "남은 시간을 확인하면서 질문마다 비슷한 길이로 답하세요. 마지막 질문까지 힘을 남겨 두세요.",
  },
  {
    id: "result",
    label: "결과",
    title: "결과 확인",
    duration: "보통 약 1주 후 (조기 발표 회차는 더 빨라요)",
    description: "opic.or.kr에서 등급을 확인해요. 등급은 NL부터 AL까지이고, IM은 IM1·IM2·IM3로 나뉘어요.",
    tip: "Opicer의 레벨은 연습용 예상치예요. 공식 등급은 시험 결과로만 확인할 수 있어요.",
  },
];

/** Plain-language summaries for practice; not the official ACTFL/OPIc rubric. */
export const LEVEL_GUIDES: LevelGuide[] = [
  {
    id: "AL",
    label: "AL",
    summary: "어떤 주제든 문단 단위로 자연스럽게 이야기해요.",
    structure: "문단 단위 전개, 원인·결과·의견을 엮어 설득력 있게 말해요.",
    tense: "과거·현재·미래를 자유롭게 넘나들며 대부분 정확해요.",
    length: "질문당 2분 안팎, 내용이 풍부해요.",
    example: "Looking back, what changed my mind was not the price but how the place made me feel, and that's why I still go there.",
    nextStep: "최고 등급이에요. 돌발·롤플레이에서도 흐름을 유지하는 연습으로 실력을 지켜요.",
  },
  {
    id: "IH",
    label: "IH",
    summary: "문단 수준으로 말하려 하고, 비교와 서사가 가능해요.",
    structure: "여러 문장을 연결해 시간 순서·비교·묘사를 해요.",
    tense: "시제를 대체로 정확히 쓰지만 가끔 실수가 있어요.",
    length: "질문당 1분 30초 이상",
    example: "When I was in college, I used to study at cafés, but these days I prefer the library because it's much quieter.",
    nextStep: "AL로 가려면 어려운 질문에서도 문단 구조와 시제 정확도를 끝까지 유지하세요.",
  },
  {
    id: "IM",
    label: "IM",
    summary: "익숙한 주제에서 문장을 이어 말할 수 있어요. 많은 응시자가 목표로 하는 등급대예요.",
    structure: "and, because, so 같은 연결어로 문장을 이어요.",
    tense: "현재 시제 위주, 기본적인 과거 시제를 써요.",
    length: "질문당 1분 안팎",
    example: "I usually go to the park on weekends because it's quiet, and I like to walk there with my dog.",
    nextStep: "IH로 가려면 과거 경험을 시간 순서대로 풀고, 비교 표현(compared to, used to)을 늘려 보세요.",
  },
  {
    id: "IL",
    label: "IL / NL",
    summary: "외운 표현과 짧은 문장으로 기본적인 내용을 말해요.",
    structure: "단어나 짧은 문장 위주, 연결이 거의 없어요.",
    tense: "주로 현재 시제",
    length: "질문당 30초 이하인 경우가 많아요.",
    example: "I like coffee. It is good. I drink coffee every day.",
    nextStep: "IM으로 가려면 한 문장에 이유(because)를 붙여 두세 문장으로 늘리는 연습부터 하세요.",
  },
];

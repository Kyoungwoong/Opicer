import type { GuideSection, TipItem } from "@/features/guide/types";

export const GUIDE_SECTIONS: GuideSection[] = [
  {
    id: "intro",
    title: "오픽 설명",
    summary: "OPIC 시험의 구조와 레벨 기준을 빠르게 이해하세요.",
  },
  {
    id: "schedule",
    title: "오픽 일정",
    summary: "opic.or.kr 시험 일정과 접수 기간을 달력으로 확인하세요.",
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

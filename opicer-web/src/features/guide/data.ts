import type {
  GuideSection,
  OpicCalendar,
  OpicScheduleItem,
  ReviewGroup,
  TipItem,
} from "@/features/guide/types";

export const GUIDE_SECTIONS: GuideSection[] = [
  {
    id: "intro",
    title: "OPIC Overview",
    summary: "OPIC text text text text text text.",
  },
  {
    id: "schedule",
    title: "OPIC Schedule",
    summary: "text text text text text (Integration pending).",
  },
  {
    id: "reviews",
    title: "Test Center Reviews",
    summary: "text mintext text text text text.",
  },
  {
    id: "tips",
    title: "Test Day Tips",
    summary: "text, text text, text text text.",
  },
];

export const OPIC_SCHEDULE: OpicScheduleItem[] = [
  {
    id: "s1",
    dateLabel: "3text 7text",
    title: "text text",
    note: "text 10text text text",
  },
  {
    id: "s2",
    dateLabel: "3text 21text",
    title: "text text",
    note: "text 2text text text",
  },
];

export const OPIC_CALENDAR: OpicCalendar = {
  monthLabel: "2026text 3text",
  startWeekday: 0,
  daysInMonth: 31,
  events: [
    { id: "c1", date: 7, label: "text text", type: "registration" },
    { id: "c2", date: 21, label: "text text", type: "exam" },
  ],
};

export const REVIEW_GROUPS: ReviewGroup[] = [
  {
    id: "gangnam",
    classroom: "text 3text",
    reviews: [
      {
        id: "g1",
        quote: "text text text text text.",
        rating: 5,
      },
      {
        id: "g2",
        quote: "text text text text text text text.",
        rating: 4,
      },
      {
        id: "g3",
        quote: "text text text itemstext text text.",
        rating: 4,
      },
    ],
  },
  {
    id: "jongro",
    classroom: "text 5text",
    reviews: [
      {
        id: "j1",
        quote: "text text text text text text.",
        rating: 4,
      },
      {
        id: "j2",
        quote: "text text text Question text text.",
        rating: 5,
      },
    ],
  },
  {
    id: "busan",
    classroom: "text 2text",
    reviews: [
      {
        id: "b1",
        quote: "text text text text text text.",
        rating: 4,
      },
      {
        id: "b2",
        quote: "text text text text text.",
        rating: 5,
      },
    ],
  },
];

export const TIP_ITEMS: TipItem[] = [
  {
    id: "t1",
    title: "text text 10min text",
    detail: "text Topictext text 1mintext text text text.",
  },
  {
    id: "t2",
    title: "text text text text",
    detail: "text 3text text text.",
  },
  {
    id: "t3",
    title: "text text text",
    detail: "Questiontext text text Sentencetext text text text text text.",
  },
  {
    id: "t4",
    title: "text text",
    detail: "text text text textmintext text text text text text.",
  },
];

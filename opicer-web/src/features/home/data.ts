import type {
  OpicCalendar,
  RecentActivity,
  UniversalSentence,
} from "@/features/home/types";
import { frontPageText } from "@/locales/frontPage";

export const UNIVERSAL_SENTENCES: UniversalSentence[] = [
  {
    id: "u1",
    type: "OPINION",
    title: frontPageText.homeData.universalTitles.opinion,
    sentence: "From my perspective, this topic is quite important because...",
    tags: ["opinion", "starter"],
  },
  {
    id: "u2",
    type: "PAST_EXPERIENCE",
    title: frontPageText.homeData.universalTitles.experience,
    sentence: "I still remember the time when I had to deal with...",
    tags: ["experience", "past"],
  },
  {
    id: "u3",
    type: "COMPARE_CONTRAST",
    title: frontPageText.homeData.universalTitles.compare,
    sentence: "Compared to the past, things have become much more...",
    tags: ["compare", "contrast"],
  },
  {
    id: "u4",
    type: "UNEXPECTED_SITUATION",
    title: frontPageText.homeData.universalTitles.solution,
    sentence: "In that situation, the first thing I did was to...",
    tags: ["solution", "structure"],
  },
];

export const RECENT_ACTIVITIES: RecentActivity[] = [
  {
    id: "a1",
    title: "Mock Test #12",
    summary: frontPageText.homeData.recent.summary1,
    dateLabel: frontPageText.homeData.recent.today,
    status: "complete",
  },
  {
    id: "a2",
    title: frontPageText.homeData.recent.title2,
    summary: frontPageText.homeData.recent.summary2,
    dateLabel: frontPageText.homeData.recent.yesterday,
    status: "in_progress",
  },
  {
    id: "a3",
    title: frontPageText.homeData.recent.title3,
    summary: frontPageText.homeData.recent.summary3,
    dateLabel: frontPageText.homeData.recent.recommended,
    status: "recommended",
  },
];

export const OPIC_CALENDAR: OpicCalendar = {
  monthLabel: frontPageText.homeData.calendar.monthLabel,
  startWeekday: 0,
  daysInMonth: 31,
  events: [
    {
      id: "e1",
      date: 7,
      label: frontPageText.homeData.calendar.registrationStart,
      type: "registration",
    },
    {
      id: "e2",
      date: 21,
      label: frontPageText.homeData.calendar.regularExam,
      type: "exam",
    },
  ],
};

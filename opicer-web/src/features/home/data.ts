import type {
  OpicCalendar,
  UniversalSentence,
} from "@/features/home/types";
import { ko } from "@/locales/ko";

export const UNIVERSAL_SENTENCES: UniversalSentence[] = [
  {
    id: "u1",
    type: "OPINION",
    title: ko.home.homeData.universalTitles.opinion,
    sentence: "From my perspective, this topic is quite important because...",
    tags: ["opinion", "starter"],
  },
  {
    id: "u2",
    type: "PAST_EXPERIENCE",
    title: ko.home.homeData.universalTitles.experience,
    sentence: "I still remember the time when I had to deal with...",
    tags: ["experience", "past"],
  },
  {
    id: "u3",
    type: "COMPARE_CONTRAST",
    title: ko.home.homeData.universalTitles.compare,
    sentence: "Compared to the past, things have become much more...",
    tags: ["compare", "contrast"],
  },
  {
    id: "u4",
    type: "UNEXPECTED_SITUATION",
    title: ko.home.homeData.universalTitles.solution,
    sentence: "In that situation, the first thing I did was to...",
    tags: ["solution", "structure"],
  },
];

export const OPIC_CALENDAR: OpicCalendar = {
  monthLabel: ko.home.homeData.calendar.monthLabel,
  startWeekday: 0,
  daysInMonth: 31,
  events: [
    {
      id: "e1",
      date: 7,
      label: ko.home.homeData.calendar.registrationStart,
      type: "registration",
    },
    {
      id: "e2",
      date: 21,
      label: ko.home.homeData.calendar.regularExam,
      type: "exam",
    },
  ],
};

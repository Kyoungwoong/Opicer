export type UniversalSentence = {
  id: string;
  type: "OPINION" | "PAST_EXPERIENCE" | "COMPARE_CONTRAST" | "UNEXPECTED_SITUATION";
  title: string;
  sentence: string;
  tags: string[];
};

export type RecentActivity = {
  id: string;
  title: string;
  summary: string;
  dateLabel: string;
  status: "complete" | "in_progress" | "recommended";
};

export type OpicScheduleEvent = {
  id: string;
  date: number;
  label: string;
  type: "exam" | "registration";
};

export type OpicCalendar = {
  monthLabel: string;
  startWeekday: number;
  daysInMonth: number;
  events: OpicScheduleEvent[];
};

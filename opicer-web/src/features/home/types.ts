export type UniversalSentence = {
  id: string;
  title: string;
  sentence: string;
  tags: string[];
  situation: string;
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

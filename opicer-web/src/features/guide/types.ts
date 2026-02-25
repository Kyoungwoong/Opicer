export type GuideSection = {
  id: string;
  title: string;
  summary: string;
};

export type OpicScheduleItem = {
  id: string;
  dateLabel: string;
  title: string;
  note: string;
};

export type OpicCalendarEvent = {
  id: string;
  date: number;
  label: string;
  type: "exam" | "registration";
};

export type OpicCalendar = {
  monthLabel: string;
  startWeekday: number;
  daysInMonth: number;
  events: OpicCalendarEvent[];
};

export type ReviewEntry = {
  id: string;
  quote: string;
  rating: number;
};

export type ReviewGroup = {
  id: string;
  classroom: string;
  reviews: ReviewEntry[];
};

export type TipItem = {
  id: string;
  title: string;
  detail: string;
};

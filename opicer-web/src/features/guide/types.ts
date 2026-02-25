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

export type ReviewItem = {
  id: string;
  classroom: string;
  quote: string;
  rating: number;
};

export type TipItem = {
  id: string;
  title: string;
  detail: string;
};

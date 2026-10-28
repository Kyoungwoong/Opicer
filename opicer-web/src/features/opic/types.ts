/** One exam round. Dates are ISO local dates (YYYY-MM-DD); entered manually for now, not collected from opic.or.kr. */
export type OpicSchedule = {
  examDate: string;
  registrationStart: string | null;
  registrationEnd: string | null;
  scoreReleaseDate: string | null;
  roundName: string;
  earlyRelease: boolean;
};

export type OpicScheduleMonth = {
  /** YYYY-MM */
  month: string;
  schedules: OpicSchedule[];
};

export type OpicCenter = {
  id: string;
  name: string;
  address: string;
  latitude: number | null;
  longitude: number | null;
  phone: string | null;
  subwayLines: string | null;
  directions: string | null;
  regionName: string | null;
  reviewCount: number;
  /** One decimal; null when the center has no reviews. */
  averageRating: number | null;
};

export type OpicCenterReview = {
  id: string;
  rating: number;
  content: string;
  sourceLabel: string | null;
  createdAt: string;
};

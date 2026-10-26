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

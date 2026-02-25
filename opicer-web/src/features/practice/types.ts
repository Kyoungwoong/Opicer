export type TopicBadge = {
  label: string;
  count?: number;
};

export type TopicItem = {
  id: string;
  title: string;
  english: string;
  badges?: TopicBadge[];
};

export type TopicCategory = {
  id: string;
  label: string;
  description: string;
  topics: TopicItem[];
};

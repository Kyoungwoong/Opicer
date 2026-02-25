export type TopicBadge = {
  label: string;
  count?: number | null;
};

export type TopicItem = {
  id: string;
  title: string;
  englishTitle: string;
  category: string;
  categoryOrder: number;
  topicOrder: number;
  badges: TopicBadge[];
  active: boolean;
};

export type TopicCategory = {
  id: string;
  label: string;
  description: string;
  topics: TopicItem[];
};

export type TopicSelection = {
  id: string;
  topicId: string;
  selectedAt: string;
};

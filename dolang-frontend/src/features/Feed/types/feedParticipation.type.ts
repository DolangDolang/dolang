export interface FeedParticipationResponse {
  isSuccess: boolean;
  message: '성공' | '실패';
  code: number;
  result: FeedParticipation[];
  meta: FeedParticipationMeta;
}

export interface FeedParticipationMeta {
  sort: 'likes' | 'bookmark';
  limit: number;
  nextCursor: string;
  hasNext: boolean;
}

export interface FeedParticipation {
  profileImage: string;
  country: string;
  countryImageUrl: string;
  voiceUrl: string;
  voiceCreatedAt: string;
  heartCount?: number;
  bookmarkCount?: number;
}
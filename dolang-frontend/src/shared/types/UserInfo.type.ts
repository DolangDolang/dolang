export interface Interest {
  tagId?: number;
  name?: string;
}

export interface User {
  profileImageUrl?: string | null;
  nickname: string;
  nationality: string;
  nativeLanguage: string | null;
  targetLanguage: string | null;
  proficiencyLevel: string | null;
  interests: Interest[];
  profileImage?: File | null;
}

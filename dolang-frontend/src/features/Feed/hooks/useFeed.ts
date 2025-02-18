import { useQuery } from '@tanstack/react-query';
import { getFeed, getFeedParticipants } from '../services/feedService';
import { getMyFeed } from '../services/myFeedService';
import { MyFeedRequest } from '../types/MyFeedRequest.type';
import { FeedParticipantsRequest } from '../types/FeedParticipantsRequest.type';
// 피드 데이터 페칭
export const useFeedSentence = (lang: 'ko' | 'en') => {
  const { data, isLoading, error } = useQuery({
    queryKey: ['feedSentence', lang],
    queryFn: () => getFeed({ lang }),
  });

  return { data, isLoading, error };
};

// 피드 참여 데이터 페칭
export const useFeedParticipaticipants = (params: Partial<FeedParticipantsRequest>) => {
  const { data, isLoading, error } = useQuery({
    queryKey: ['feedParticipants', params],
    queryFn: () => getFeedParticipants({ ...params, length: params.length ?? 10 }),
  });

  return { data, isLoading, error };
};

// 내 피드 페칭
export const useMyFeed = (params: Partial<MyFeedRequest>) => {
  const { data, isLoading, error } = useQuery({
    queryKey: ['myFeed', params],
    queryFn: () => getMyFeed(params),
  });

  return { data, isLoading, error };
};

// 유저 피드 참여
// export const useUserFeedParticipation = () => {
//   const { mutate, isPending, error } = useMutation({
//     mutationFn: postFeedParticipation,
//   });
//   return { mutate, isPending, error };
// };

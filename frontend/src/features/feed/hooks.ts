import { useGetNew } from '@/api/generated/ranking/ranking'

export function useAskPosts(page = 0, size = 20) {
  return useGetNew({ page, size, type: 'ASK' })
}

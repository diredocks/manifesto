import { useGetPostsByUser } from '@/api/generated/posts/posts'

export function useUserPosts(username: string, page = 0, size = 20) {
  return useGetPostsByUser(username, { page, size }, {
    query: { enabled: !!username },
  })
}

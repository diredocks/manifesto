import { useGetPostsByUser } from '@/api/generated/posts/posts'
import { useGetUserProfile } from '@/api/generated/users/users'
import { useGetCommentsByUser } from '@/api/generated/comments/comments'

export function useUserPosts(username: string, page = 0, size = 20) {
  return useGetPostsByUser(username, { page, size }, {
    query: { enabled: !!username },
  })
}

export function useUserProfile(username: string) {
  return useGetUserProfile(username, {
    query: { enabled: !!username },
  })
}

export function useUserComments(username: string, page = 0, size = 20) {
  return useGetCommentsByUser(username, { page, size }, {
    query: { enabled: !!username },
  })
}

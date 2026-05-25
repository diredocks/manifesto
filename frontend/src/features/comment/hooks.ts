import { useCreateComment as useCreateCommentGen } from '@/api/generated/comments/comments'

export function useCreateComment() {
  return useCreateCommentGen()
}

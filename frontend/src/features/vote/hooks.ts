import { useQueryClient } from '@tanstack/react-query'
import { useUpvote, useRemoveVote } from '@/api/generated/voting/voting'

export function useUpvotePost(postId: number) {
  const queryClient = useQueryClient()

  return useUpvote({
    mutation: {
      onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: [`/api/v1/posts/${postId}/vote-status`] })
        queryClient.invalidateQueries({ queryKey: [`/api/v1/posts/${postId}/vote-count`] })
        queryClient.invalidateQueries({ queryKey: ['/api/v1/ranking'] })
        queryClient.invalidateQueries({ queryKey: [`/api/v1/posts/${postId}`] })
      },
    },
  })
}

export function useRemoveUpvotePost(postId: number) {
  const queryClient = useQueryClient()

  return useRemoveVote({
    mutation: {
      onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: [`/api/v1/posts/${postId}/vote-status`] })
        queryClient.invalidateQueries({ queryKey: [`/api/v1/posts/${postId}/vote-count`] })
        queryClient.invalidateQueries({ queryKey: ['/api/v1/ranking'] })
        queryClient.invalidateQueries({ queryKey: [`/api/v1/posts/${postId}`] })
      },
    },
  })
}

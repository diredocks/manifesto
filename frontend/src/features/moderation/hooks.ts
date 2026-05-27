import { useQueryClient } from '@tanstack/react-query'
import { useDeletePost } from '@/api/generated/posts/posts'
import { useDeleteComment1 } from '@/api/generated/comments/comments'
import {
  useDeletePost1,
  useDeleteComment,
  useBanUser as useBanUserGen,
  useUnbanUser as useUnbanUserGen,
} from '@/api/generated/moderator/moderator'

export function useDeleteOwnPost() {
  const queryClient = useQueryClient()

  return useDeletePost({
    mutation: {
      onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: ['/api/v1/ranking'] })
        queryClient.invalidateQueries({ queryKey: ['/api/v1/posts'] })
      },
    },
  })
}

export function useDeleteOwnComment(postId: number) {
  const queryClient = useQueryClient()

  return useDeleteComment1({
    mutation: {
      onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: [`/api/v1/posts/${postId}/comments`] })
      },
    },
  })
}

export function useModDeletePost() {
  const queryClient = useQueryClient()

  return useDeletePost1({
    mutation: {
      onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: ['/api/v1/ranking'] })
        queryClient.invalidateQueries({ queryKey: ['/api/v1/posts'] })
      },
    },
  })
}

export function useModDeleteComment(postId: number) {
  const queryClient = useQueryClient()

  return useDeleteComment({
    mutation: {
      onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: [`/api/v1/posts/${postId}/comments`] })
      },
    },
  })
}

export function useBanUser() {
  const queryClient = useQueryClient()

  return useBanUserGen({
    mutation: {
      onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: ['/api/v1/admin/users'] })
        queryClient.invalidateQueries({ queryKey: ['/api/v1/moderator/users'] })
      },
    },
  })
}

export function useUnbanUser() {
  const queryClient = useQueryClient()

  return useUnbanUserGen({
    mutation: {
      onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: ['/api/v1/admin/users'] })
        queryClient.invalidateQueries({ queryKey: ['/api/v1/moderator/users'] })
      },
    },
  })
}

import { useNavigate } from 'react-router-dom'
import { useCreatePost } from '@/api/generated/posts/posts'
import { useQueryClient } from '@tanstack/react-query'

export function useSubmitPost() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  return useCreatePost({
    mutation: {
      onSuccess: (response) => {
        queryClient.invalidateQueries({ queryKey: ['/api/v1/ranking'] })
        if (response.data) {
          navigate(`/item/${response.data.id}`)
        }
      },
    },
  })
}

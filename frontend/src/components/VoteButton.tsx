import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/features/auth/store'
import { useVoteStatus } from '@/api/generated/voting/voting'
import { useUpvotePost, useRemoveUpvotePost } from '@/features/vote/hooks'

interface VoteButtonProps {
  postId: number
}

export function VoteButton({ postId }: VoteButtonProps) {
  const token = useAuthStore((s) => s.token)
  const navigate = useNavigate()

  const { data: voteStatusData } = useVoteStatus(postId, {
    query: { enabled: !!token && postId > 0 },
  })

  const upvote = useUpvotePost(postId)
  const removeVote = useRemoveUpvotePost(postId)

  const hasVoted = voteStatusData?.data === true
  const isLoading = upvote.isPending || removeVote.isPending

  const handleClick = () => {
    if (!token) {
      navigate('/login')
      return
    }
    if (hasVoted) {
      removeVote.mutate({ postId })
    } else {
      upvote.mutate({ postId })
    }
  }

  return (
    <button
      onClick={handleClick}
      disabled={isLoading}
      className={`text-xs cursor-pointer bg-transparent border-none p-0 leading-none ${
        hasVoted ? 'text-primary' : 'text-gray-400 hover:text-primary'
      }`}
      title={hasVoted ? 'Remove upvote' : 'Upvote'}
    >
      ▲
    </button>
  )
}

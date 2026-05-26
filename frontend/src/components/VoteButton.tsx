import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/features/auth/store'
import { useVoteStatus, useVoteStatusComment } from '@/api/generated/voting/voting'
import {
  useUpvotePost,
  useRemoveUpvotePost,
  useUpvoteCommentVote,
  useRemoveUpvoteCommentVote,
} from '@/features/vote/hooks'

interface VoteButtonProps {
  postId?: number
  commentId?: number
}

export function VoteButton({ postId, commentId }: VoteButtonProps) {
  const token = useAuthStore((s) => s.token)
  const navigate = useNavigate()

  const postVoteStatus = useVoteStatus(postId ?? 0, {
    query: { enabled: !!token && !!postId && postId > 0 },
  })

  const commentVoteStatus = useVoteStatusComment(commentId ?? 0, {
    query: { enabled: !!token && !!commentId && commentId > 0 },
  })

  const upvotePost = useUpvotePost(postId ?? 0)
  const removeVotePost = useRemoveUpvotePost(postId ?? 0)
  const upvoteComment = useUpvoteCommentVote(commentId ?? 0)
  const removeVoteComment = useRemoveUpvoteCommentVote(commentId ?? 0)

  const hasVoted = postId
    ? postVoteStatus.data?.data === true
    : commentVoteStatus.data?.data === true

  const isLoading =
    upvotePost.isPending ||
    removeVotePost.isPending ||
    upvoteComment.isPending ||
    removeVoteComment.isPending

  const handleClick = () => {
    if (!token) {
      navigate('/login')
      return
    }
    if (postId) {
      if (hasVoted) {
        removeVotePost.mutate({ postId })
      } else {
        upvotePost.mutate({ postId })
      }
    } else if (commentId) {
      if (hasVoted) {
        removeVoteComment.mutate({ commentId })
      } else {
        upvoteComment.mutate({ commentId })
      }
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

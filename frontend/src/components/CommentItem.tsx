import { Link } from 'react-router-dom'
import type { CommentResponse } from '@/api/generated/model'
import { VoteButton } from '@/components/VoteButton'

interface CommentItemProps {
  comment: CommentResponse
  currentUsername?: string
  isMod?: boolean
  onDelete?: (commentId: number) => void
}

function timeAgo(dateStr: string): string {
  const now = Date.now()
  const diff = now - new Date(dateStr).getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 60) return `${minutes}m ago`
  if (hours < 24) return `${hours}h ago`
  if (days < 30) return `${days}d ago`
  return new Date(dateStr).toLocaleDateString()
}

export function CommentItem({ comment, currentUsername, isMod, onDelete }: CommentItemProps) {
  if (comment.deleted) {
    return (
      <div className="pl-4 py-1">
        <p className="text-xs text-gray-400 italic">[deleted]</p>
      </div>
    )
  }

  const canDelete = currentUsername === comment.authorUsername || isMod

  return (
    <div className="py-1" style={{ paddingLeft: `${comment.depth * 16}px` }}>
      <div className="text-xs text-gray-500 mb-1 flex items-center gap-1">
        <VoteButton commentId={comment.id} />
        <Link to={`/user/${comment.authorUsername}`} className="text-gray-500 no-underline hover:underline">
          {comment.authorUsername}
        </Link>{' '}
        <span>{comment.score} {comment.score === 1 ? 'point' : 'points'}</span>{' '}
        {timeAgo(comment.createdAt)}
        {canDelete && onDelete && (
          <>
            {' | '}
            <button
              onClick={() => onDelete(comment.id)}
              className="text-xs text-red-600 hover:underline cursor-pointer"
            >
              delete
            </button>
          </>
        )}
      </div>
      <p className="text-sm whitespace-pre-wrap">{comment.content}</p>
      {comment.children.length > 0 &&
        comment.children.map((child) => (
          <CommentItem
            key={child.id}
            comment={child}
            currentUsername={currentUsername}
            isMod={isMod}
            onDelete={onDelete}
          />
        ))}
    </div>
  )
}

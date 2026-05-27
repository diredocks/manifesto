import { Link } from 'react-router-dom'
import type { PostResponse } from '@/api/generated/model'
import { VoteButton } from './VoteButton'
import { timeAgo } from '@/lib/date'

interface PostItemProps {
  post: PostResponse
  rank?: number
}

function extractDomain(url?: string): string | null {
  if (!url) return null
  try {
    return new URL(url).hostname.replace('www.', '')
  } catch {
    return null
  }
}

export function PostItem({ post, rank }: PostItemProps) {
  const domain = extractDomain(post.url)

  return (
    <div className="flex items-start gap-1 py-1 text-sm">
      {rank !== undefined && (
        <span className="text-xs text-gray-500 w-6 text-right shrink-0 pt-0.5">{rank}.</span>
      )}
      <div className="shrink-0 pt-0.5">
        <VoteButton postId={post.id} />
      </div>
      <div className="min-w-0">
        <div className="flex items-baseline gap-1 flex-wrap">
          {post.url ? (
            <a
              href={post.url}
              target="_blank"
              rel="noopener noreferrer"
              className="text-link visited:text-link-visited no-underline hover:underline"
            >
              {post.title}
            </a>
          ) : (
            <Link to={`/item/${post.id}`} className="text-link visited:text-link-visited no-underline hover:underline">
              {post.title}
            </Link>
          )}
          {domain && <span className="text-xs text-gray-500">({domain})</span>}
        </div>
        <div className="text-xs text-gray-500">
          {post.score} points by{' '}
          <Link to={`/user/${post.authorUsername}`} className="text-gray-500 no-underline hover:underline">
            {post.authorUsername}
          </Link>{' '}
          {timeAgo(post.createdAt)}{' '}
          |{' '}
          <Link to={`/item/${post.id}`} className="text-gray-500 no-underline hover:underline">
            {post.commentCount > 0 ? `${post.commentCount} comments` : 'discuss'}
          </Link>
        </div>
      </div>
    </div>
  )
}

import { Link } from 'react-router-dom'
import type { PostResponse } from '@/api/generated/model'

interface PostItemProps {
  post: PostResponse
  rank?: number
}

function timeAgo(dateStr: string): string {
  const now = Date.now()
  const then = new Date(dateStr).getTime()
  const diff = now - then
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 60) return `${minutes}m ago`
  if (hours < 24) return `${hours}h ago`
  if (days < 30) return `${days}d ago`
  return new Date(dateStr).toLocaleDateString()
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
    <div className="flex items-start gap-2 py-1 text-sm">
      {rank !== undefined && (
        <span className="text-xs text-gray-500 w-6 text-right shrink-0">{rank}.</span>
      )}
      <div className="min-w-0">
        <div className="flex items-baseline gap-1 flex-wrap">
          {post.url ? (
            <a
              href={post.url}
              target="_blank"
              rel="noopener noreferrer"
              className="text-link visited:text-link-visited"
            >
              {post.title}
            </a>
          ) : (
            <Link to={`/item/${post.id}`} className="text-link visited:text-link-visited">
              {post.title}
            </Link>
          )}
          {domain && <span className="text-xs text-gray-500">({domain})</span>}
        </div>
        <div className="text-xs text-gray-500">
          {post.score} points by{' '}
          <Link to={`/user/${post.authorUsername}`} className="text-gray-500 hover:underline">
            {post.authorUsername}
          </Link>{' '}
          {timeAgo(post.createdAt)}{' '}
          |{' '}
          <Link to={`/item/${post.id}`} className="text-gray-500 hover:underline">
            {post.commentCount > 0 ? `${post.commentCount} comments` : 'discuss'}
          </Link>
        </div>
      </div>
    </div>
  )
}

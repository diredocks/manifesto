import { Link } from 'react-router-dom'
import type { PostResponse } from '@/api/generated/model'
import { PostItem } from './PostItem'

interface PostListProps {
  posts: PostResponse[] | undefined
  isLoading: boolean
  isError: boolean
  error?: unknown
  moreUrl?: string
}

export function PostList({ posts, isLoading, isError, error, moreUrl }: PostListProps) {
  if (isLoading) {
    return <div className="py-4 text-sm text-gray-500">Loading...</div>
  }

  if (isError) {
    return (
      <div className="py-4 text-sm text-red-600">
        Error loading posts.{' '}
        {error instanceof Error ? error.message : 'Please try again.'}
      </div>
    )
  }

  if (!posts || posts.length === 0) {
    return <div className="py-4 text-sm text-gray-500">No posts yet.</div>
  }

  return (
    <div>
      {posts.map((post, i) => (
        <PostItem key={post.id} post={post} rank={i + 1} />
      ))}
      {moreUrl && (
        <div className="py-2 ml-4">
          <Link to={moreUrl} className="text-sm text-[#ff6600] hover:underline">
            More
          </Link>
        </div>
      )}
    </div>
  )
}

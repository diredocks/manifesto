import type { PostResponse } from '@/api/generated/model'
import { PostItem } from './PostItem'

interface PostListProps {
  posts: PostResponse[] | undefined
  isLoading: boolean
  isError: boolean
  error?: unknown
}

export function PostList({ posts, isLoading, isError, error }: PostListProps) {
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
    </div>
  )
}

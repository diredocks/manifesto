import { useNewPosts } from '@/features/feed/hooks'
import { PostList } from '@/components/PostList'

export function AskPage() {
  const { data, isLoading, isError, error } = useNewPosts()

  return (
    <div>
      <h2 className="text-sm text-gray-500 mb-2">Ask posts — coming soon.</h2>
      <PostList
        posts={data?.data}
        isLoading={isLoading}
        isError={isError}
        error={error}
      />
    </div>
  )
}

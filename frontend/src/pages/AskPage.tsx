import { useAskPosts } from '@/features/feed/hooks'
import { PostList } from '@/components/PostList'

export function AskPage() {
  const { data, isLoading, isError, error } = useAskPosts()

  return (
    <div>
      <PostList
        posts={data?.data}
        isLoading={isLoading}
        isError={isError}
        error={error}
      />
    </div>
  )
}

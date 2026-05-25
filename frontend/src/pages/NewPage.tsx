import { useNewPosts } from '@/features/feed/hooks'
import { PostList } from '@/components/PostList'

export function NewPage() {
  const { data, isLoading, isError, error } = useNewPosts()

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

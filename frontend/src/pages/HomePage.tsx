import { useSearchParams } from 'react-router-dom'
import { useHotPosts } from '@/features/feed/hooks'
import { PostList } from '@/components/PostList'

export function HomePage() {
  const [searchParams] = useSearchParams()
  const p = Number(searchParams.get('p') || '1')
  const apiPage = p - 1

  const { data, isLoading, isError, error } = useHotPosts(apiPage)
  const posts = data?.data
  const hasMore = posts && posts.length >= 20
  const moreUrl = hasMore ? `/?p=${p + 1}` : undefined

  return (
    <div>
      <PostList
        posts={posts}
        isLoading={isLoading}
        isError={isError}
        error={error}
        moreUrl={moreUrl}
      />
    </div>
  )
}

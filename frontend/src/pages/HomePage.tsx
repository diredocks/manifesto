import { useSearchParams } from 'react-router-dom'
import { useGetHot } from '@/api/generated/ranking/ranking'
import { PostList } from '@/components/PostList'

export function HomePage() {
  const [searchParams] = useSearchParams()
  const p = Number(searchParams.get('p') || '1')
  const apiPage = p - 1

  const { data, isLoading, isError, error } = useGetHot({ page: apiPage, size: 20 })
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

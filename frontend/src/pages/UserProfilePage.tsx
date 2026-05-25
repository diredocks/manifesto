import { useSearchParams, useParams } from 'react-router-dom'
import { useUserPosts } from '@/features/profile/hooks'
import { PostList } from '@/components/PostList'

export function UserProfilePage() {
  const { username } = useParams<{ username: string }>()

  if (!username) {
    return <div className="py-4 text-sm text-red-600">Invalid username.</div>
  }

  return (
    <div className="py-2">
      <h1 className="text-base font-bold mb-1">{username}</h1>
      <UserPosts username={username} />
    </div>
  )
}

function UserPosts({ username }: { username: string }) {
  const [searchParams] = useSearchParams()
  const p = Number(searchParams.get('p') || '1')
  const apiPage = p - 1

  const { data, isLoading, isError, error } = useUserPosts(username, apiPage)
  const posts = data?.data?.content
  const hasMore = posts && posts.length >= 20
  const moreUrl = hasMore ? `/user/${username}?p=${p + 1}` : undefined

  return (
    <div className="mt-2">
      <h2 className="text-sm font-bold mb-2 border-t border-border pt-2">Posts</h2>
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

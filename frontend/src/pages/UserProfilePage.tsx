import { useParams } from 'react-router-dom'
import { useUserPosts } from '@/features/profile/hooks'
import { PostItem } from '@/components/PostItem'

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
  const { data, isLoading, isError, error } = useUserPosts(username)

  const posts = data?.data?.content

  return (
    <div className="mt-2">
      <h2 className="text-sm font-bold mb-2 border-t border-border pt-2">Posts</h2>
      {isLoading && <div className="text-sm text-gray-500">Loading posts...</div>}
      {isError && (
        <div className="text-sm text-red-600">
          {error instanceof Error ? error.message : 'Failed to load posts.'}
        </div>
      )}
      {!isLoading && !isError && posts && posts.length > 0 ? (
        <div>
          {posts.map((post) => (
            <PostItem key={post.id} post={post} />
          ))}
        </div>
      ) : (
        !isLoading && <div className="text-sm text-gray-500">No posts yet.</div>
      )}
    </div>
  )
}

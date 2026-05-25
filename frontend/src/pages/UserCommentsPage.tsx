import { useSearchParams, useParams, Link } from 'react-router-dom'
import { useUserComments } from '@/features/profile/hooks'

export function UserCommentsPage() {
  const { username } = useParams<{ username: string }>()

  if (!username) {
    return <div className="py-4 text-sm text-red-600">Invalid username.</div>
  }

  return (
    <div className="py-2">
      <h1 className="text-base font-bold mb-1">{username}&rsquo;s comments</h1>
      <UserComments username={username} />
    </div>
  )
}

function UserComments({ username }: { username: string }) {
  const [searchParams] = useSearchParams()
  const p = Number(searchParams.get('p') || '1')
  const apiPage = p - 1

  const { data, isLoading, isError, error } = useUserComments(username, apiPage)
  const comments = data?.data?.content
  const hasMore = comments && comments.length >= 20
  const moreUrl = hasMore ? `/user/${username}/comments?p=${p + 1}` : undefined

  if (isLoading) {
    return <div className="py-2 text-sm text-[#828282]">loading...</div>
  }

  if (isError) {
    return (
      <div className="py-2 text-sm text-red-600">
        {(error as unknown as Error)?.message || 'Failed to load comments.'}
      </div>
    )
  }

  if (!comments || comments.length === 0) {
    return <div className="py-2 text-sm text-[#828282]">No comments yet.</div>
  }

  return (
    <div>
      {comments.map((comment) => (
        <div key={comment.id} className="py-1.5 border-b border-border last:border-b-0">
          <div className="text-sm">
            <span className="text-[#828282]">
              {comment.score} point{comment.score !== 1 ? 's' : ''} on{' '}
            </span>
            <Link to={`/item/${comment.postId}`} className="hover:underline">
              {comment.postTitle}
            </Link>
          </div>
          <div className="text-sm text-[#333] mt-0.5 line-clamp-2">
            {comment.content}
          </div>
          <div className="text-xs text-[#828282] mt-0.5">
            {new Date(comment.createdAt).toLocaleDateString()}
          </div>
        </div>
      ))}
      {moreUrl && (
        <div className="py-2 text-sm">
          <Link to={moreUrl} className="text-[#000] hover:underline">
            More
          </Link>
        </div>
      )}
    </div>
  )
}

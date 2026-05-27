import { useParams } from 'react-router-dom'
import { useGetPostsByTag } from '@/api/generated/tags/tags'
import { PostItem } from '@/components/PostItem'

export function TagPostsPage() {
  const { tagName } = useParams<{ tagName: string }>()
  const decodedTag = tagName ? decodeURIComponent(tagName) : ''

  const { data, isLoading, isError } = useGetPostsByTag(
    decodedTag,
    { query: { enabled: !!decodedTag } },
  )

  const posts = data?.data

  return (
    <div className="py-2">
      <div className="text-sm mb-2">
        <span className="text-gray-500">Posts tagged </span>
        <span className="font-bold">{decodedTag}</span>
      </div>

      {isLoading && <div className="text-sm text-gray-500 py-2">Loading...</div>}

      {isError && (
        <div className="text-sm text-red-600 py-2">Failed to load posts.</div>
      )}

      {!isLoading && !isError && posts && posts.length === 0 && (
        <div className="text-sm text-gray-500 py-2">
          No posts found with tag &quot;{decodedTag}&quot;.
        </div>
      )}

      {!isLoading && !isError && posts && posts.length > 0 && (
        <div>
          {posts.map((post, index) => (
            <PostItem key={post.id} post={post} rank={index + 1} />
          ))}
        </div>
      )}
    </div>
  )
}

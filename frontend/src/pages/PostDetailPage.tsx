import { useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { useGetPost } from '@/api/generated/posts/posts'
import { useGetComments, useCreateComment } from '@/api/generated/comments/comments'
import { useQueryClient } from '@tanstack/react-query'
import { CommentItem } from '@/components/CommentItem'
import { CommentForm } from '@/components/CommentForm'

function timeAgo(dateStr: string): string {
  const now = Date.now()
  const diff = now - new Date(dateStr).getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 60) return `${minutes}m ago`
  if (hours < 24) return `${hours}h ago`
  if (days < 30) return `${days}d ago`
  return new Date(dateStr).toLocaleDateString()
}

export function PostDetailPage() {
  const { id } = useParams<{ id: string }>()
  const postId = id ? parseInt(id, 10) : 0
  const queryClient = useQueryClient()
  const [replyingTo, setReplyingTo] = useState<number | undefined>()

  const {
    data: postData,
    isLoading: postLoading,
    isError: postError,
  } = useGetPost(postId, { query: { enabled: postId > 0 } })

  const {
    data: commentsData,
    isLoading: commentsLoading,
    isError: commentsError,
  } = useGetComments(postId, { query: { enabled: postId > 0 } })

  const createComment = useCreateComment()

  const post = postData?.data
  const comments = commentsData?.data

  if (!id || postId <= 0) {
    return <div className="py-4 text-sm text-red-600">Invalid post ID.</div>
  }

  if (postLoading) {
    return <div className="py-4 text-sm text-gray-500">Loading...</div>
  }

  if (postError || !post) {
    return <div className="py-4 text-sm text-red-600">Post not found.</div>
  }

  return (
    <div className="py-2">
      <article className="mb-4">
        <h1 className="text-base font-bold mb-1">
          {post.url ? (
            <a
              href={post.url}
              target="_blank"
              rel="noopener noreferrer"
              className="text-link visited:text-link-visited"
            >
              {post.title}
            </a>
          ) : (
            post.title
          )}
        </h1>
        <div className="text-xs text-gray-500 mb-2">
          {post.score} points by{' '}
          <Link to={`/user/${post.authorUsername}`} className="text-gray-500 hover:underline">
            {post.authorUsername}
          </Link>{' '}
          {timeAgo(post.createdAt)}
        </div>
        {post.content && (
          <div className="text-sm mb-3 whitespace-pre-wrap">{post.content}</div>
        )}
        {post.summary && (
          <div className="text-sm mb-3 p-2 bg-gray-100 border border-border">
            <span className="text-xs text-gray-500 font-bold">AI Summary: </span>
            {post.summary}
          </div>
        )}
      </article>

      <section className="border-t border-border pt-2">
        <h2 className="text-sm font-bold mb-3">
          {comments && comments.length > 0 ? `${comments.length} comments` : 'No comments yet'}
        </h2>

        <div className="mb-3">
          <CommentForm
            postId={postId}
            onSubmit={(data) => {
              createComment.mutate(data, {
                onSuccess: () => {
                  queryClient.invalidateQueries({ queryKey: [`/api/v1/posts/${postId}/comments`] })
                },
              })
            }}
            isPending={createComment.isPending}
          />
        </div>

        {commentsLoading && <div className="text-sm text-gray-500 py-2">Loading comments...</div>}

        {commentsError && (
          <div className="text-sm text-red-600 py-2">Failed to load comments.</div>
        )}

        {!commentsLoading &&
          !commentsError &&
          comments?.map((comment) => (
            <div key={comment.id} className="border-t border-border/50">
              <CommentItem comment={comment} />
              {replyingTo === comment.id && (
                <div className="mb-2" style={{ paddingLeft: `${(comment.depth + 1) * 16}px` }}>
                  <CommentForm
                    postId={postId}
                    parentId={comment.id}
                    onSubmit={(data) => {
                      createComment.mutate(data, {
                        onSuccess: () => {
                          queryClient.invalidateQueries({
                            queryKey: [`/api/v1/posts/${postId}/comments`],
                          })
                          setReplyingTo(undefined)
                        },
                      })
                    }}
                    isPending={createComment.isPending}
                    onCancel={() => setReplyingTo(undefined)}
                  />
                </div>
              )}
              {replyingTo !== comment.id && (
                <button
                  onClick={() => setReplyingTo(comment.id)}
                  className="text-xs text-gray-500 hover:underline ml-0 mb-1"
                  style={{ marginLeft: `${comment.depth * 16}px` }}
                >
                  reply
                </button>
              )}
            </div>
          ))}
      </section>
    </div>
  )
}

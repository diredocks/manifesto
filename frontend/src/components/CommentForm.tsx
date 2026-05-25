import { useState } from 'react'
import { useAuthStore } from '@/features/auth/store'
import { Link } from 'react-router-dom'

interface CommentFormProps {
  postId: number
  parentId?: number
  onSubmit: (data: { postId: number; data: { content: string; parentId?: number } }) => void
  isPending: boolean
  onCancel?: () => void
}

export function CommentForm({ postId, parentId, onSubmit, isPending, onCancel }: CommentFormProps) {
  const [content, setContent] = useState('')
  const token = useAuthStore((s) => s.token)

  if (!token) {
    return (
      <p className="text-xs text-gray-500 py-2">
        <Link to="/login" className="text-primary">Login</Link> to comment.
      </p>
    )
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!content.trim()) return
    onSubmit({
      postId,
      data: { content: content.trim(), ...(parentId ? { parentId } : {}) },
    })
    setContent('')
    onCancel?.()
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-2">
      <textarea
        value={content}
        onChange={(e) => setContent(e.target.value)}
        rows={3}
        placeholder="Add a comment..."
        className="border border-border px-2 py-1 text-sm w-full"
      />
      <div className="flex gap-2">
        <button
          type="submit"
          disabled={isPending || !content.trim()}
          className="bg-primary text-white px-3 py-1 text-xs hover:opacity-90 disabled:opacity-50"
        >
          {isPending ? 'Posting...' : 'Post'}
        </button>
        {onCancel && (
          <button type="button" onClick={onCancel} className="text-xs text-gray-500">
            Cancel
          </button>
        )}
      </div>
    </form>
  )
}

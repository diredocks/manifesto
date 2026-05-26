import { useSearchParams, Link } from 'react-router-dom'
import { useNotifications, useMarkNotificationRead } from '@/features/notifications/hooks'
import type { NotificationResponse } from '@/api/generated/model/notificationResponse'

function timeAgo(dateStr: string): string {
  const now = Date.now()
  const then = new Date(dateStr).getTime()
  const diff = now - then
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 60) return `${minutes}m ago`
  if (hours < 24) return `${hours}h ago`
  if (days < 30) return `${days}d ago`
  return new Date(dateStr).toLocaleDateString()
}

function getLinkTarget(n: NotificationResponse): string | null {
  if (n.relatedCommentId && n.relatedPostId) {
    return `/item/${n.relatedPostId}#comment-${n.relatedCommentId}`
  }
  if (n.relatedPostId) {
    return `/item/${n.relatedPostId}`
  }
  return null
}

function NotificationItem({ notification }: { notification: NotificationResponse }) {
  const markAsRead = useMarkNotificationRead()
  const linkTarget = getLinkTarget(notification)

  const handleClick = () => {
    if (!notification.isRead) {
      markAsRead.mutate({ id: notification.id })
    }
  }

  return (
    <div className="flex items-start gap-2 py-1 text-sm">
      {!notification.isRead && <span className="text-primary shrink-0 mt-1.5">&#9679;</span>}
      <div className="min-w-0">
        {linkTarget ? (
          <Link
            to={linkTarget}
            className="text-link no-underline hover:underline"
            onClick={handleClick}
          >
            {notification.content}
          </Link>
        ) : (
          <span className={notification.isRead ? '' : 'font-semibold'}>{notification.content}</span>
        )}
        <div className="text-xs text-gray-500">{timeAgo(notification.createdAt)}</div>
      </div>
    </div>
  )
}

export function NotificationsPage() {
  const [searchParams] = useSearchParams()
  const p = Number(searchParams.get('p') || '1')
  const apiPage = p - 1

  const { data, isLoading, isError, error } = useNotifications(apiPage)
  const notifications = data?.data?.content
  const hasMore = notifications && notifications.length >= 20
  const moreUrl = hasMore ? `/notifications?p=${p + 1}` : undefined

  if (isLoading) return <div className="text-sm text-gray-500 py-2">Loading...</div>
  if (isError) return <div className="text-sm text-red-500 py-2">Error: {String(error)}</div>
  if (!notifications || notifications.length === 0) {
    return <div className="text-sm text-gray-500 py-2">No notifications yet.</div>
  }

  return (
    <div>
      {notifications.map((n) => (
        <NotificationItem key={n.id} notification={n} />
      ))}
      {moreUrl && (
        <Link to={moreUrl} className="text-sm text-link no-underline hover:underline">
          More
        </Link>
      )}
    </div>
  )
}

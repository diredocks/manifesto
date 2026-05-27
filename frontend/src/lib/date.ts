import { Temporal } from '@js-temporal/polyfill'

export function timeAgo(dateStr: string): string {
  const now = Temporal.Now.instant().epochMilliseconds
  const then = Temporal.Instant.from(dateStr).epochMilliseconds
  const diff = now - then
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 60) return `${minutes}m ago`
  if (hours < 24) return `${hours}h ago`
  if (days < 30) return `${days}d ago`
  return formatDate(dateStr)
}

export function formatDate(dateStr: string): string {
  return Temporal.Instant.from(dateStr)
    .toZonedDateTimeISO(Temporal.Now.timeZoneId())
    .toPlainDate()
    .toLocaleString()
}

export function formatDateTime(dateStr: string): string {
  return Temporal.Instant.from(dateStr)
    .toZonedDateTimeISO(Temporal.Now.timeZoneId())
    .toLocaleString()
}

export function isFuture(dateStr: string): boolean {
  return Temporal.Instant.from(dateStr).epochMilliseconds > Temporal.Now.instant().epochMilliseconds
}

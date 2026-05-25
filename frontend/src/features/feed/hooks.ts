import { useGetHot, useGetNew, useGetTop } from '@/api/generated/ranking/ranking'

export function useHotPosts(page = 0, size = 20) {
  return useGetHot({ page, size })
}

export function useNewPosts(page = 0, size = 20) {
  return useGetNew({ page, size })
}

export function useAskPosts(page = 0, size = 20) {
  return useGetNew({ page, size, type: 'ASK' })
}

export function useTopPosts(page = 0, size = 20) {
  return useGetTop({ page, size })
}

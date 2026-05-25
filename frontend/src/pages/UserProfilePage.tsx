import { useParams, Link } from 'react-router-dom'
import { useUserProfile } from '@/features/profile/hooks'

export function UserProfilePage() {
  const { username } = useParams<{ username: string }>()

  if (!username) {
    return <div className="py-4 text-sm text-red-600">Invalid username.</div>
  }

  return (
    <div className="py-2">
      <UserInfo username={username} />
      <div className="mt-3 flex gap-4 text-sm">
        <Link to={`/user/${username}/posts`} className="text-[#000] hover:underline">posts</Link>
        <Link to={`/user/${username}/comments`} className="text-[#000] hover:underline">comments</Link>
      </div>
    </div>
  )
}

function UserInfo({ username }: { username: string }) {
  const { data, isLoading, isError } = useUserProfile(username)

  if (isLoading) {
    return <div className="text-sm text-[#828282]">loading...</div>
  }

  if (isError || !data?.data) {
    return <div className="text-sm text-red-600">User not found.</div>
  }

  const profile = data.data
  const joined = new Date(profile.createdAt).toLocaleDateString()

  return (
    <div>
      <h1 className="text-base font-bold">{profile.username}</h1>
      <div className="text-sm text-[#828282] mt-1">
        <span>karma: {profile.karma}</span>
        <span className="mx-2">|</span>
        <span>joined: {joined}</span>
      </div>
    </div>
  )
}

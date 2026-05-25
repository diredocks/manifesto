import { Routes, Route } from 'react-router-dom'
import { Layout } from '@/components/Layout'
import { ProtectedRoute } from '@/components/ProtectedRoute'
import { HomePage } from '@/pages/HomePage'
import { NewPage } from '@/pages/NewPage'
import { AskPage } from '@/pages/AskPage'
import { SubmitPage } from '@/pages/SubmitPage'
import { LoginPage } from '@/pages/LoginPage'
import { RegisterPage } from '@/pages/RegisterPage'
import { PostDetailPage } from '@/pages/PostDetailPage'
import { UserProfilePage } from '@/pages/UserProfilePage'
import { UserPostsPage } from '@/pages/UserPostsPage'
import { UserCommentsPage } from '@/pages/UserCommentsPage'
import { ModPage } from '@/pages/ModPage'
import { AdminPage } from '@/pages/AdminPage'
import { NotFoundPage } from '@/pages/NotFoundPage'

export function AppRouter() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<HomePage />} />
        <Route path="new" element={<NewPage />} />
        <Route path="ask" element={<AskPage />} />
        <Route path="item/:id" element={<PostDetailPage />} />
        <Route path="user/:username" element={<UserProfilePage />} />
        <Route path="user/:username/posts" element={<UserPostsPage />} />
        <Route path="user/:username/comments" element={<UserCommentsPage />} />
        <Route path="submit" element={<ProtectedRoute><SubmitPage /></ProtectedRoute>} />
        <Route path="mod" element={<ProtectedRoute><ModPage /></ProtectedRoute>} />
        <Route path="admin" element={<ProtectedRoute><AdminPage /></ProtectedRoute>} />
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}

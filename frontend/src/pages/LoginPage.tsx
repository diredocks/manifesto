import { useNavigate, Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useLogin } from '@/features/auth/hooks'

const schema = z.object({
  username: z.string().min(1, 'Username is required'),
  password: z.string().min(1, 'Password is required'),
})

type FormData = z.infer<typeof schema>

export function LoginPage() {
  const navigate = useNavigate()
  const login = useLogin()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
  })

  const onSubmit = (data: FormData) => {
    login.mutate(
      { data },
      {
        onSuccess: () => navigate('/'),
      },
    )
  }

  return (
    <div className="mx-auto max-w-sm py-8">
      <h1 className="text-base font-bold mb-4">Login</h1>
      <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-3">
        <div>
          <input
            {...register('username')}
            placeholder="Username"
            className="w-full border border-border px-2 py-1 text-sm"
            autoComplete="username"
          />
          {errors.username && (
            <p className="text-xs text-red-600 mt-1">{errors.username.message}</p>
          )}
        </div>
        <div>
          <input
            {...register('password')}
            type="password"
            placeholder="Password"
            className="w-full border border-border px-2 py-1 text-sm"
            autoComplete="current-password"
          />
          {errors.password && (
            <p className="text-xs text-red-600 mt-1">{errors.password.message}</p>
          )}
        </div>
        {login.isError && (
          <p className="text-xs text-red-600">
            {(login.error as { response?: { data?: { message?: string } } })?.response?.data?.message ||
              'Login failed'}
          </p>
        )}
        <button
          type="submit"
          disabled={login.isPending}
          className="bg-primary text-white px-3 py-1 text-sm hover:opacity-90 disabled:opacity-50"
        >
          {login.isPending ? 'Logging in...' : 'Login'}
        </button>
      </form>
      <p className="text-xs mt-3">
        Don&apos;t have an account?{' '}
        <Link to="/register" className="text-primary">
          Register
        </Link>
      </p>
    </div>
  )
}

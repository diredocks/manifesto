import { useNavigate, Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useRegister } from '@/features/auth/hooks'

const schema = z.object({
  username: z
    .string()
    .min(3, 'Username must be at least 3 characters')
    .max(50, 'Username must be at most 50 characters'),
  email: z.string().email('Invalid email address'),
  password: z
    .string()
    .min(6, 'Password must be at least 6 characters')
    .max(100, 'Password must be at most 100 characters'),
})

type FormData = z.infer<typeof schema>

export function RegisterPage() {
  const navigate = useNavigate()
  const registerMutation = useRegister()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
  })

  const onSubmit = (data: FormData) => {
    registerMutation.mutate(
      { data },
      {
        onSuccess: () => navigate('/'),
      },
    )
  }

  return (
    <div className="mx-auto max-w-sm py-8">
      <h1 className="text-base font-bold mb-4">Register</h1>
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
            {...register('email')}
            type="email"
            placeholder="Email"
            className="w-full border border-border px-2 py-1 text-sm"
            autoComplete="email"
          />
          {errors.email && (
            <p className="text-xs text-red-600 mt-1">{errors.email.message}</p>
          )}
        </div>
        <div>
          <input
            {...register('password')}
            type="password"
            placeholder="Password"
            className="w-full border border-border px-2 py-1 text-sm"
            autoComplete="new-password"
          />
          {errors.password && (
            <p className="text-xs text-red-600 mt-1">{errors.password.message}</p>
          )}
        </div>
        {registerMutation.isError && (
          <p className="text-xs text-red-600">
            {(registerMutation.error as { response?: { data?: { message?: string } } })?.response?.data
              ?.message || 'Registration failed'}
          </p>
        )}
        <button
          type="submit"
          disabled={registerMutation.isPending}
          className="bg-primary text-white px-3 py-1 text-sm hover:opacity-90 disabled:opacity-50"
        >
          {registerMutation.isPending ? 'Creating account...' : 'Register'}
        </button>
      </form>
      <p className="text-xs mt-3">
        Already have an account?{' '}
        <Link to="/login" className="text-primary no-underline hover:underline">
          Login
        </Link>
      </p>
    </div>
  )
}

import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { CreatePostRequestType } from '@/api/generated/model'
import { useSubmitPost } from '@/features/submit/hooks'

const schema = z.discriminatedUnion('type', [
  z.object({
    type: z.literal(CreatePostRequestType.LINK),
    title: z.string().min(1, 'Title is required').max(300, 'Title too long'),
    url: z.string().url('Valid URL is required for link posts'),
    content: z.string().optional(),
  }),
  z.object({
    type: z.literal(CreatePostRequestType.ASK),
    title: z.string().min(1, 'Title is required').max(300, 'Title too long'),
    content: z.string().min(1, 'Content is required for text posts'),
    url: z.string().optional(),
  }),
])

type FormData = z.infer<typeof schema>

export function SubmitPage() {
  const mutation = useSubmitPost()
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { type: CreatePostRequestType.LINK },
  })

  const postType = watch('type')

  const onSubmit = (data: FormData) => {
    const payload: { title: string; type: typeof CreatePostRequestType.LINK | typeof CreatePostRequestType.ASK; url?: string; content?: string } = {
      title: data.title,
      type: data.type,
    }
    if (data.type === CreatePostRequestType.LINK) {
      payload.url = data.url
    } else {
      payload.content = data.content
    }
    mutation.mutate({ data: payload })
  }

  return (
    <div className="mx-auto max-w-lg py-4">
      <h1 className="text-base font-bold mb-4">Submit Post</h1>
      <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-3">
        <div>
          <select
            {...register('type')}
            className="border border-border px-2 py-1 text-sm bg-white"
          >
            <option value={CreatePostRequestType.LINK}>Link</option>
            <option value={CreatePostRequestType.ASK}>Ask / Text</option>
          </select>
        </div>
        <div>
          <input
            {...register('title')}
            placeholder="Title"
            className="w-full border border-border px-2 py-1 text-sm"
          />
          {errors.title && (
            <p className="text-xs text-red-600 mt-1">{errors.title.message}</p>
          )}
        </div>
        {postType === CreatePostRequestType.LINK ? (
          <div>
            <input
              {...register('url')}
              placeholder="https://example.com"
              className="w-full border border-border px-2 py-1 text-sm"
            />
            {errors.url && (
              <p className="text-xs text-red-600 mt-1">{errors.url.message}</p>
            )}
          </div>
        ) : (
          <div>
            <textarea
              {...register('content')}
              placeholder="Text (optional)"
              rows={4}
              className="w-full border border-border px-2 py-1 text-sm"
            />
            {'content' in errors && errors.content && (
              <p className="text-xs text-red-600 mt-1">{errors.content?.message}</p>
            )}
          </div>
        )}
        {mutation.isError && (
          <p className="text-xs text-red-600">Failed to submit post. Please try again.</p>
        )}
        <button
          type="submit"
          disabled={mutation.isPending}
          className="bg-primary text-white px-3 py-1 text-sm hover:opacity-90 disabled:opacity-50 self-start"
        >
          {mutation.isPending ? 'Submitting...' : 'Submit'}
        </button>
      </form>
    </div>
  )
}

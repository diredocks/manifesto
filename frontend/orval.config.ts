import { defineConfig } from 'orval'

export default defineConfig({
  manifesto: {
    input: {
      target: 'openapi.json',
    },
    output: {
      mode: 'tags-split',
      target: 'src/api/generated',
      schemas: 'src/api/generated/model',
      client: 'react-query',
      override: {
        mutator: {
          path: 'src/api/client/axios-client.ts',
          name: 'customInstance',
        },
      },
      clean: true,
    },
  },
})

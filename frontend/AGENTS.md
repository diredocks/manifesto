# AGENTS.md

This file provides guidance to agents when working in the frontend repository.

---

## Tech Stack (Strict)

The frontend MUST use:

### Core

* React 19
* TypeScript (strict mode)
* Vite
* pnpm (never npm/yarn)
* TailwindCSS v4

---

## API Layer (Strict React Query Model)

### Required Stack

* Orval (OpenAPI client generation)
* TanStack React Query (server state layer)

---

### Key Principle

React Query is the ONLY interface for server state.

Never use:

```ts
axios.get(...)
fetch(...)
```

Never bypass React Query hooks when an endpoint exists.

---

### Data Flow Model

UI → React Query Hook → Orval Client → Backend

UI must never call API clients directly.

---

### Generated API Client

Location:

```
src/api/generated/
```

* Never modify generated files
* Regenerate after backend changes:

```bash
pnpm orval
```

---

### Custom Client Layer

Location:

```
src/api/client/
```

Responsibilities:

* interceptors
* auth token injection
* refresh token flow
* error normalization

No business logic allowed.

Never manually set Authorization headers in UI.

---

### React Query Rules

All API calls must be wrapped in hooks:

Examples:

* useGetHotPosts()
* useGetPostById()
* useCreateComment()
* useLogin()

Rules:

* Server state must NOT be cached (always fetch fresh data)
* Mutations must invalidate/update queries explicitly
* No duplicate fetch logic in components
* No ad-hoc request logic outside hooks

---

### Query Key Convention

Structured keys only:

```ts
['posts', 'hot']
['posts', 'new']
['post', id]
['comments', 'post', id]
['user', username]
```

No string-only or ad-hoc keys.

---

### Error Handling (Unified Model)

Backend response:

```ts
ApiResponse<T>
```

Rules:

* success → `response.data.data`
* error → `response.data.message`
* UI-safe error objects only
* never expose raw backend payload

---

### Auth Integration

JWT flow:

* access token in memory + localStorage fallback

React Query rules:

* auth change invalidates:

```txt
['me']
['user', '*']
```

* logout:

```ts
queryClient.clear()
```

---

## Routing & State

* React Router v7
* Zustand only for UI/client state (NOT server state)
* React Hook Form + Zod for forms

Zustand must never store API data.

---

## UI Rules

* TailwindCSS only
* clsx + tailwind-merge for class composition

No UI frameworks:

* MUI
* Ant Design
* Chakra
* shadcn/ui

---

## Design Philosophy (Hacker News Style)

* dense layout
* text-first UI
* minimal spacing
* performance > aesthetics

### Visual Rules

* max width ~1100px
* no cards
* no shadows
* rounded-none only

Typography:

* text-sm body
* text-xs metadata
* text-base titles

Colors:

* primary #ff6600
* background #f6f6ef
* border #d9d9d9
* text #222
* links #000 (visited distinct)

---

## Folder Structure

```
src/
├── api/
│   ├── generated/
│   └── client/
├── app/
│   ├── router/
│   ├── providers/
│   └── store/
├── features/
│   ├── auth/
│   ├── feed/
│   ├── submit/
│   ├── comment/
│   ├── vote/
│   ├── profile/
│   └── moderation/
├── components/
├── hooks/
├── lib/
├── pages/
├── styles/
└── types/
```

Feature-first architecture required.

---

## Authentication Rules

* JWT only
  * attach token
  * refresh token
  * 401 redirect

Never handle auth headers in UI or hooks.

---

## Routing

Pages:

```
/
 /new
 /ask
 /item/:id
 /submit
 /login
 /register
 /user/:username
 /mod
```

Protected:

* /submit
* /mod

---

## Performance Rules

* lazy load routes
* avoid premature memoization

---

## Error States (Mandatory)

Every screen must handle:

* loading
* error
* empty

Never assume success-only UI.

---

## Build & Commands

```bash
pnpm install
pnpm dev
pnpm lint
pnpm tsc --noEmit
pnpm test
pnpm build
pnpm orval
```

Pre-commit:

```bash
pnpm lint
pnpm tsc --noEmit
pnpm build
```

---

## Definition of Done (Global)

A feature is complete only if:

* UI implemented
* React Query integrated
* loading state
* error state
* empty state
* `pnpm lint` passes
* `pnpm tsc --noEmit` passes
* `pnpm build` passes
* tests pass (if applicable)

---

## Agent Workflow

1. Inspect Orval-generated hooks
2. Implement feature UI
3. Use React Query only
4. Ensure 3 UI states exist
5. Validate types strictly
6. Run:

```bash
pnpm lint
pnpm tsc --noEmit
pnpm build
```

7. Fix issues before proceeding
8. Commit in small units

---

## Commit Discipline

* one feature per commit
* no mixed refactor + feature

Format:

```
feat(feed): implement hot ranking page
fix(comment): resolve nesting recursion bug
refactor(api): standardize query keys
```

---

## Development Plan

### P0 — Core MVP

* auth (login/register/JWT)
* feed (hot/new/ask)
* submit post
* post detail + comments
* voting system
* user profile
* moderation basics

Success:

* full functional Hacker News clone

---

### P1 — Community

* pagination / infinite scroll
* comment UX improvements
* search
* notifications
* moderator dashboard
* toast system
* skeleton loading

---

### P2 — Polish

* dark mode (Zustand optional)
* keyboard shortcuts
* optimistic updates
* admin panel
* mobile improvements

---

## Global Definition of Done

A feature is only complete if:

* UI exists
* API wired via React Query
* loading/error/empty handled
* type-safe
* build passes
* tests pass (if applicable)

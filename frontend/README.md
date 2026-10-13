# zorg-app frontend

React frontend for the zorg-app web API. Built with Vite, Tailwind CSS v4 and
[shadcn/ui](https://ui.shadcn.com). The API itself is described in the
[main README](../README.md#web-app).

## Running

```bash
npm install            # first time only
npm run dev            # backend + frontend (JSON mode)
npm run dev:firebase   # backend + frontend against the local Firebase emulators
```

Both start the Spring Boot backend and the Vite dev server through
[../scripts/dev.sh](../scripts/dev.sh); Ctrl+C stops everything. They need bash.

| Script | What it does |
| --- | --- |
| `npm run dev` | backend (`./gradlew bootRun`) and Vite together, JSON mode |
| `npm run dev:firebase` | starts the Firebase emulators, seeds them, then backend and Vite |
| `npm run dev:web` | only Vite, for when the backend is already running |
| `npm run build` | production build into `dist/` |
| `npm run lint` | ESLint |
| `npm run preview` | serves the `dist/` build locally |

## How it fits together

- **API calls** go through [src/api.js](src/api.js). The dev server proxies
  `/api` to the backend on `127.0.0.1:8080` ([vite.config.js](vite.config.js)),
  so the browser sees one origin and sends the session cookie. `api.js` adds the
  `X-Zorg-Client: web` header to requests that change data, and throws an
  `Error` with the problem's `detail` when a request fails.
- **Imports** can use `@/` for `src/` (e.g. `@/components/ui/button`), set up in
  [jsconfig.json](jsconfig.json) and `vite.config.js`.
- **Styling** is Tailwind utility classes. The theme (colors, radius, fonts, dark
  mode) is defined as CSS variables in [src/index.css](src/index.css). Dark mode
  turns on when the `dark` class is on `<html>`.
- **Components** from shadcn/ui are added with `npx shadcn@latest add <name>`
  and land in `src/components/ui/`. They are generated code you own: edit them
  freely, but `add` with `--overwrite` replaces them. ESLint's fast-refresh rule
  is off for that folder because shadcn components also export their variants.

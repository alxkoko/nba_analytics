# NBA Player Analytics — Frontend (Vite + React)

## Prerequisites

- Node.js 18+
- Backend running on **http://localhost:8080** (see `backend/README.md`)

## Setup

```bash
cd frontend
npm install
```

## Run

```bash
npm run dev
```

App: **http://localhost:5173**

Vite proxies `/api` to the backend, so API calls from the browser go to the same origin and hit Spring Boot.

## Build

```bash
npm run build
npm run preview
```

## Features

- **Home:** Search players by name; click a player to open detail.
- **Player detail:** Season selector, season stats (PPG, RPG, etc.), over/under (threshold + last N), points chart (last N games), game log table.

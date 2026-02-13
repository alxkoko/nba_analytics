# Deploying NBA Player Analytics

You have three main ways to put the app online. Each needs: **PostgreSQL**, **Spring Boot** (API), and **React** (static frontend). Python ingestion can stay on your machine (run locally and point `DATABASE_URL` at the hosted DB) or run on the server (e.g. cron).

---

## Option 1: Single VPS (e.g. DigitalOcean, Linode, Hetzner)

**Idea:** One Linux server runs PostgreSQL, the Spring Boot JAR, and Nginx. Nginx serves the built React app and proxies `/api` to Spring Boot.

| Pros | Cons |
|------|------|
| One bill, full control | You manage OS, DB, backups, SSL |
| No cold starts | Need to set up Nginx, systemd, firewall |

### Steps (high level)

1. **Create a VPS** (Ubuntu 22.04, 1 GB RAM minimum; 2 GB better). Note the server IP.

2. **Install and configure PostgreSQL**
   - Install: `sudo apt update && sudo apt install postgresql postgresql-contrib`
   - Create DB and user: `sudo -u postgres psql` then `CREATE DATABASE nba_stats; CREATE USER nba_app WITH PASSWORD 'your_secure_password'; GRANT ALL PRIVILEGES ON DATABASE nba_stats TO nba_app;`
   - Apply schema: `psql -h localhost -U nba_app -d nba_stats -f database/schema.sql` (from your repo on the server).

3. **Install Java 17** (e.g. `sudo apt install openjdk-17-jdk`).

4. **Build and run the backend**
   - On your PC: `cd backend && mvn clean package -DskipTests`
   - Copy `backend/target/nba-stats-api-0.0.1-SNAPSHOT.jar` to the server (e.g. `/opt/nba-stats/app.jar`).
   - Run with: `java -jar app.jar` using env vars for DB (see below). For production, run as a systemd service so it restarts on reboot.

5. **Configure Nginx**
   - Install: `sudo apt install nginx`
   - Create a site config (e.g. `/etc/nginx/sites-available/nba-stats`) that:
     - Serves static files from the built frontend (e.g. `root /var/www/nba-stats/frontend/dist;` and `try_files $uri $uri/ /index.html;` for SPA).
     - Proxies `location /api/ { proxy_pass http://127.0.0.1:8080; proxy_http_version 1.1; proxy_set_header Host $host; proxy_set_header X-Real-IP $remote_addr; }`
   - Enable: `sudo ln -s /etc/nginx/sites-available/nba-stats /etc/nginx/sites-enabled` and `sudo nginx -t && sudo systemctl reload nginx`.

6. **Build and upload the frontend**
   - On your PC: `cd frontend && npm run build`. The build assumes the app is served from the same origin as the API (Nginx serves both), so no `VITE_API_URL` needed if `/api` is on the same domain.
   - Upload the contents of `frontend/dist` to `/var/www/nba-stats/frontend/dist` (or the path you set in Nginx).

7. **SSL (HTTPS)**  
   Use Let’s Encrypt: `sudo apt install certbot python3-certbot-nginx && sudo certbot --nginx -d yourdomain.com`.

8. **Backend env vars on the server**  
   When running the JAR, set at least:  
   `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/nba_stats`  
   `SPRING_DATASOURCE_USERNAME=nba_app`  
   `SPRING_DATASOURCE_PASSWORD=your_secure_password`

9. **Ingestion**  
   Keep running `python ingest.py` from your machine (or a cron job), with `DATABASE_URL=postgresql://nba_app:password@your-server-ip:5432/nba_stats`. Open port 5432 only if you’re comfortable (or use SSH tunnel); otherwise run ingestion from the server or a job that has network access to the DB.

---

## Option 2: PaaS (e.g. Vercel + Railway)

**Idea:** Frontend on Vercel, backend + PostgreSQL on Railway. No server to maintain.

| Pros | Cons |
|------|------|
| No server admin, auto SSL | Multiple services, free tiers can have cold starts |
| Easy Git-based deploys | DB and backend may cost a few $/month after trial |

### Steps (high level)

1. **Database on Railway**
   - Sign up at [railway.app](https://railway.app).
   - New Project → Add PostgreSQL. Copy the `DATABASE_URL` (or `PGHOST`, `PGUSER`, `PGPASSWORD`, `PGDATABASE`).
   - Connect (e.g. with `psql` or a GUI) and run `database/schema.sql`.

2. **Backend on Railway**
   - In the same project (or new one): New Service → deploy from GitHub (your repo).
   - Root directory: `backend` (or repo root and set build command to `cd backend && mvn clean package -DskipTests` and start command to `java -jar target/nba-stats-api-0.0.1-SNAPSHOT.jar`).
   - Add env vars: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` (from the Railway Postgres service).
   - Railway will assign a URL like `https://your-app.up.railway.app`. This is your **API base URL**.

3. **Frontend on Vercel**
   - Sign up at [vercel.com](https://vercel.com), import your repo.
   - Root directory: `frontend`. Build command: `npm run build`. Output: `dist`.
   - Add env var so the frontend calls the deployed API: `VITE_API_URL=https://your-app.up.railway.app` (no trailing slash). You must use this in the frontend for fetch (see below).
   - Deploy. Vercel gives you a URL like `https://nba-stats.vercel.app`.

4. **CORS**  
   Your Spring Boot app has `@CrossOrigin(origins = "*")`. For production you can restrict to your Vercel domain, e.g. `@CrossOrigin(origins = "https://nba-stats.vercel.app")`.

5. **Frontend API base URL**  
   In production the frontend must call the Railway URL, not relative `/api`. In `frontend/src/api/client.js` use:
   - `const API_BASE = import.meta.env.VITE_API_URL ? import.meta.env.VITE_API_URL + '/api' : '/api'`
   so that with `VITE_API_URL` set (Vercel) you use the Railway API; locally you keep using `/api` (Vite proxy).

6. **Ingestion**  
   Run locally (or on a small cron host) with `DATABASE_URL` set to Railway’s Postgres URL so new data is ingested into the same DB.

---

## Option 3: Docker (single server or PaaS)

**Idea:** Run PostgreSQL, Spring Boot, and Nginx (serving the built frontend) via Docker Compose. Deploy the stack to a VPS or to a platform that runs Docker (e.g. Railway, Fly.io).

- Define `Dockerfile` for the backend (build JAR, run `java -jar`).
- Build frontend locally or in CI; put `dist` into an Nginx image or volume.
- `docker-compose.yml`: postgres, backend, nginx. Nginx proxies `/api` to backend and serves `dist`.
- Same checklist as Option 1 for DB, env vars, and SSL (or use the platform’s HTTPS).

---

## Checklist (any option)

- [ ] **PostgreSQL** created and `database/schema.sql` applied.
- [ ] **Backend** runs with correct `SPRING_DATASOURCE_*` (or `DATABASE_URL` parsed into those).
- [ ] **Frontend** build uses the right API base URL in production (e.g. `VITE_API_URL` for PaaS).
- [ ] **CORS** allows your frontend origin (or `*` for quick testing).
- [ ] **Ingestion** can reach the DB (same network or secure connection); run manually or via cron.
- [ ] **HTTPS** enabled (Nginx + Certbot on VPS, or automatic on Vercel/Railway).

---

## Quick reference: frontend API base

For **same-origin** (Option 1, Nginx serves frontend and proxies `/api`): no change; keep `const API_BASE = '/api'`.

For **split hosting** (Option 2, frontend on Vercel, API on Railway):

```js
const API_BASE = import.meta.env.VITE_API_URL
  ? `${import.meta.env.VITE_API_URL}/api`
  : '/api'
```

Then set `VITE_API_URL=https://your-backend.up.railway.app` in Vercel (no trailing slash).

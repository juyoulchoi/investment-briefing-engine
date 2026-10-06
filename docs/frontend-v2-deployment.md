# V2 frontend deployment

The maintained source directory is `frontend-next`, exposed at http://localhost:4175.
The Compose project is `investment-v2`, the service is `frontend`, and the
container is `investment-v2-frontend`, grouped with the V2 backend and database.
`../investment-briefing-engine-v2/docker-compose.yml` connects it to `investment-v2-backend:8080`
through the existing `investment-v2-network`. The V2 backend and database must
be running before starting this frontend.

The legacy root `frontend` directory and Main Compose frontend service have
been removed. The former Vite frontend is replaced by this Next.js frontend.
Main backend/database and the shared KOFIA collector remain separate services.

Build image `investment-frontend` before deploying from the Main repository:

```powershell
docker compose -f ../investment-briefing-engine-v2/docker-compose.yml build frontend
docker compose -f ../investment-briefing-engine-v2/docker-compose.yml up -d --no-deps --force-recreate frontend
```

For local development, set `BACKEND_URL=http://127.0.0.1:8082` before starting
Next.js. Existing fallback URLs in the source still target Main when this
environment variable is omitted.

# V2 frontend deployment

The maintained frontend is `frontend-next`, exposed at http://localhost:4175.
`docker-compose.frontend-next.yml` connects it to `investment-v2-backend:8080`
through the existing `investment-v2-network`. The V2 backend and database must
be running before starting this frontend.

The legacy root `frontend` directory and Main Compose frontend service have
been removed. The legacy `investment-v2-frontend` container is stopped.
Main backend/database and the shared KOFIA collector remain separate services.

Apply connection configuration using the existing frontend image:

```powershell
docker compose -f docker-compose.frontend-next.yml up -d --no-deps --force-recreate frontend-next
```

For local development, set `BACKEND_URL=http://127.0.0.1:8082` before starting
Next.js. Existing fallback URLs in the source still target Main when this
environment variable is omitted.

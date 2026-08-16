# RAG MCP Assistant

AI assistant for SMEs with Retrieval-Augmented Generation (RAG). The chatbot answers **only** from the documents you upload — if the answer isn't in the semantic search results, it replies "I don't know."

## Features

- **RAG chat**: ask questions, answers are grounded in your documents (anti-hallucination)
- **Document upload**: PDF and TXT files, indexed with semantic embeddings
- **Semantic search**: pgvector (HNSW, cosine distance)
- **Admin page**: list and delete documents
- **Multilingual**: French, English, German, Arabic (fr/en/de/ar)
- **MCP server**: exposed over Streamable HTTP

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 4.1, Java 21, Spring AI, OpenRouter (`gpt-4o-mini`) |
| Database | PostgreSQL 16 + pgvector (Docker only) |
| Frontend | Angular 22 (standalone) + Tailwind CSS 4, ngx-translate |
| MCP | Spring AI MCP server (Streamable HTTP) |

## Quick start

```bash
cp .env.example .env    # then fill in OPENROUTER_API_KEY
docker compose up -d --build
```

- App: http://localhost:4200
- Backend: http://localhost:8080

One command starts everything: PostgreSQL, backend, and frontend.

## Configuration

Set these values in `.env`:

| Variable | Description |
|---|---|
| `OPENROUTER_API_KEY` | OpenRouter API key (LLM access) |
| `MAILTRAP_USERNAME` / `MAILTRAP_PASSWORD` | Mailtrap SMTP credentials |
| `ESCALATION_RECIPIENT_EMAIL` | Escalation notification recipient |

## Translations

The app is translated in fr, en, de, ar. `fr.json` is the source of truth:

1. Add the key to `frontend/src/assets/i18n/fr.json`
2. Use it in templates: `{{ 'key' | translate }}`
3. Regenerate the other languages (via OpenRouter):

```bash
cd frontend
npm run i18n:generate
```

Fallback language: French.

## MCP server

The backend exposes an MCP server over Streamable HTTP at `/mcp`, enabled automatically with the backend.

## Deployment

- **Backend**: Render (via Docker)
- **Database**: Neon (PostgreSQL + pgvector)
- **Frontend**: Vercel (rewrites `/api/*` to the backend)

Note: with an empty database the chat answers "I don't know." Upload documents through the admin page first.

## Project structure

```
backend/   Spring Boot API (chat, document, knowledge, llm, mcp, notification)
frontend/  Angular SPA (chat + admin)
```

### API

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/chat` | Send a chat message |
| GET | `/api/chat/{sessionId}/history` | Chat history for a session |
| POST | `/api/documents/upload` | Upload a document |
| GET | `/api/documents` | List documents |
| DELETE | `/api/documents/{id}` | Delete a document |
| GET | `/api/search` | Semantic search |
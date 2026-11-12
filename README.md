# Notes AI

An AI-powered notes application built with **Java 17, Spring Boot 3, Spring Security (JWT), Spring Data JPA/Hibernate and MySQL**, with LLM features powered by the **Claude API**.

## Features
- User registration and login with **JWT authentication** (BCrypt-hashed passwords)
- Full **CRUD REST APIs** for notes, with server-side ownership checks (users only see their own notes)
- **AI features**
  - Summarize a note
  - Auto-generate tags for a note
  - Ask natural-language questions across all your notes
- Lightweight web UI served by the same Spring Boot app (`src/main/resources/static/index.html`)

## Tech stack
Java 17 · Spring Boot 3.3 · Spring Web · Spring Security · Spring Data JPA (Hibernate) · MySQL (H2 for quick demo) · JJWT · Maven · Claude Messages API

## Project structure
```
src/main/java/com/notesai
├── NotesAiApplication.java
├── controller   AuthController, NoteController, AiController
├── service      AiService (Claude API client)
├── security     SecurityConfig, JwtService, JwtAuthFilter
├── model        User, Note (JPA entities)
├── repository   UserRepository, NoteRepository
└── dto          request/response records with validation
```

## Run it

Prerequisites: JDK 17+, Maven 3.9+.

### Option A: quick start with in-memory H2 (no database setup)
```bash
export ANTHROPIC_API_KEY=your_key_here      # Windows PowerShell: $env:ANTHROPIC_API_KEY="your_key_here"
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

### Option B: MySQL
1. Make sure MySQL is running. The `notesdb` database is created automatically.
2. Set your credentials and API key:
```bash
export DB_USER=root
export DB_PASSWORD=your_mysql_password
export ANTHROPIC_API_KEY=your_key_here
export JWT_SECRET=some-long-random-string-of-at-least-32-characters
mvn spring-boot:run
```

Open <http://localhost:8080>, register an account and add some notes.

Get an API key at <https://console.anthropic.com>. Without a key the app still works; AI buttons show a clear "AI is not configured" message.

> Never commit your API key or DB password. They are read from environment variables, and `.env` is in `.gitignore`.

## API

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | no | Create account, returns JWT |
| POST | `/api/auth/login` | no | Log in, returns JWT |
| GET | `/api/notes` | yes | List my notes |
| POST | `/api/notes` | yes | Create a note |
| PUT | `/api/notes/{id}` | yes | Update a note |
| DELETE | `/api/notes/{id}` | yes | Delete a note |
| POST | `/api/ai/notes/{id}/summarize` | yes | AI summary of a note |
| POST | `/api/ai/notes/{id}/tags` | yes | AI tags (saved on the note) |
| POST | `/api/ai/ask` | yes | Ask a question over all my notes |

Send the token as `Authorization: Bearer <token>`.

Example:
```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"secret123"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")

curl -X POST localhost:8080/api/notes -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"Hibernate","content":"Lazy loading fetches associations on first access..."}'

curl -X POST localhost:8080/api/ai/ask -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"question":"What did I write about Hibernate?"}'
```

## Design notes
- **Service layer for AI**: `AiService` isolates the LLM call so it is easy to mock and swap.
- **Secrets via environment variables**: API key, DB password and JWT secret are never hard-coded.
- **Authorization before AI**: every AI endpoint loads the note by `id + owner`, so one user's data is never sent on behalf of another.
- **Bounded prompts**: the "ask" feature caps context at 20,000 characters to control cost and context limits. At larger scale this would become retrieval (embeddings or full-text search) before calling the model.
- **XSS-safe UI**: the front end renders user content with `textContent`, never `innerHTML`.

## Possible next steps
- Unit and integration tests (JUnit 5, Mockito, MockMvc)
- Dockerfile and docker-compose with MySQL
- Retrieval (embeddings) for large note collections
- Streaming AI responses

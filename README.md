# Job Application Tracker with Outlook Email Sync

A full-stack portfolio project to track job applications manually and sync status updates from Outlook using Microsoft Graph API with **rule-based keyword parsing**.

## Tech stack

- **Backend:** Java 17, Spring Boot 3.2, Spring Security, JWT, Spring Data JPA, PostgreSQL, Maven, OpenAPI/Swagger, Lombok
- **Frontend:** React, React Router, Axios, clean CSS
- **Integration:** Microsoft Graph API, OAuth 2.0 for Outlook / Microsoft 365

## Features

- **Auth:** Register, login, BCrypt password hashing, JWT, protected routes
- **Job applications CRUD:** Create, read, update, delete with company, role, status, source, link, location, dates, notes
- **Status enum:** APPLIED, ASSESSMENT, INTERVIEW, OFFER, REJECTED, ARCHIVED
- **Search & filter:** By company/role, by status; sort by latest
- **Dashboard:** Counts by status, recent applications
- **Outlook sync:** Connect organizational mailbox via OAuth; fetch recent emails; rule-based classification (rejection, offer, interview, assessment, applied); create/update applications from detected emails

## Run locally

### Prerequisites

- Java 17+
- Maven
- Node.js 18+
- (Optional) PostgreSQL for persistent data; otherwise use H2 in-memory.

### 1. Backend – run without PostgreSQL (easiest)

Use the **H2 in-memory** profile so the app starts without any database setup:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

API: http://localhost:8080  
Swagger UI: http://localhost:8080/swagger-ui.html  
H2 console: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:jobtracker`, user: `sa`, password empty)

Data is in-memory and resets when the app stops.

### 2. Backend – run with PostgreSQL

If you use PostgreSQL:

1. Create the database and, if needed, a role. On macOS, the default superuser is often your **system username**, not `postgres`:

```bash
createdb jobtracker
# If you see "role postgres does not exist", use your macOS username instead:
# createuser -s $USER  # if needed
# createdb jobtracker
```

2. Set credentials if your PostgreSQL username differs from your system username:

```bash
export SPRING_DATASOURCE_USERNAME=your_db_username
export SPRING_DATASOURCE_PASSWORD=your_db_password
```

> **macOS note:** On macOS with Homebrew PostgreSQL, the default role is your system username (not `postgres`). The app defaults to `krishnachittella` — override with the env vars above if needed.

3. Run without the `h2` profile:

```bash
cd backend
mvn spring-boot:run
```

### 3. JWT secret

Set a secret at least 32 characters (or leave default for local dev only):

```bash
export JWT_SECRET="your-256-bit-secret-key-change-in-production-must-be-at-least-32-chars"
```

### 4. Frontend

```bash
cd frontend
npm install
npm run dev
```

App: http://localhost:5173 (or the port Vite prints).  
Set `VITE_API_URL=http://localhost:8080` if your API is elsewhere.

---

## Microsoft Graph / Outlook setup (optional)

To enable “Connect Outlook” and email sync:

1. **Azure Portal**
   - Go to [Azure Portal](https://portal.azure.com) → **Microsoft Entra ID** (Azure AD) → **App registrations** → **New registration**.
   - Name: e.g. “Job Tracker”.
   - Supported account types: “Accounts in any organizational directory” (or as needed for your tenant).
   - Redirect URI: **Single-page application (SPA)** and set:
     - `http://localhost:5173/outlook/callback` (for local frontend)
     - Or your production URL, e.g. `https://yourdomain.com/outlook/callback`.
   - Register.

2. **Application (client) ID and Tenant ID**
   - Overview page: copy **Application (client) ID** and **Directory (tenant) ID**.

3. **Client secret**
   - Certificates & secrets → New client secret → copy the **Value** (not the Secret ID). Store it safely; it’s shown only once.

4. **API permissions**
   - API permissions → Add permission → **Microsoft Graph** → **Delegated**.
   - Add: `User.Read`, `Mail.Read`, `openid`, `profile`, `email`, `offline_access`.
   - Grant admin consent if required by your tenant.

5. **Redirect URI for backend (if using backend redirect)**
   - The app uses **frontend** as the redirect target: user is sent to Microsoft, then back to your frontend at `/outlook/callback` with `?code=...&state=...`. The frontend sends `code` and `state` (JWT) to the backend `/api/outlook/callback`, which exchanges the code for tokens and stores them. So in Azure, the redirect URI must be the **frontend** URL (e.g. `http://localhost:5173/outlook/callback`), not the backend.

6. **Backend configuration**
   - Set environment variables (or add to `application.yml` / `application-local.yml`):

   ```bash
   export AZURE_CLIENT_ID="<application-client-id>"
   export AZURE_CLIENT_SECRET="<client-secret-value>"
   export AZURE_TENANT_ID="<directory-tenant-id>"
   export AZURE_REDIRECT_URI="http://localhost:5173/outlook/callback"
   ```

   Restart the backend. “Connect Outlook” will then work and sync will use Mail.Read to fetch recent messages and apply rule-based status extraction.

---

## Project structure

```
backend/
  src/main/java/com/jobtracker/
    entity/           # User, JobApplication, ApplicationStatus
    repository/       # JPA repositories
    dto/              # Request/response DTOs
    security/        # JWT, UserPrincipal, filter, UserDetailsService
    config/          # Security, CORS
    controller/      # Auth, Job applications, Dashboard, Outlook
    service/         # Auth, JobApplication, OutlookSync
    outlook/         # GraphClient, EmailStatusParser
  src/main/resources/
    application.yml
frontend/
  src/
    api.js            # Axios + auth API
    context/          # AuthContext
    pages/            # Login, Register, Dashboard, Applications, Outlook
    ProtectedRoute.jsx
    App.jsx, main.jsx, index.css
```

---

## API overview

- `POST /api/auth/register` – register
- `POST /api/auth/login` – login (returns JWT)
- `GET /api/applications` – list (optional `status`, `search`, `page`, `size`)
- `GET /api/applications/dashboard` – counts + recent
- `GET /api/applications/{id}` – get one
- `POST /api/applications` – create
- `PUT /api/applications/{id}` – update
- `DELETE /api/applications/{id}` – delete
- `GET /api/outlook/authorize?token=<JWT>` – get Microsoft authorize URL
- `POST /api/outlook/callback` – exchange code for tokens (body: `code`, `state` = JWT)
- `GET /api/outlook/status` – configured + connected
- `POST /api/outlook/sync` – sync mailbox and apply rule-based status extraction

All application and Outlook endpoints (except auth and callback) require `Authorization: Bearer <JWT>`.

---

## Email status extraction (rule-based)

The app uses keyword/phrase rules in `EmailStatusParser` to map email subject and body to status:

- **REJECTED:** e.g. “we have decided to move forward with other”, “not selected to move forward”
- **OFFER:** e.g. “we are pleased to offer”, “job offer”
- **INTERVIEW:** e.g. “interview scheduled”, “invite you to interview”
- **ASSESSMENT:** e.g. “online assessment”, “coding assessment”
- **APPLIED:** e.g. “application received”, “thank you for applying”

Company hint is taken from sender email domain or display name.

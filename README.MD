# Production-oriented  Authentication System

A full-stack, production-level authentication system built with
**Spring Boot 3.5** and **React**. Covers local JWT authentication,
email verification, Google OAuth2 login, refresh tokens, and
password reset — with a complete step-by-step build guide.

> Built as the authentication foundation for
> [CodeLab](https://github.com/Ashish77u/auth-system)
> — a source code marketplace.

---

## features

- Register with username, email, and password
- Email verification (UUID token, 24-hour expiry, one-time use)
- Login with JWT access token + refresh token
- Google OAuth2 login (auto-register or link existing account)
- Password reset via email (1-hour expiry)
- Auto token refresh (silent re-login before JWT expires)
- Rate limiting on auth endpoints (10 req/min per IP)
- Protected routes on frontend
- Full error handling — validation, expired tokens, disabled accounts

---

## Tech stack

### Backend
| Technology | Purpose |
|---|---|
| Spring Boot 3.5 | Application framework |
| Spring Security 6 | Security filter chain |
| Java 21 | Language |
| MySQL | Database |
| Spring Data JPA | Database ORM |
| jjwt 0.12.6 | JWT generation and validation |
| BCrypt | Password hashing |
| JavaMailSender | Email sending |
| Google OAuth2 Client | Google login |
| Bucket4j | Rate limiting |
| Lombok | Boilerplate reduction |

### Frontend
| Technology | Purpose |
|---|---|
| React 18 | UI framework |
| Vite | Build tool and dev server |
| Tailwind CSS v4 | Styling |
| React Router v6 | Client-side routing |
| Axios | HTTP client with interceptors |
| React Query | Server state and caching |
| React Hook Form | Form state management |
| Zod | Schema validation |
| React Hot Toast | Notifications |

---

## Project structure

```
auth-system/
├── auth-backend/                    # Spring Boot project
│   ├── src/main/java/com/codelab/backend/
│   │   ├── config/                  # Security, CORS, JWT filter
│   │   │   ├── ApplicationConfig.java
│   │   │   ├── SecurityConfig.java
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   ├── CorsConfig.java
│   │   │   ├── RateLimitingFilter.java
│   │   │   ├── HttpCookieOAuth2AuthorizationRequestRepository.java
│   │   │   ├── OAuth2AuthenticationSuccessHandler.java
│   │   │   ├── OAuth2AuthenticationFailureHandler.java
│   │   │   └── CookieUtils.java
│   │   ├── controller/
│   │   │   ├── AuthController.java  # register, login, verify, reset
│   │   │   └── UserController.java  # /api/user/me
│   │   ├── dto/                     # Request and response objects
│   │   ├── entity/                  # JPA entities (DB tables)
│   │   ├── enums/
│   │   │   └── Role.java
│   │   ├── exception/
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── repository/              # JPA repositories
│   │   └── service/                 # Business logic
│   │       ├── AuthService.java
│   │       ├── JwtService.java
│   │       ├── EmailService.java
│   │       ├── VerificationTokenService.java
│   │       ├── RefreshTokenService.java
│   │       └── CustomOAuth2UserService.java
│   ├── src/main/resources/
│   │   └── application.yml
│   ├── .env.example
│   └── pom.xml
│
└── auth-frontend/                   # React project
    ├── src/
    │   ├── api/
    │   │   └── axiosInstance.js     # Axios + interceptors
    │   ├── context/
    │   │   └── AuthContext.jsx      # Global auth state
    │   ├── hooks/
    │   │   └── useAuth.js
    │   ├── pages/
    │   │   ├── LoginPage.jsx
    │   │   ├── RegisterPage.jsx
    │   │   ├── VerifyEmailPage.jsx
    │   │   ├── OAuth2CallbackPage.jsx
    │   │   ├── DashboardPage.jsx
    │   │   ├── ForgotPasswordPage.jsx
    │   │   └── ResetPasswordPage.jsx
    │   ├── components/
    │   │   ├── ProtectedRoute.jsx
    │   │   ├── PublicRoute.jsx
    │   │   └── ErrorBoundary.jsx
    │   └── App.jsx
    ├── .env.example
    └── package.json
```

---

## Step-by-step build guide

This is the complete guide to building this project from scratch.
Every phase builds on the previous one.

---

### Prerequisites

Before starting, make sure you have:

- Java 21 — [download](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
- Maven — bundled with IntelliJ or install separately
- MySQL 8+ — [download](https://dev.mysql.com/downloads)
- IntelliJ IDEA or VS Code
- A Gmail account (for sending emails)
- A Google Cloud account (for OAuth2)

---

### Phase 1 — Backend project setup

**1. Generate the project**

Go to [start.spring.io](https://start.spring.io) and configure:

| Field | Value |
|---|---|
| Project | Maven |
| Language | Java |
| Spring Boot | 3.5.0 |
| Group | `com.codelab` |
| Artifact | `auth-backend` |
| Packaging | Jar |
| Java | 21 |

Add dependencies: Spring Web, Spring Security, Spring Data JPA,
MySQL Driver, Lombok, Java Mail Sender, OAuth2 Client, Validation.

**2. Add JWT dependency to `pom.xml`**

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

**3. Create `application.yml`**

Delete `application.properties`. Create `application.yml`:

```yaml
server:
  port: 8080

spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: [email, profile]
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"

application:
  jwt:
    secret: ${JWT_SECRET}
    expiration: ${JWT_EXPIRATION}
  frontend-url: ${FRONTEND_URL}
  oauth2:
    authorized-redirect-uri: ${FRONTEND_URL}/oauth2/callback
    cookie-expire-seconds: 180
```

**4. Create entities**

Create these JPA entities:
- `User` — implements `UserDetails` and `OAuth2User`
- `VerificationToken` — for email verification
- `RefreshToken` — for JWT refresh
- `PasswordResetToken` — for password reset

**5. Create repositories**

Create JPA repositories for each entity.
Add `@Transactional` to all `@Modifying` query methods.

---

### Phase 2 — JWT authentication

**Key files to create:**

- `ApplicationConfig.java` — `PasswordEncoder`, `AuthenticationProvider`,
  `AuthenticationManager` beans
- `JwtService.java` — generates and validates JWT tokens
- `JwtAuthenticationFilter.java` — reads `Authorization` header on every request
- `SecurityConfig.java` — filter chain, public vs protected URLs, stateless session
- `AuthController.java` — `/api/auth/register` and `/api/auth/login` endpoints
- `AuthService.java` — register (BCrypt hash) and login (authenticate + generate token)
- `GlobalExceptionHandler.java` — returns clean JSON errors

**How it works:**

```
Every request → JwtAuthenticationFilter
    → reads "Authorization: Bearer eyJ..." header
    → extracts email from token
    → loads user from DB
    → validates token signature and expiry
    → sets SecurityContextHolder
    → continues to controller
```

**Important:** Set `SessionCreationPolicy.STATELESS` in `SecurityConfig`.
No sessions. Every request must carry its own JWT.

---

### Phase 3 — Email verification

**Key files:**

- `EmailService.java` — sends HTML emails via JavaMailSender with `@Async`
- `VerificationTokenService.java` — creates UUID tokens, 24-hour expiry
- `AuthController.java` — add `GET /api/auth/verify-email?token=`
  and `POST /api/auth/resend-verification`

**Flow:**

```
Register → save user (enabled=false)
         → create UUID token → save to DB
         → send email with link (async)

User clicks link → GET /verify-email?token=UUID
         → find token in DB
         → check not expired
         → user.enabled = true
         → delete token (one-time use)
         → login now works
```

**Gmail setup:**
1. Enable 2-Step Verification on your Google account
2. Go to Google Account → Security → App passwords
3. Generate a 16-character app password
4. Use that in `MAIL_PASSWORD` — not your Gmail login password

**Add `@EnableAsync` to your main application class.**

---

### Phase 4 — Google OAuth2

**Google Cloud Console setup:**

```
1. console.cloud.google.com → New Project
2. APIs & Services → OAuth consent screen
   → External → fill app name and email
3. APIs & Services → Credentials
   → Create OAuth Client ID → Web application
   → Authorized redirect URIs:
     http://localhost:8081/login/oauth2/code/google
4. Copy Client ID and Client Secret
```

**Key files:**

- `HttpCookieOAuth2AuthorizationRequestRepository.java`
  — stores OAuth2 state in a cookie (stateless, no session)
- `CookieUtils.java` — cookie helper methods
- `OAuth2UserInfo.java` — extracts email, name, sub from Google response
- `CustomOAuth2UserService.java` — finds or creates user after Google login
- `OAuth2AuthenticationSuccessHandler.java`
  — generates JWT, redirects to frontend with token in URL
- `OAuth2AuthenticationFailureHandler.java`
  — redirects to login page with error message

**Critical:** The cookie serializer must save `registrationId` as an
attribute on the `OAuth2AuthorizationRequest`.
Without it Spring throws `registrationId cannot be empty`.

**Do not use `SecurityJackson2Modules` on the default `ObjectMapper`.**
It breaks normal REST request deserialization.
Use a manual Base64 field serializer instead.

**OAuth2 flow:**

```
/oauth2/authorization/google
    → Spring builds Google URL + saves state in cookie
    → Browser goes to Google
    → User picks account
    → Google redirects back with ?code=xyz
    → Spring exchanges code for user info
    → CustomOAuth2UserService: find or create user
    → OAuth2SuccessHandler: generate JWT
    → Redirect to frontend /oauth2/callback?token=eyJ...
```

---

### Phase 5 — React frontend

**1. Create the project**

```bash
npm create vite@latest auth-frontend -- --template react
cd auth-frontend
npm install react-router-dom axios @tanstack/react-query \
  react-hook-form @hookform/resolvers zod react-hot-toast @heroicons/react
npm install -D tailwindcss @tailwindcss/vite
```

**2. Configure Tailwind in `vite.config.js`**

```js
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
})
```

Replace `src/index.css` with `@import "tailwindcss";`

**3. Key concepts**

`axiosInstance.js` — single Axios client with two interceptors:
- Request interceptor: reads token from localStorage,
  adds `Authorization: Bearer` header to every request
- Response interceptor: catches 401, clears localStorage,
  redirects to login

`AuthContext.jsx` — React Context that stores user + token globally.
Any component calls `useAuth()` to get the logged-in user.

`ProtectedRoute.jsx` — checks `isAuthenticated` before rendering.
Redirects to login if no token.

`PublicRoute.jsx` — redirects to dashboard if already logged in.
Prevents logged-in users seeing login/register pages.

**4. Pages to build**

| Page | Route | Purpose |
|---|---|---|
| `RegisterPage` | `/register` | Registration form |
| `LoginPage` | `/login` | Login form + Google button |
| `VerifyEmailPage` | `/verify-email` | Handles email verify link |
| `OAuth2CallbackPage` | `/oauth2/callback` | Reads token from URL after Google login |
| `DashboardPage` | `/dashboard` | Protected, shows user profile |
| `ForgotPasswordPage` | `/forgot-password` | Sends reset email |
| `ResetPasswordPage` | `/reset-password` | Resets password with token |

---

### Phase 6 — Hardening

**Backend:**

- Normalize emails to lowercase in `AuthService` before every DB query
- Rate limiting with Bucket4j — 10 requests/min per IP on `/api/auth/**`
- Refresh tokens — 7-day expiry, one per user, stored in DB
- Password reset flow — 1-hour token, kills all refresh tokens on reset
- Move all secrets to `.env` file, read via `${VAR_NAME}` in `application.yml`
- Add `@Transactional` to all `@Modifying` repository methods

**Frontend:**

- Store refresh token in localStorage alongside access token
- Schedule auto-refresh 2 minutes before JWT expiry using `setTimeout`
- `ErrorBoundary` component — catches unexpected React crashes
- Add `/forgot-password` and `/reset-password` routes

---

### Running the project

**1. Set up environment variables**

Backend — copy `.env.example` to `.env` and fill in your values:

```bash
cd auth-backend
cp .env.example .env
# Edit .env with your MySQL password, Gmail app password, Google credentials
```

Frontend — copy `.env.example` to `.env`:

```bash
cd auth-frontend
cp .env.example .env
```

**2. Start MySQL**

```bash
# Mac
mysql.server start

# Windows — start MySQL service from Services panel
```

**3. Run backend**

```bash
cd auth-backend
./mvnw spring-boot:run
# Runs on http://localhost:8081
```

**4. Run frontend**

```bash
cd auth-frontend
npm run dev
# Runs on http://localhost:5173
```

---

### API reference

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | None | Register new user |
| POST | `/api/auth/login` | None | Login, returns JWT |
| GET | `/api/auth/verify-email?token=` | None | Verify email token |
| POST | `/api/auth/resend-verification` | None | Resend verify email |
| POST | `/api/auth/refresh` | None | Get new access token |
| POST | `/api/auth/logout` | None | Delete refresh token |
| POST | `/api/auth/forgot-password` | None | Send reset email |
| POST | `/api/auth/reset-password` | None | Reset password |
| GET | `/api/user/me` | JWT | Get current user |
| GET | `/oauth2/authorization/google` | None | Start Google login |


---

## License

MIT — use freely for learning or as a starting point for your projects.

---

## Author

Built by [Ashish Sahani](https://github.com/Ashish77u/auth-system)
as part of the CodeLab project.

⭐ Star this repo if it helped you understand authentication.
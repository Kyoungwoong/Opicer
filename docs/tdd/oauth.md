# OAuth (Kakao) + JWT — TDD Script

## Scope
- Kakao OAuth login
- JWT issuance + HttpOnly cookie
- /api/auth/me + /api/auth/logout
- ROLE_ADMIN via email allowlist

Non-goals:
- Google OAuth
- Refresh tokens
- UI beyond basic login/success pages

## Domain Rules (Acceptance Criteria)
1. OAuth success persists user and sets JWT cookie.
2. /api/auth/me returns 401 without JWT.
3. /api/auth/me returns user payload with valid JWT.
4. /api/admin/** requires ROLE_ADMIN.

## Test Strategy
- Unit tests:
  - JwtService create/parse token
- Integration tests:
  - /api/auth/me 401 vs 200
  - /api/admin/** 403 for ROLE_USER

## Test Cases (RED → GREEN)

### A. JWT
**A1 (RED): creates and parses JWT**
- Given: user info
- When: token issued
- Then: parsed claims match

Implementation notes (GREEN):
- HMAC secret >= 32 bytes

### B. Auth API
**B1 (RED): /api/auth/me without JWT returns 401**
- Given: no cookie
- When: GET /api/auth/me
- Then: 401

**B2 (RED): /api/auth/me with JWT returns 200**
- Given: valid JWT cookie
- When: GET /api/auth/me
- Then: 200 + user payload

### C. Admin Guard
**C1 (RED): /api/admin/** denied for ROLE_USER**
- Given: JWT with ROLE_USER
- When: GET /api/admin/ping
- Then: 403

## How to Run (Local)
```bash
./gradlew test
```

## Coverage Map (Requirements → Tests)
- R1 → A1
- R2 → B1
- R3 → B2
- R4 → C1

## Next Extensions
- Refresh token flow
- Google OAuth provider

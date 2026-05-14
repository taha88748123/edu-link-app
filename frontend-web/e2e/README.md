# NAJA7I-APP — End-to-end tests (Playwright)

This folder holds the Playwright spec suite that drives the React frontend
against a live backend. Five **golden paths** are exercised on every CI run:

| Spec                          | Flow                                            |
| ----------------------------- | ----------------------------------------------- |
| `auth-register-login.spec`    | Register → OTP → home → logout → login          |
| `student-booking-flow.spec`   | Search → pick prof → book a slot                |
| `admin-validate-prof.spec`    | Admin approves a pending prof                   |
| `anti-bypass-chat.spec`       | Phone digits get masked in PRE_ACCEPT chats     |
| `admin-topup-approve.spec`    | Student topup → admin approval → balance bumps  |

## Running locally

```bash
# 1. Bring up the full Docker stack with the test backdoor enabled.
NAJA7I_AUTH_TEST_MODE=true make up

# 2. Install Playwright browsers (one-off).
make e2e-install

# 3. Run the tests.
make e2e
```

The `playwright.config.ts` also boots `npm run dev` itself when no Vite server
is listening on `:5173`, so running `npm run e2e` from `frontend-web/` works
out of the box once Docker is up.

## The test backdoor — security model

In production the auth-service generates a 6-digit OTP and ships it via SMS
or WhatsApp. In dev the SMS driver is a stub, so the test runner needs another
way to read the code. We expose a **DEV-ONLY** endpoint:

```
GET http://localhost:8101/api/v1/dev/auth/otp/{userId}
Header: X-Test-Mode: true
```

The endpoint is protected by **three independent gates**:

1. **Spring `@ConditionalOnProperty`** — the controller bean is only created
   when `naja7i.auth.dev.test-mode=true`. In production this property is
   absent (defaults to `false`), so the handler does not exist. There is no
   `/api/v1/dev/auth/**` URL to call.
2. **Header check** — every handler additionally requires
   `X-Test-Mode: true`. Belt-and-suspenders against a misconfigured deploy.
3. **Gateway routing** — the dev path is NOT in `PublicPaths` of the
   `api-gateway`. Even if test mode leaked, the endpoint is unreachable from
   the public internet.

When `naja7i.auth.dev.test-mode=true` is set, auth-service emits a loud WARN
on startup so the canary log is impossible to miss:

```
WARN: naja7i.auth.dev.test-mode=true — DO NOT ENABLE IN PRODUCTION
```

The matching plain-OTP companion key (`auth:otp:plain:{userId}`) in Redis is
only written when the property is true. With test mode off, the store
behaves exactly as it did before this PR — bcrypt hashes only.

## Environment variables

| Variable                | Default                 | Purpose                                                 |
| ----------------------- | ----------------------- | ------------------------------------------------------- |
| `E2E_BASE_URL`          | `http://localhost:5173` | Frontend origin Playwright drives                       |
| `E2E_API_URL`           | `http://localhost:8080` | Backend gateway base URL                                |
| `E2E_AUTH_DIRECT_URL`   | `http://localhost:8101` | auth-service direct port (only used by the OTP peek)    |
| `E2E_ADMIN_PHONE`       | _required_              | Phone/email of a pre-seeded admin                       |
| `E2E_ADMIN_PASSWORD`    | _required_              | Password of the pre-seeded admin                        |
| `NAJA7I_AUTH_TEST_MODE` | `false`                 | Server-side flag that enables the OTP backdoor          |

Admins cannot self-register (the AuthService refuses `Role.ADMIN`), so the
test admin MUST be created by a separate seeder before the suite runs. Set
the two `E2E_ADMIN_*` vars to match that account.

## Anti-bypass test rationale

The anti-bypass spec is the single most important security regression test in
the suite. It asserts that, while a chat conversation is in `PRE_ACCEPT`
(i.e. the booking has been requested but not yet accepted), any message body
sent through the UI is **mutated server-side** to strip phone digits before
the recipient ever sees it. If this test fails, the platform's main
off-platform-deal control is broken — never merge changes that turn it red.

## Test data hygiene

Specs generate a fresh `+2126XXXXXXXX` phone for every user so two runs
against the same DB cannot collide. There is no per-test cleanup hook in v1
(`_fixtures/cleanup.ts` is a stub). A future revision may add a backend
`DELETE /api/v1/dev/test/reset` to wipe data between runs.

## Adding a new testid

When you need to anchor an E2E click on a new element, add a
`data-testid="…"` to the source component rather than relying on CSS classes
or i18n-aware text. Keep the testid stable across locale changes; never use
translated strings as a locator.

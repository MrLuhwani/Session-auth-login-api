package dev.luhwani.cookieLoginApi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CookieLoginApi {

	public static void main(String[] args) {
		SpringApplication.run(CookieLoginApi.class, args);
	}

}

/*
The current issue is that the cache for keepinf locked acct has it's issues

Maybe create other endpoints like /admin, delete account, change password

➕ 3. Things to ADD (core improvements)

These are MUST-HAVE for a solid backend project.

📄 README (very important)

Include:

Project overview
Tech stack
How to run
DB setup
Endpoints list
Sample requests/responses
Auth flow explanation (session + CSRF)
🧪 Tests

Start simple:

Unit tests:
Service layer (registration logic)
Integration tests:
/register
/login
/me
/logout

👉 Use:

MockMvc or TestRestTemplate
⚡ Input validation

🔐 CSRF clarity

You already implemented CSRF endpoint — good.

Improve:

Document how frontend uses it
Ensure cookie + header flow is clear

🧱 Database migration tool

Add:

Flyway or Liquibase

👉 So your schema is versioned

🔑 Password policy

🧾 Audit fields

Add to user:

Maybe a change password endpoint
an updated_at field to the sche,a

🧠 5. OPTIONAL (Advanced / standout features)

These are what make recruiters pause.

🌍 API documentation

Add:

Swagger / OpenAPI
🔄 Refresh session / remember-me

Implement:

persistent login (optional)

🧩 Role-based authorization

Right now:

roles exist

Upgrade:

restrict endpoints by role
🌐 Deployment

Deploy to:

Render / Railway / VPS

🧾 Final Prioritized Roadmap (DO THIS ORDER)

If you follow this order, your project jumps from 7.8 → 9+

Phase 3 (professionalism)
README
tests
Phase 5 (standout)
Swagger
deployment
 */
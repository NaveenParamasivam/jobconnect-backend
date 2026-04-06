# JobConnect — Backend API

<div align="center">

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen?style=flat-square&logo=springboot)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=flat-square&logo=mysql)
![JWT](https://img.shields.io/badge/Auth-JWT-black?style=flat-square&logo=jsonwebtokens)
![Twilio](https://img.shields.io/badge/SMS-Twilio-red?style=flat-square&logo=twilio)
![Swagger](https://img.shields.io/badge/Docs-Swagger%20UI-green?style=flat-square&logo=swagger)
![Tests](https://img.shields.io/badge/Tests-72%20cases-blue?style=flat-square&logo=junit5)
![Railway](https://img.shields.io/badge/Deployed-Railway-purple?style=flat-square&logo=railway)

**REST API for the JobConnect Job Portal**

[Live API Base URL](https://jobconnect-backend-production-436f.up.railway.app) · [Swagger UI](https://jobconnect-backend-production-436f.up.railway.app/swagger-ui.html) · [API Docs JSON](https://jobconnect-backend-production-436f.up.railway.app/api-docs) · [Frontend Repo](https://github.com/NaveenParamasivam/jobconnect-frontend)

</div>

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [API Endpoints](#api-endpoints)
- [Authentication](#authentication)
- [SMS Notifications (Twilio)](#sms-notifications-twilio)
- [Project Structure](#project-structure)
- [Local Setup](#local-setup)
- [Environment Variables](#environment-variables)
- [Running Tests](#running-tests)
- [Test Results](#test-results)
- [Deployment](#deployment)

---

## Overview

JobConnect Backend is a production-ready Spring Boot REST API that powers the JobConnect job portal. It provides role-based authentication (JWT), full job CRUD, job search with filters, application management, and Twilio SMS notifications.

**Two user roles:**
- **EMPLOYER** — post, edit, delete jobs; review and update applicant status
- **JOB_SEEKER** — browse jobs, search by keyword/location/category, apply with cover letter

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.0 |
| Security | Spring Security + JWT (jjwt 0.11.5) |
| Persistence | Spring Data JPA + Hibernate |
| Database | MySQL 8.0 |
| SMS | Twilio SDK 9.14.0 |
| API Docs | SpringDoc OpenAPI 3 / Swagger UI |
| Build | Maven |
| Testing | JUnit 5 + Mockito + Spring Boot Test |
| Deployment | Railway |

---

## Architecture

```
src/main/java/com/jobconnect/
├── config/
│   ├── SecurityConfig.java        # JWT filter chain, CORS, role-based access
│   ├── OpenApiConfig.java         # Swagger UI configuration
│   └── TwilioConfig.java          # Twilio SDK initialization
├── controller/
│   ├── AuthController.java        # POST /api/auth/register, /login
│   ├── JobController.java         # GET  /api/jobs (public)
│   ├── EmployerController.java    # /api/employer/** (EMPLOYER role)
│   └── SeekerController.java      # /api/seeker/**  (JOB_SEEKER role)
├── service/
│   ├── AuthService.java + impl
│   ├── JobService.java + impl
│   ├── ApplicationService.java + impl
│   └── SmsService.java + impl     # Twilio SMS dispatch
├── security/
│   ├── filter/JwtAuthFilter.java  # Token extraction & validation per request
│   └── service/UserDetailsServiceImpl.java
├── entity/
│   ├── User.java                  # Roles: JOB_SEEKER, EMPLOYER
│   ├── Job.java                   # Status: ACTIVE, CLOSED, DRAFT
│   └── JobApplication.java        # Status: PENDING → SHORTLISTED → ACCEPTED
├── repository/                    # Spring Data JPA with custom @Query search
├── dto/request/ + dto/response/   # Validated request DTOs, clean response DTOs
├── exception/                     # GlobalExceptionHandler + custom exceptions
└── util/JwtUtil.java              # Token generate / validate
```

---

## API Endpoints

### 🔓 Auth — Public

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/register` | Register as JOB_SEEKER or EMPLOYER |
| `POST` | `/api/auth/login` | Login → returns JWT token |

### 🔓 Jobs — Public (no auth required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/jobs` | List all active job postings |
| `GET` | `/api/jobs/{jobId}` | Get single job details |
| `GET` | `/api/jobs/search?keyword=&location=&category=&jobType=` | Search with filters |

### 🏢 Employer — Requires `EMPLOYER` role

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/employer/jobs` | Create a new job posting |
| `PUT` | `/api/employer/jobs/{jobId}` | Update a job posting |
| `DELETE` | `/api/employer/jobs/{jobId}` | Delete a job posting |
| `GET` | `/api/employer/jobs` | Get all my posted jobs |
| `GET` | `/api/employer/applications` | Get all applications (all jobs) |
| `GET` | `/api/employer/jobs/{jobId}/applications` | Get applications for a specific job |
| `PATCH` | `/api/employer/applications/{applicationId}/status` | Update application status |

### 🔍 Seeker — Requires `JOB_SEEKER` role

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/seeker/jobs/{jobId}/apply` | Apply to a job |
| `GET` | `/api/seeker/applications` | View my submitted applications |

> 📖 **Full interactive documentation:** [Swagger UI →](https://jobconnect-backend-production-436f.up.railway.app/swagger-ui.html)

---

## Authentication

All protected endpoints require a `Bearer` token in the `Authorization` header:

```http
Authorization: Bearer <your_jwt_token>
```

**Register example:**
```bash
curl -X POST https://jobconnect-backend-production-436f.up.railway.app/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Jane Seeker",
    "email": "jane@example.com",
    "password": "password123",
    "role": "JOB_SEEKER"
  }'
```

**Login example:**
```bash
curl -X POST https://jobconnect-backend-production-436f.up.railway.app/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "jane@example.com", "password": "password123"}'
```

Response includes `token` — use it as `Bearer <token>` for protected routes.

---

## SMS Notifications (Twilio)

SMS is sent automatically for the following events (if phone number provided):

| Event | Recipient |
|---|---|
| User registration | New user welcome message |
| Job posting created | Employer confirmation |
| Job application submitted | Seeker confirmation |
| Application status updated | Seeker status update |

SMS failures are logged as warnings and do **not** fail the API response.

---

## Project Structure

```
jobconnect-backend/
├── src/
│   ├── main/
│   │   ├── java/com/jobconnect/     # Application source
│   │   └── resources/
│   │       └── application.properties  # All values from env vars
│   └── test/
│       └── java/com/jobconnect/
│           ├── controller/          # WebMvcTest — 15 tests
│           ├── service/             # Mockito unit tests — 15 tests
│           ├── repository/          # DataJpaTest + H2 — 17 tests
│           └── e2e/                 # Full SpringBootTest — 15 tests
├── .env.example                     # Environment variable template
├── pom.xml
└── README.md
```

---

## Local Setup

### Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8.0+

### Steps

```bash
# 1. Clone the repository
git clone https://github.com/NaveenParamasivam/jobconnect-backend.git
cd jobconnect-backend

# 2. Create MySQL database
mysql -u root -p -e "CREATE DATABASE jobconnect_db;"

# 3. Copy env template and fill in values
cp .env.example .env
# Edit .env with your DB credentials, JWT secret, Twilio credentials

# 4. Generate a secure JWT secret
openssl rand -base64 32
# Paste the output as JWT_SECRET in .env

# 5. Run the application
./mvnw spring-boot:run
```

App starts at `http://localhost:8080`
Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## Environment Variables

Copy `.env.example` to `.env` and fill in all values. **Never commit `.env`.**

```env
# Database
DB_HOST=localhost
DB_PORT=3306
DB_NAME=jobconnect_db
DB_USERNAME=root
DB_PASSWORD=your_password

# JWT — generate with: openssl rand -base64 32
JWT_SECRET=your_256bit_secret
JWT_EXPIRATION_MS=86400000

# Twilio
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=your_twilio_auth_token
TWILIO_PHONE_NUMBER=+1XXXXXXXXXX

# Server
SERVER_PORT=8080
FRONTEND_URL=http://localhost:3000
```

---

## Running Tests

Tests use **H2 in-memory database** — no MySQL or Twilio required.

```bash
# Run all tests
./mvnw test

# Run only unit tests (service layer)
./mvnw test -pl . -Dtest="*ServiceTest"

# Run only controller tests
./mvnw test -pl . -Dtest="*ControllerTest"

# Run only E2E tests
./mvnw test -pl . -Dtest="*E2ETest"

# Generate HTML test report
./mvnw surefire-report:report
# Report saved to: target/site/surefire-report.html
```

---

## Test Results

### Test Coverage — 72 Test Cases across 12 Test Classes

| Layer | Test Class | Tests | Type |
|---|---|---|---|
| Controller | `AuthControllerTest` | 3 | WebMvcTest + MockMvc |
| Controller | `JobControllerTest` | 3 | WebMvcTest + MockMvc |
| Controller | `EmployerControllerTest` | 8 | WebMvcTest + MockMvc |
| Controller | `SeekerControllerTest` | 4 | WebMvcTest + MockMvc |
| Service | `AuthServiceTest` | 4 | JUnit 5 + Mockito |
| Service | `JobServiceTest` | 6 | JUnit 5 + Mockito |
| Service | `ApplicationServiceTest` | 5 | JUnit 5 + Mockito |
| Repository | `UserRepositoryTest` | 5 | DataJpaTest + H2 |
| Repository | `JobRepositoryTest` | 5 | DataJpaTest + H2 |
| Repository | `JobApplicationRepositoryTest` | 7 | DataJpaTest + H2 |
| E2E | `AuthE2ETest` | 6 | SpringBootTest + H2 |
| E2E | `JobWorkflowE2ETest` | 9 | SpringBootTest + H2 (no mocks) |

### Test Report

<!-- ADD TEST RESULTS SCREENSHOT/HTML REPORT BELOW -->
<!-- To host on GitHub Pages: -->
<!-- 1. Run: ./mvnw surefire-report:report -->
<!-- 2. Copy target/site/ to a /docs folder in your repo -->
<!-- 3. Go to repo Settings → Pages → Source: main branch /docs folder -->
<!-- 4. Your report will be live at: https://naveenparamasivam.github.io/jobconnect-backend/surefire-report.html -->

> 📊 **Test Report:** [View on GitHub Pages →](https://naveenparamasivam.github.io/jobconnect-backend/surefire-report.html)
>
> *(See [How to host test report on GitHub Pages](#how-to-host-test-report-on-github-pages) below)*

<!-- PASTE SCREENSHOT OF TEST RESULTS HERE -->
<!-- ![Test Results](docs/test-results-screenshot.png) -->

### How to Host Test Report on GitHub Pages

```bash
# 1. Generate the Surefire HTML report
./mvnw surefire-report:report

# 2. Create docs folder and copy report into it
mkdir -p docs
cp -r target/site/* docs/

# 3. Commit and push
git add docs/
git commit -m "Add test report to GitHub Pages"
git push

# 4. Enable GitHub Pages:
#    → Go to repo Settings → Pages
#    → Source: Deploy from branch
#    → Branch: main, Folder: /docs
#    → Click Save

# 5. Your report will be live at:
#    https://naveenparamasivam.github.io/jobconnect-backend/surefire-report.html
```

---

## Deployment

### Deployed on Railway

**Live API:** `https://jobconnect-backend-production-436f.up.railway.app`

**Swagger UI:** `https://jobconnect-backend-production-436f.up.railway.app/swagger-ui.html`

### Deploy your own instance on Railway

1. Create a new project on [Railway](https://railway.app)
2. Add a **MySQL** plugin to the project
3. Connect your GitHub repository
4. Set all environment variables from `.env.example` in Railway's **Variables** tab
5. Railway auto-deploys on every push to `main`

### Environment variables to set on Railway

```
DB_HOST         → from Railway MySQL plugin
DB_PORT         → 3306
DB_NAME         → railway
DB_USERNAME     → from Railway MySQL plugin
DB_PASSWORD     → from Railway MySQL plugin
JWT_SECRET      → run: openssl rand -base64 32
JWT_EXPIRATION_MS → 86400000
TWILIO_ACCOUNT_SID → from Twilio Console
TWILIO_AUTH_TOKEN  → from Twilio Console
TWILIO_PHONE_NUMBER → from Twilio Console
FRONTEND_URL    → https://jobconnectfe.netlify.app
SERVER_PORT     → 8080
```

---

## Related

- 🌐 **Frontend Repository:** [jobconnect-frontend](https://github.com/NaveenParamasivam/jobconnect-frontend)
- 🚀 **Frontend Live:** [jobconnectfe.netlify.app](https://jobconnectfe.netlify.app)

---

<div align="center">
  <sub>Built with Spring Boot 3 · Deployed on Railway · Documented with Swagger UI</sub>
</div>
# Marmoraria Orçamentos — API

REST API for quotation management built for Gaúcha Mármores, a marble and stone business in Campina das Missões, RS.

Developed as a real-world portfolio project, replacing a fully manual quotation process. The system is in active production use.

## Tech stack

- Java 17 + Spring Boot 3
- PostgreSQL
- Flyway (database migrations)
- Spring Security + JWT
- Cloudinary (image storage)
- Railway (cloud deployment)

## Features

- Full quotation lifecycle — create, update, and track status (draft → approved / rejected / expired)
- Client and product registry
- Multi-item quotations with discounts, shipping, and deadlines
- Image upload per item and per project
- Commercial proposal generation in PDF — three document modes: full proposal, objective summary, totals only
- Reusable observation templates
- Dashboard with key metrics
- JWT-based authentication
- Multi-environment configuration (local and production profiles)
- CORS configured for frontend integration

## Project structure

```
src/
├── config/          # Security, CORS, and app configuration
├── controller/      # REST endpoints
├── dto/             # Request and response objects
├── entity/          # JPA entities
├── repository/      # Spring Data repositories
├── service/         # Business logic
└── resources/
    ├── application.properties
    └── application-prod.properties
```

## Running locally

```bash
# Clone the repository
git clone https://github.com/victorkreuz/marmoraria-orcamentos-api

# Copy and fill in your local values
cp src/main/resources/application-example.properties \
   src/main/resources/application-local.properties

# Run with local profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

## Environment variables

| Variable | Description |
|---|---|
| `DATABASE_URL` | PostgreSQL connection string (provided by Railway) |
| `JWT_SECRET` | Secret key for token signing |
| `CORS_ALLOWED_ORIGINS` | Frontend origin (e.g. `https://your-app.vercel.app`) |
| `CLOUDINARY_URL` | Cloudinary connection string for image storage |
| `PORT` | Injected automatically by Railway |

## Related

Frontend: [marmoraria-orcamentos-web](https://github.com/victorkreuz/marmoraria-orcamentos-web)

## Deployment

Backend deployed on Railway. Internal production use — no public demo available.

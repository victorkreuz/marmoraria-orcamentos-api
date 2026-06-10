# \# Marmoraria Orçamentos — API

# 

# REST API for quotation management built for Gaúcha Mármores, a marble and stone business in Campina das Missões, RS.

# 

# Developed as a real-world portfolio project, replacing a fully manual quotation process.

# 

# \## Tech stack

# 

# \- Java 17 + Spring Boot 3

# \- PostgreSQL

# \- Spring Security + JWT

# \- Thymeleaf (PDF generation)

# \- Railway (cloud deployment)

# 

# \## Features

# 

# \- Full quotation lifecycle — create, update, and generate PDF documents

# \- Client and product registry

# \- JWT-based authentication

# \- Multi-environment configuration (local and production profiles)

# \- CORS configured for frontend integration

# 

# \## Project structure

# 

# ```

# src/

# ├── config/          # Security, CORS, and app configuration

# ├── controller/      # REST endpoints

# ├── dto/             # Request and response objects

# ├── entity/          # JPA entities

# ├── repository/      # Spring Data repositories

# ├── service/         # Business logic

# └── resources/

# &#x20;   ├── templates/   # Thymeleaf templates for PDF generation

# &#x20;   ├── application.properties

# &#x20;   └── application-prod.properties

# ```

# 

# \## Running locally

# 

# ```bash

# \# Clone the repository

# git clone https://github.com/victorkreuz/marmoraria-orcamentos-api

# 

# \# Copy and fill in your local values

# cp src/main/resources/application-example.properties \\

# &#x20;  src/main/resources/application-local.properties

# 

# \# Run with local profile

# ./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# ```

# 

# \## Environment variables

# 

# | Variable | Description |

# |---|---|

# | `DATABASE\_URL` | PostgreSQL connection string (provided by Railway) |

# | `JWT\_SECRET` | Secret key for token signing |

# | `CORS\_ALLOWED\_ORIGINS` | Frontend origin (e.g. `https://your-app.vercel.app`) |

# | `PORT` | Injected automatically by Railway |

# 

# \## Related

# 

# Frontend: \[marmoraria-orcamentos-web](https://github.com/victorkreuz/marmoraria-orcamentos-web)

# 

# \## Live demo

# 

# API: \*available after deploy\*  

# Frontend: \*available after deploy\*


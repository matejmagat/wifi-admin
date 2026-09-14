# Wi-Fi Admin

A full-stack application for managing Wi-Fi access points through a clean web interface and a Spring Boot backend. The project integrates a React frontend with an external SOAP service and exposes the integration through a RESTful API.

It was built as a practical backend-development exercise, with emphasis on service integration, API design, validation, authentication, error handling, testing, and containerised local development.

## Highlights

- Full-stack architecture: React frontend and Java/Spring Boot backend
- REST API for Wi-Fi access-point management
- SOAP client integration with an external Wi-Fi administration service
- Basic HTTP authentication for protected API endpoints
- PostgreSQL persistence with Flyway database migrations
- Input validation and structured error responses
- OpenAPI/Swagger documentation
- Automated tests with JUnit and Mockito
- Docker Compose setup for local infrastructure
- Mockoon mock server to support development without the external SOAP service

## What the application does

The application provides a browser-based interface for viewing and administering Wi-Fi access points. The frontend calls the backend REST API; the backend validates requests, applies application logic, and communicates with the external SOAP service responsible for Wi-Fi operations.

The project demonstrates a common enterprise integration pattern:

```text
React frontend
      │ HTTP/JSON
      ▼
Spring Boot REST API
      │ SOAP/XML
      ▼
External Wi-Fi administration service
```

PostgreSQL stores application data, while Flyway keeps database schema changes versioned and repeatable.

## Technology stack

| Area | Technologies |
| --- | --- |
| Backend | Java, Spring Boot, Gradle |
| API | REST, JSON, OpenAPI / Swagger |
| External integration | SOAP, WSDL-generated client code |
| Frontend | React, JavaScript |
| Database | PostgreSQL, Flyway |
| Security | HTTP Basic Authentication |
| Testing | JUnit, Mockito |
| Local development | Docker, Docker Compose, Mockoon |

## Repository structure

```text
.
├── wifi-admin-service/      # Spring Boot backend
├── wifi-admin-frontend/     # React single-page application
├── wsdl/                    # SOAP service definitions
├── openapi/                 # API specification and related resources
├── mockoon/                 # Mock API environment for local development
├── docker-compose.yml       # Local PostgreSQL/infrastructure configuration
├── TASK.md                  # Original project requirements
└── README.md
```

## Backend capabilities

The Spring Boot service is responsible for:

- Exposing REST endpoints consumed by the frontend
- Managing Wi-Fi access-point-related operations
- Integrating with a SOAP-based external system
- Converting between REST/JSON and SOAP/XML data models
- Validating incoming requests before processing them
- Protecting endpoints with HTTP Basic Authentication
- Persisting relevant application data in PostgreSQL
- Applying schema migrations through Flyway
- Returning consistent error responses for invalid requests and integration failures
- Publishing interactive API documentation through Swagger/OpenAPI

## Getting started

### Prerequisites

Install the following tools before running the project:

- Java Development Kit compatible with the backend configuration
- Node.js and npm
- Docker and Docker Compose
- Git

### 1. Clone the repository

```bash
git clone https://github.com/matejmagat/wifi-admin.git
cd wifi-admin
```

### 2. Start local infrastructure

Use Docker Compose to start the PostgreSQL database and any configured local services:

```bash
docker compose up -d
```

### 3. Run the backend

```bash
cd wifi-admin-service
./gradlew bootRun
```

On Windows:

```bat
gradlew.bat bootRun
```

### 4. Run the frontend

Open a second terminal:

```bash
cd wifi-admin-frontend
npm install
npm start
```

The frontend then communicates with the locally running backend. Check the frontend configuration and backend application configuration for the exact local ports, credentials, and service URLs used by your environment.

## API documentation

When the backend is running, Swagger/OpenAPI documentation is available through the Spring Boot service. Use it to inspect the available endpoints, request models, response formats, and authentication requirements.

The `openapi/` directory contains API-related resources, while the backend source defines the application behaviour.

## Testing

Run the backend test suite with:

```bash
cd wifi-admin-service
./gradlew test
```

The tests cover application logic in isolation using JUnit and Mockito, helping verify expected behaviour without relying on a live external system.

## Local SOAP integration

The application uses a SOAP service contract stored in `wsdl/`. For local development and predictable testing, the `mockoon/` directory provides a mock environment that can stand in for the external dependency.

This approach makes it possible to develop and test the REST layer independently while retaining the structure of a real-world legacy or enterprise SOAP integration.

## Engineering focus

This project was designed to practise production-relevant backend skills rather than only building a UI:

- Designing clear REST interfaces around an existing SOAP dependency
- Working with WSDL contracts and generated client code
- Separating controller, service, integration, and persistence responsibilities
- Handling validation, errors, and authentication consistently
- Managing relational schema changes with Flyway
- Testing service-layer behaviour with mocks
- Reproducing a local environment with Docker Compose

## Author

**Matej Magat**

- GitHub: [@matejmagat](https://github.com/matejmagat)
- LinkedIn: [matej-magat](https://www.linkedin.com/in/matej-magat/)

## License

This repository is intended as a portfolio and learning project. Please contact the author before reusing substantial parts of the implementation.

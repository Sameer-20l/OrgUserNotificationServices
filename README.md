# Organisation Catalogue Service

A Spring Boot microservice that manages and serves **organisation** catalogue data — the organisations (and their departments) that consume the Open Agri Stack catalogues — backed by PostgreSQL for storage, Elasticsearch for fast search, and Redis for caching.

## Overview

The Organisation Catalogue Service is part of the **OpenAgriStack** ecosystem and is built on the **VERG** registry framework (see [TECH-README.md](TECH-README.md) and [Catalogue-Spec.MD](Catalogue-Spec.MD)). It exposes REST APIs to create, read, search, and manage organisation records — e.g. government departments, research institutes, and NGOs such as the departments of Horticulture, Soil Science, or Plant Pathology — that reference and leverage the agri catalogues (crop, seed, soil, etc.) elsewhere in the platform.

Each record is stored schemalessly: an incoming JSON document is validated against a per-catalogue JSON Schema, persisted in PostgreSQL (as a `jsonb` column), indexed into Elasticsearch for search, and cached in Redis.

## Features

- CRUD-style operations for organisation catalogue records (create, read, search, delete, bulk import)
- Schemaless JSON storage in PostgreSQL (`jsonb`) with JSON-Schema payload validation per catalogue
- Elasticsearch-backed search with configurable index mappings per entity
- Redis caching for reads and search results with configurable TTL
- Externalized, per-entity Elasticsearch field configuration via `application.properties`
- Code-generated catalogues — new registries are scaffolded by a metaprogramming tool (`main.py`), not hand-written

## Tech Stack

- **Java 17 / Spring Boot 3.3.5**
- **PostgreSQL** — source-of-truth storage (documents held in a `jsonb` column)
- **Elasticsearch 8.13** — search index for catalogue records
- **Redis** — caching layer for reads and search results
- **Maven** — build tooling
- **Lombok** — boilerplate reduction

## Supported Catalogues

- **Organisation** (`org`)

> More catalogues can be onboarded with the metaprogramming tool — see [Adding a Catalogue](#adding-a-catalogue).

## Getting Started

### Prerequisites

- Java 17+ (see `pom.xml`)
- Maven 3.9+ &nbsp;*(note: the `./mvnw` wrapper is not checked in — use a system `mvn`)*
- Docker + Docker Compose (provides PostgreSQL, Elasticsearch, and Redis)

### Configuration

Environment-specific values live in `src/main/resources/application.properties`:

- PostgreSQL connection — `spring.datasource.url/username/password` (defaults to `oas_db` / `oas_user`)
- Elasticsearch connection — `elasticsearch.host/port`
- Redis connection and cache TTL — `spring.redis.host/port`, `spring.redis.cacheTtl`
- Per-catalogue Elasticsearch field config — `elastic.required.field.<catalogue>.json.path`

The bundled `docker-compose.yml` starts PostgreSQL (5433), Redis (6379), and Elasticsearch (9200), plus pgAdmin (5050), Kibana (5601), and Redis Commander (8081).

### Build

```bash
mvn clean package
```

### Run

```bash
# 1. Start the backing services
docker compose up -d

# 2. Start the application (defaults to http://localhost:8080)
mvn spring-boot:run
```

Swagger UI is available at `http://localhost:8080/swagger-ui/index.html`.

### Seeding Catalogue Entities

A helper script creates the catalogue(s) in bulk via the metaprogramming tool:

```bash
./createEntities.sh
```

This loops through the entity list and invokes `python3 main.py --action create --name <name>` for each.

### Adding a Catalogue

New catalogues (registries) are generated from the templates in `registry_template/` by `main.py` — you do **not** hand-write the Java. See [Setup.MD](Setup.MD) for the full workflow. In short:

```bash
python3 main.py --action create --name <catalogue-name>   # requires: pip install pyfiglet
```

This scaffolds the controller/entity/repository/service slice under `com.catalogue.verg.<name>`, creates the payload-validation and Elasticsearch field JSONs under `src/main/resources/`, and registers the catalogue in `Constants.java`, `application.properties`, and `VergProperties.java`. After generating, define the catalogue's schema in:

- `src/main/resources/payloadValidation/<name>PayloadValidation.json` — the JSON Schema (use **camelCase** field keys; the primary-key field carries `prefix`, `key: "Primary"`, `keyLength`)
- `src/main/resources/EsFieldsmapping/es<Name>RequiredFields.json` — the allowlist of fields to index into Elasticsearch

To remove a catalogue: `python3 main.py --action delete --name <catalogue-name>`.

## API

All endpoints live under `/<catalogue>/v1/...` (for the organisation catalogue, `/org/v1/...`):

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/org/v1/create` | Create a record (JSON body, validated against the schema) |
| `GET` | `/org/v1/read/{id}` | Read a record by id |
| `POST` | `/org/v1/search` | Search (Elasticsearch-backed; filter / query / facets) |
| `DELETE` | `/org/v1/delete/{id}` | Soft-delete a record (status → `INACTIVE`) |
| `POST` | `/org/v1/import` | Bulk import from an uploaded CSV/XLSX file (≤ 5 MB) |

Example — create an organisation:

```bash
curl -s -X POST http://localhost:8080/org/v1/create \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Karnataka Department of Horticulture",
    "type": "Government Department",
    "location": "locmap-000000000001",
    "department": "Horticulture",
    "address": "Lalbagh Road, Bengaluru 560004",
    "contactNumber": "+91-80-26578184",
    "contactEmail": "horticulture@karnataka.gov.in"
  }'
```

## Project Structure

```
src/main/java/com/catalogue/verg/
├── core/                 # Shared, generic infrastructure (framework — do not hand-edit)
│   ├── cache/            # Redis cache service + config
│   ├── config/           # Jackson / app config
│   ├── dto/              # CustomResponse and shared DTOs
│   ├── elasticsearch/    # ES client, search service, search DTOs
│   ├── exception/        # Global REST exception handling
│   ├── logger/           # Logging helper
│   ├── service/          # Generic bulk-import service
│   └── util/             # Constants, PayloadValidation, PrimaryKeyUtil, VergProperties, ...
└── org/                  # Organisation catalogue (generated via main.py)
    ├── controller/       # OrgController — REST endpoints
    ├── entity/           # OrgEntity — JPA @Entity (jsonb data column)
    ├── repository/       # OrgRepository
    └── service/          # OrgService + impl/OrgServiceImpl

src/main/resources/
├── application.properties   # DB / ES / Redis config + per-catalogue registration
├── payloadValidation/       # <catalogue>PayloadValidation.json — JSON Schema per catalogue
└── EsFieldsmapping/         # es<Catalogue>RequiredFields.json — ES field allowlist per catalogue
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Open a pull request

**Conventions**

- `core/`, `main.py`, and `registry_template/` are finalized framework code — **do not hand-edit them.** Onboard new catalogues with `main.py` (see [Adding a Catalogue](#adding-a-catalogue)).
- Per-catalogue changes belong in the generated slice and its `payloadValidation` / `EsFieldsmapping` JSONs.
- Elasticsearch field keys must be **camelCase** — spaces or capitalized keys break ES.

## License

MIT License — see [LICENSE](LICENSE).

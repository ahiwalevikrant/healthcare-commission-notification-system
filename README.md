# 🏥 Healthcare Commission Payout Notification System

A production-grade, event-driven microservices platform built in the **US healthcare insurance domain** — simulating how carriers pay commissions to agents, how disputes are filed, and how payouts are notified. Built for portfolio.

---

## 📌 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Services](#services)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [API Reference](#api-reference)
- [Kafka Event Flow](#kafka-event-flow)
- [Project Structure](#project-structure)
- [Frontend](#frontend)
- [What This Project Demonstrates](#what-this-project-demonstrates)
- [Roadmap](#roadmap)

---

## Overview

In the US healthcare insurance market, **carriers** (e.g. Aetna, BCBS, UnitedHealth) pay **commissions** to **agents** (identified by NPN — National Producer Number) based on policies they sell. This system automates:

- Agent registration and JWT-based authentication
- Commission record management and monthly payout calculation
- Event-driven payout notifications via Apache Kafka
- Free-text dispute submission with Named Entity Recognition (Stanford NLP)
- Natural language Q&A over commission data using a local LLM (Ollama / Llama 3)

```
Agent registers → Commission submitted → Commission calculated
      → Kafka event published → Notification email sent
            → Agent files dispute (free text)
                  → NER extracts entities → Commission flagged for review
                        → GenAI answers ops team queries in natural language
```

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                  Healthcare Commission Platform                     │
│                                                                     │
│   ┌──────────────┐     REST      ┌────────────────────┐            │
│   │ agent-service│◄─────────────►│ commission-service │            │
│   │   port 8081  │               │    port 8082        │            │
│   └──────────────┘               └────────┬───────────┘            │
│          │ JWT Auth                        │                        │
│          │                         Kafka Topic:                     │
│          │                     commission.calculated                │
│          │                                │                        │
│          │                       ┌────────▼───────────┐            │
│          │                       │notification-service │            │
│          │                       │    port 8083        │            │
│          │                       │  (Email via Mailhog)│            │
│          │                       └────────────────────┘            │
│          │                                                          │
│   ┌──────▼──────┐    Kafka Topic:   ┌────────────────────┐         │
│   │  ner-service│   dispute.submitted│ commission-service │         │
│   │  port 8084  │──────────────────►│  (flags record)    │         │
│   │(Stanford NLP)│                  └────────────────────┘         │
│   └─────────────┘                                                   │
│                                                                     │
│   ┌──────────────┐    REST (calls all services)                     │
│   │ genai-service│◄────────────────────────────────────────────     │
│   │  port 8085   │  Natural language Q&A via Ollama (Llama 3)       │
│   └──────────────┘                                                  │
└─────────────────────────────────────────────────────────────────────┘
```

**Each service has its own isolated MYSQL database. Services communicate via REST (sync) and Kafka (async).**

---

## Services

### 1. `agent-service` — Port 8081
Manages agent registration and authentication.

| Endpoint | Method | Description |
|---|---|---|
| `/api/agents/register` | POST | Register a new agent |
| `/api/agents/login` | POST | Login → returns JWT token |
| `/api/agents/{npn}` | GET | Get agent by NPN |
| `/api/agents` | GET | Paginated agent list |

- JWT-based stateless authentication (Spring Security)
- NPN (National Producer Number) as unique agent identifier
- Database: `agentdb`

---

### 2. `commission-service` — Port 8082
Core commission management — submit, calculate, filter, and flag records.

| Endpoint | Method | Description |
|---|---|---|
| `/api/commissions` | POST | Submit a commission record |
| `/api/commissions/{id}/calculate` | PUT | Trigger calculation → publishes Kafka event |
| `/api/commissions` | GET | Filtered + paginated list |
| `/api/commissions/summary` | GET | Monthly payout summary by agent |
| `/api/commissions/{id}/flag-dispute` | PUT | Flag a record for review |

- Status lifecycle: `PENDING → CALCULATED → PAID`
- Publishes to Kafka topic: `commission.calculated`
- Consumes from Kafka topic: `dispute.submitted` (flags affected record)
- Database: `commissiondb`

---

### 3. `notification-service` — Port 8083
Listens for payout events and sends email notifications.

| Endpoint | Method | Description |
|---|---|---|
| `/api/notifications` | GET | Notification history by agent NPN |

- Kafka consumer on topic: `commission.calculated`
- Sends payout summary emails via JavaMailSender → Mailhog (local SMTP)
- Dead Letter Topic: `commission.calculated.DLT` for failed deliveries
- Retry: 3 attempts before routing to DLT
- Database: `notificationdb`

---

### 4. `ner-service` — Port 8084
Analyses free-text dispute submissions using Stanford NLP Named Entity Recognition.

| Endpoint | Method | Description |
|---|---|---|
| `/api/disputes/analyse` | POST | Extract entities + save + publish Kafka event |
| `/api/disputes` | GET | Paginated dispute list |
| `/api/disputes/{id}/resolve` | PUT | Mark dispute as resolved |

**Example input:**
```
"Agent John Smith, NPN 1234567, claims missing commission 
from Aetna for policy EP-9921 in Ohio for March 2026"
```

**Extracted entities:**
```json
{
  "PERSON": "John Smith",
  "ORGANIZATION": "Aetna",
  "NPN": "1234567",
  "POLICY": "EP-9921",
  "LOCATION": "Ohio",
  "DATE": "March 2026"
}
```

- Publishes to Kafka topic: `dispute.submitted`
- Database: `nerdb`

---

### 5. `genai-service` — Port 8085
Natural language Q&A interface over commission data using Ollama (Llama 3).

| Endpoint | Method | Description |
|---|---|---|
| `/api/query` | POST | Ask a natural language question |

**Supported intents:**

| Question Pattern | Calls |
|---|---|
| "Unpaid commissions for agent {NPN}" | `commission-service /api/commissions?agentNpn=X&status=PENDING` |
| "Total payout for month {month}" | `commission-service /api/commissions/summary?month=X` |
| "Disputes submitted today" | `ner-service /api/disputes?date=today` |
| "Notifications for agent {NPN}" | `notification-service /api/notifications?agentNpn=X` |
| "Agent details for NPN {NPN}" | `agent-service /api/agents/{npn}` |

- Stateless — no database
- Uses locally running Ollama at `http://localhost:11434/api/generate`

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| Messaging | Apache Kafka (Confluent) |
| ORM | Spring Data JPA + Hibernate |
| Databases | PostgreSQL 15 (one per service) |
| Auth | Spring Security + JWT (stateless) |
| NLP | Stanford CoreNLP |
| GenAI | Ollama (Llama 3, local) |
| Email | JavaMailSender + Mailhog (local SMTP) |
| API Docs | Springdoc OpenAPI 2.x (Swagger UI) |
| Containerisation | Docker + Docker Compose |
| Build | Maven |
| Frontend | Angular 17 / React 18 (see Frontend section) |

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker + Docker Compose
- Ollama installed locally → [ollama.ai](https://ollama.ai)

### 1. Clone the repository

```bash
git clone https://github.com/ahiwalevikrant/healthcare-commission-notification-system.git
cd healthcare-commission-notification-system
```

### 2. Start infrastructure (Kafka + all databases + Mailhog)

```bash
docker-compose up -d
```

This starts:
- Zookeeper + Kafka (port 9092)
- agent-db (PostgreSQL, port 5433)
- commission-db (PostgreSQL, port 5434)
- notification-db (PostgreSQL, port 5435)
- ner-db (PostgreSQL, port 5436)
- Mailhog UI (port 8025) + SMTP (port 1025)

### 3. Pull the Ollama model (for genai-service)

```bash
ollama pull llama3
ollama serve
```

### 4. Start each service

Open 5 terminals, one per service:

```bash
# Terminal 1
cd agent-service && mvn spring-boot:run

# Terminal 2
cd commission-service && mvn spring-boot:run

# Terminal 3
cd notification-service && mvn spring-boot:run

# Terminal 4
cd ner-service && mvn spring-boot:run

# Terminal 5
cd genai-service && mvn spring-boot:run
```

### 5. Access Swagger UI for each service

| Service | Swagger URL |
|---|---|
| agent-service | http://localhost:8081/swagger-ui.html |
| commission-service | http://localhost:8082/swagger-ui.html |
| notification-service | http://localhost:8083/swagger-ui.html |
| ner-service | http://localhost:8084/swagger-ui.html |
| genai-service | http://localhost:8085/swagger-ui.html |

### 6. View emails

Open Mailhog UI at **http://localhost:8025** to see all payout notification emails.

---

## API Reference

### Standard Response Wrapper

All APIs return:

```json
{
  "success": true,
  "data": {},
  "message": "Commission calculated successfully",
  "timestamp": "2026-04-01T10:30:00Z"
}
```

### Pagination

All list endpoints support:

```
GET /api/commissions?page=0&size=10&sort=createdAt,desc
```

### Authentication

All endpoints (except `/api/agents/register` and `/api/agents/login`) require:

```
Authorization: Bearer <jwt_token>
```

---

## Kafka Event Flow

### Topic: `commission.calculated`

**Published by:** `commission-service`
**Consumed by:** `notification-service`

```json
{
  "agentNpn": "1234567",
  "agentEmail": "agent@example.com",
  "carrierId": "AETNA",
  "month": "2026-04",
  "totalPayout": 1250.00,
  "status": "CALCULATED"
}
```

### Topic: `dispute.submitted`

**Published by:** `ner-service`
**Consumed by:** `commission-service`

```json
{
  "disputeId": "uuid",
  "agentNpn": "1234567",
  "carrierName": "Aetna",
  "policyId": "EP-9921",
  "state": "Ohio",
  "month": "March 2026",
  "rawText": "Agent John Smith..."
}
```

### Dead Letter Topic: `commission.calculated.DLT`

If `notification-service` fails to send an email after 3 retries, the event is routed here for manual review.

---

## Project Structure

```
payout-notification-system/
│
├── docker-compose.yml               ← Full infrastructure setup
│
├── agent-service/
│   ├── src/main/java/
│   │   ├── entity/Agent.java
│   │   ├── repository/AgentRepository.java
│   │   ├── service/AgentService.java
│   │   ├── controller/AgentController.java
│   │   ├── security/JwtUtil.java
│   │   └── config/SecurityConfig.java
│   └── src/main/resources/application.yml
│
├── commission-service/
│   ├── src/main/java/
│   │   ├── entity/CommissionRecord.java
│   │   ├── repository/CommissionRepository.java
│   │   ├── service/CommissionService.java
│   │   ├── controller/CommissionController.java
│   │   ├── kafka/CommissionEventProducer.java
│   │   └── kafka/DisputeEventConsumer.java
│   └── src/main/resources/application.yml
│
├── notification-service/
│   ├── src/main/java/
│   │   ├── entity/NotificationLog.java
│   │   ├── kafka/PayoutNotificationConsumer.java
│   │   ├── service/EmailNotificationService.java
│   │   └── controller/NotificationController.java
│   └── src/main/resources/application.yml
│
├── ner-service/
│   ├── src/main/java/
│   │   ├── entity/DisputeRecord.java
│   │   ├── nlp/StanfordNERExtractor.java
│   │   ├── service/DisputeService.java
│   │   ├── kafka/DisputeEventProducer.java
│   │   └── controller/DisputeController.java
│   └── src/main/resources/application.yml
│
├── genai-service/
│   ├── src/main/java/
│   │   ├── service/IntentDetectorService.java
│   │   ├── service/OllamaService.java
│   │   ├── client/ (Feign clients for all services)
│   │   └── controller/GenAIController.java
│   └── src/main/resources/application.yml
│
└── README.md
```

---

## Frontend

The UI is built as a **lazy-loaded Angular module** integrated into a host application, mirroring enterprise CRM architecture patterns.

### Pages

| Page | Description |
|---|---|
| **Dashboard** | Summary cards + monthly payout trend chart + recent notifications |
| **Commissions** | Filterable paginated table with calculate/flag-dispute actions |
| **Dispute Analyser** | Free-text input → coloured entity extraction tags + dispute history |
| **AI Assistant** | Chat-style interface for natural language commission queries |
| **Notifications** | Read-only payout notification history |
| **Agents** | Agent directory with NPN search |

### Key Angular patterns used

- Lazy-loaded feature module with nested child routes
- RxJS operators: `forkJoin`, `switchMap`, `BehaviorSubject`, `takeUntil`, `debounceTime`
- `OnPush` change detection on all table/list components
- HTTP interceptor for JWT attachment and global 401 handling
- Reactive Forms with custom validators
- Server-side pagination, filtering, and sorting

---

## What This Project Demonstrates

| Concept | Where |
|---|---|
| Microservices architecture | 5 independent Spring Boot services |
| Event-driven communication | Kafka producer/consumer across services |
| Fault tolerance | Dead letter topic + retry in notification-service |
| Database isolation | Separate PostgreSQL per service |
| Stateless auth | JWT via Spring Security in agent-service |
| NLP integration | Stanford CoreNLP in ner-service |
| GenAI integration | Ollama (Llama 3) in genai-service |
| API documentation | Swagger UI on every service |
| Containerisation | Full docker-compose.yml for local dev |
| Enterprise Angular patterns | Lazy loading, RxJS, OnPush, interceptors |

---

## Roadmap

- [ ] agent-service — registration + JWT auth
- [ ] commission-service — CRUD + Kafka producer
- [ ] notification-service — Kafka consumer + Mailhog email
- [ ] ner-service — Stanford NLP + Kafka producer
- [ ] genai-service — Ollama + intent detection
- [ ] Docker Compose for full system
- [ ] Angular frontend — all 6 pages
- [ ] JUnit tests for commission calculation logic
- [ ] Dead letter queue handling + retry
- [ ] Architecture diagram (Excalidraw)
- [ ] Postman collection

---

## Author

**Vikrant Ahiwale**
Product Engineer | Java · Spring Boot · Angular · React

[![LinkedIn](https://img.shields.io/badge/LinkedIn-vikrant--ahiwale-blue)](https://linkedin.com/in/vikrant-ahiwale)
[![GitHub](https://img.shields.io/badge/GitHub-ahiwalevikrant-black)](https://github.com/ahiwalevikrant)

---

> Built as a portfolio project to demonstrate microservices, event-driven architecture, NLP, and GenAI integration in the US healthcare insurance domain.
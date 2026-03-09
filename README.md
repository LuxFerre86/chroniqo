# chroniqo

A self-hosted time tracking web application built with Spring Boot and Vaadin.

Track your working hours, manage absences, and monitor your work-time balance — all in a clean, privacy-friendly interface you host yourself.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-green)
![Vaadin](https://img.shields.io/badge/Vaadin-25-blue)
![License](https://img.shields.io/badge/License-AGPL%20v3-blue)

## Features

- **Time Tracking** – Log daily work entries with start time, end time, and break duration
- **Dashboard** – See today's summary, weekly progress chart, and your current balance at a glance
- **Month View** – Calendar-style overview of all days, worked hours, and daily balance
- **Absence Management** – Record vacation, sick leave, and public holidays
- **Work-Time Balance** – Automatic calculation of overtime and undertime across any period
- **Email Verification** – Account registration with email confirmation
- **Password Reset** – Self-service password reset via email
- **Remember Me** – Persistent login sessions (configurable duration)
- **Docker Ready** – Single-container deployment with Docker Compose

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 4, Spring Security, Spring Data JPA |
| Frontend | Vaadin 25 (server-side Java UI) |
| Database | MariaDB |
| Build | Maven, Vaadin Maven Plugin |
| Deployment | Docker, GitHub Container Registry |

## Quick Start

### Prerequisites

- Docker & Docker Compose
- A MariaDB instance (or use the Docker example below)
- A Gmail account with an [App Password](https://support.google.com/accounts/answer/185833) for email sending

### 1. Configure environment

```bash
cp .env.example .env
```

Edit `.env` with your values:

```env
DATABASE_URL=jdbc:mariadb://your-db-host:3306/chroniqo
DATABASE_USERNAME=chroniqo
DATABASE_PASSWORD=your-db-password

GMAIL_USERNAME=you@gmail.com
GMAIL_PASSWORD=your-app-password

REMEMBER_ME_KEY=your-random-32-char-string
REMEMBER_ME_SECURE_COOKIE=true
COOKIE_DOMAIN=yourdomain.com

APP_VERSION=latest
```

### 2. Set up the database schema

chroniqo uses Hibernate's `ddl-auto: validate` – the schema must exist before the app starts.
Run the DDL script against your MariaDB instance:

```bash
mysql -u chroniqo -p chroniqo < src/main/resources/db/migration/schema.sql
```

### 3. Start the application

```bash
docker compose up -d
```

The app will be available at `http://localhost:8180`.

## Development Setup

See [CONTRIBUTING.md](CONTRIBUTING.md) for local development instructions.

## Configuration Reference

All configuration is done via environment variables. See [`.env.example`](.env.example) for a full list with descriptions.

| Variable | Description | Default |
|----------|-------------|---------|
| `DATABASE_URL` | JDBC connection URL | – |
| `DATABASE_USERNAME` | Database user | – |
| `DATABASE_PASSWORD` | Database password | – |
| `GMAIL_USERNAME` | Gmail address for sending emails | – |
| `GMAIL_PASSWORD` | Gmail App Password | – |
| `REMEMBER_ME_KEY` | Secret key for remember-me cookies (min. 32 chars) | – |
| `REMEMBER_ME_SECURE_COOKIE` | Restrict cookie to HTTPS | `true` |
| `COOKIE_DOMAIN` | Domain for the remember-me cookie | `localhost` |
| `APP_VERSION` | Displayed application version | `dev` |

## Deployment

chroniqo ships as a multi-arch Docker image (`linux/amd64`, `linux/arm64`) published to GitHub Container Registry on every merge to `master`.

```bash
docker pull ghcr.io/luxferre86/chroniqo:latest
```

See [docker-compose.yaml](docker-compose.yaml) for a ready-to-use Compose configuration.

## Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request.

## License

This project is licensed under the **GNU Affero General Public License v3.0 (AGPL-3.0)**.

You are free to use, modify, and self-host this software. If you use it to provide
a network service, you must make the complete source code — including any modifications
— available to your users under the same license.

For commercial use without these obligations, contact the author via GitHub for a
commercial license. See [LICENSE](LICENSE) for full terms.

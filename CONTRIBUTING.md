# Contributing to chroniqo

Thank you for your interest in contributing! Here's everything you need to get started.

## Development Setup

### Prerequisites

- Java 21
- Maven 3.9+
- Docker & Docker Compose (for running MariaDB locally)
- Node.js 18+ (used internally by Vaadin's frontend build)

### Getting Started

1. **Clone the repository**
   ```bash
   git clone https://github.com/luxferre86/chroniqo.git
   cd chroniqo
   ```

2. **Configure your environment**
   ```bash
   cp .env.example .env
   # Edit .env with your local values
   ```

3. **Start a local MariaDB**
   ```bash
   docker run -d \
     --name chroniqo-db \
     -e MYSQL_DATABASE=chroniqo \
     -e MYSQL_USER=chroniqo \
     -e MYSQL_PASSWORD=change-me \
     -e MYSQL_ROOT_PASSWORD=root \
     -p 3306:3306 \
     mariadb:11
   ```

4. **Run the application**
   ```bash
   mvn spring-boot:run
   ```
   The app will be available at `http://localhost:8080`.

5. **Run the tests**
   ```bash
   mvn verify
   ```

## Branch Naming

| Type | Pattern | Example |
|------|---------|---------|
| Feature | `feat/<short-description>` | `feat/workday-config` |
| Bug fix | `fix/<short-description>` | `fix/balance-calculation` |
| Chore / refactoring | `chore/<short-description>` | `chore/cleanup-pom` |

## Commit Messages

This project uses [Conventional Commits](https://www.conventionalcommits.org/).
The release pipeline uses commit messages to generate the changelog automatically.

```
feat: add configurable working days per user
fix: correct balance calculation for midnight-spanning entries
chore: remove unused test dependencies
```

## Pull Requests

- Open PRs against `master`
- Make sure `mvn verify` passes locally before pushing
- Keep PRs focused – one logical change per PR
- The PR pipeline runs tests + a Vaadin production build check automatically

## Reporting Issues

Please use [GitHub Issues](https://github.com/luxferre86/chroniqo/issues) and include:
- A clear description of the problem
- Steps to reproduce
- Expected vs. actual behaviour
- Your environment (OS, Java version, browser if UI-related)

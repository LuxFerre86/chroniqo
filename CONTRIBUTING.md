# Contributing to chroniqo

Thank you for your interest in contributing! By participating you agree to abide by our [Code of Conduct](CODE_OF_CONDUCT.md). Here's everything you need to get started.

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

Use the **Bug Report** template when opening a [GitHub Issue](https://github.com/luxferre86/chroniqo/issues/new/choose). Please include:
- A clear description of the problem
- Steps to reproduce
- Expected vs. actual behaviour
- Your environment (OS, Java version, browser if UI-related)

## Requesting Features

For small, focused improvements, use the **Feature Request** template on [GitHub Issues](https://github.com/luxferre86/chroniqo/issues/new/choose).

For larger ideas or open-ended discussions (e.g. "should we support X?"), open a thread in [GitHub Discussions](https://github.com/luxferre86/chroniqo/discussions) instead — this keeps the issue tracker focused on actionable tasks.

When requesting a feature, please describe:
- The problem you are trying to solve
- Your proposed solution (even if rough)
- Any alternatives you have considered

## ☕ Supporting the project

If chroniqo has been useful to you and you'd like to say thanks, you can
[buy me a coffee](https://www.buymeacoffee.com/luxferre86). Not expected, always appreciated.

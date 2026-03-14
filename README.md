# chroniqo

A self-hosted time tracking web application built with Spring Boot and Vaadin.

Track your working hours, manage absences, and monitor your work-time balance — all in a clean, privacy-friendly interface you host yourself.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-green)
![Vaadin](https://img.shields.io/badge/Vaadin-25-blue)
![License](https://img.shields.io/badge/License-AGPL%20v3-blue)

## ✨ Features

- **Time Tracking** – Log daily work entries with start time, end time, and break duration
- **Dashboard** – See today's summary, weekly progress chart, and your current balance at a glance
- **Month View** – Calendar-style overview of all days, worked hours, and daily balance
- **Absence Management** – Record vacation, sick leave, and public holidays
- **Work-Time Balance** – Automatic calculation of overtime and undertime across any period
- **Email Verification** – Account registration with email confirmation
- **Password Reset** – Self-service password reset via email
- **Remember Me** – Persistent login sessions (configurable duration)
- **Docker Ready** – Single-command deployment with Docker Compose

## 🛠 Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 4, Spring Security, Spring Data JPA |
| Frontend | Vaadin 25 (server-side Java UI) |
| Database | MariaDB 11.6 |
| Build | Maven, Vaadin Maven Plugin |
| Deployment | Docker, GitHub Container Registry |

## 🚀 Quick Start

### Option 1: Docker Compose (Recommended)

**Prerequisites:** Docker & Docker Compose installed

1. **Clone the repository:**
```bash
git clone https://github.com/luxferre86/chroniqo.git
cd chroniqo
```

2. **Configure environment:**
```bash
cp .env.example .env
```

Edit `.env` and configure at minimum:
- `APP_BASE_URL` – Your domain (e.g., `https://chroniqo.example.com` or `http://localhost:8180`)
- `GMAIL_USERNAME` and `GMAIL_PASSWORD` – Gmail credentials for sending emails ([Get App Password](https://support.google.com/accounts/answer/185833))
- `REMEMBER_ME_KEY` – Generate with `openssl rand -base64 32`
- Database passwords (use strong values!)

3. **Start the application:**
```bash
docker compose up -d
```

The application will:
- Start MariaDB on port 3306
- Automatically initialize the database schema
- Start chroniqo on port 8180

4. **Access the application:**

Open [http://localhost:8180](http://localhost:8180) and create your first account via the registration page.

5. **View logs (optional):**
```bash
docker compose logs -f chroniqo
```

### Option 2: Manual Setup (Development)

**Prerequisites:**
- Java 21 or higher
- Maven 3.9+
- MariaDB 10.11+
- Node.js 18+ (for Vaadin frontend build)

**Steps:**

1. **Set up MariaDB:**
```bash
mysql -u root -p
CREATE DATABASE chroniqo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'chroniqo'@'localhost' IDENTIFIED BY 'your-password';
GRANT ALL PRIVILEGES ON chroniqo.* TO 'chroniqo'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

2. **Initialize schema:**
```bash
mysql -u chroniqo -p chroniqo < src/main/resources/db/migration/schema.sql
```

3. **Configure environment variables:**

Create a `.env` file or export variables:
```bash
export APP_BASE_URL=http://localhost:8080
export DATABASE_URL=jdbc:mariadb://localhost:3306/chroniqo
export DATABASE_USERNAME=chroniqo
export DATABASE_PASSWORD=your-password
export GMAIL_USERNAME=you@gmail.com
export GMAIL_PASSWORD=your-app-password
export REMEMBER_ME_KEY=$(openssl rand -base64 32)
export REGISTRATION_ENABLED=true
```

4. **Build and run:**
```bash
# Development mode (with hot-reload)
mvn spring-boot:run

# Production build
mvn clean install -Pproduction
java -jar target/chroniqo-*.jar
```

The app will be available at `http://localhost:8080`.

## ⚙️ Configuration Reference

All configuration is done via environment variables. See [`.env.example`](.env.example) for the complete list.

### Essential Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `APP_BASE_URL` | **Required**. Public URL of your application | `https://chroniqo.example.com` |
| `DATABASE_URL` | **Required**. JDBC connection string | `jdbc:mariadb://mariadb:3306/chroniqo` |
| `DATABASE_USERNAME` | **Required**. Database user | `chroniqo` |
| `DATABASE_PASSWORD` | **Required**. Database password | `strong-password` |
| `GMAIL_USERNAME` | **Required**. Gmail address for sending emails | `noreply@example.com` |
| `GMAIL_PASSWORD` | **Required**. Gmail App Password | `abcdefghijklmnop` |
| `REMEMBER_ME_KEY` | **Required**. Secret key (min. 32 chars) | Generate: `openssl rand -base64 32` |

### Optional Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `REGISTRATION_ENABLED` | Allow new user registration | `false` |
| `REMEMBER_ME_SECURE_COOKIE_ENABLED` | Use HTTPS-only cookies | `true` |
| `REMEMBER_ME_COOKIE_DOMAIN` | Cookie domain | `localhost` |
| `TZ` | Application timezone | `UTC` |
| `APP_VERSION` | Displayed version string | `dev` |

## 📊 Database Schema

The database schema is automatically created on first startup. For manual initialization or upgrades, see [`src/main/resources/db/migration/schema.sql`](src/main/resources/db/migration/schema.sql).

**Tables:**
- `users` – User accounts and authentication
- `user_workdays` – Working days configuration per user
- `time_entries` – Daily time tracking records
- `absences` – Vacation, sick leave, and holiday records

## 🐳 Deployment

### Docker (Production)

chroniqo ships as a multi-arch Docker image (`linux/amd64`, `linux/arm64`) published to GitHub Container Registry.

**Pull the latest version:**
```bash
docker pull ghcr.io/luxferre86/chroniqo:latest
```

**Example docker-compose.yaml:**
```yaml
version: '3.8'

services:
  mariadb:
    image: mariadb:11.6
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: chroniqo
      MYSQL_USER: ${DATABASE_USERNAME}
      MYSQL_PASSWORD: ${DATABASE_PASSWORD}
    volumes:
      - mariadb-data:/var/lib/mysql
    
  chroniqo:
    image: ghcr.io/luxferre86/chroniqo:latest
    depends_on:
      - mariadb
    env_file:
      - .env
    ports:
      - "8180:8080"

volumes:
  mariadb-data:
```

### Reverse Proxy (Nginx/Traefik)

For production deployments with HTTPS, use a reverse proxy:

**Nginx Example:**
```nginx
server {
    listen 443 ssl http2;
    server_name chroniqo.example.com;
    
    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;
    
    location / {
        proxy_pass http://localhost:8180;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## 🔧 Troubleshooting

### Email verification emails not received

**Possible causes:**
- Incorrect `GMAIL_USERNAME` or `GMAIL_PASSWORD`
- Using regular Gmail password instead of App Password
- Gmail blocked the login attempt (check Gmail security settings)

**Solutions:**
1. Verify credentials in `.env`
2. Create an [App Password](https://support.google.com/accounts/answer/185833) if not done yet
3. Check application logs: `docker compose logs chroniqo`
4. Check spam folder

### Database connection failed

**Symptoms:** Application fails to start with connection errors

**Solutions:**
1. Verify MariaDB is running: `docker compose ps mariadb`
2. Check database credentials in `.env`
3. For Docker Compose: Use `DATABASE_URL=jdbc:mariadb://mariadb:3306/chroniqo`
4. For external DB: Use correct host and port
5. Check MariaDB logs: `docker compose logs mariadb`

### "baseUrl must be configured" error

**Cause:** `APP_BASE_URL` environment variable is not set

**Solution:** Add to `.env`:
```env
# Development
APP_BASE_URL=http://localhost:8180

# Production
APP_BASE_URL=https://chroniqo.example.com
```

### Login fails after registration

**Cause:** Account is not verified yet (email verification pending)

**Solution:**
1. Check your email for verification link
2. Click the link to activate your account
3. If no email received, see "Email verification emails not received" above

## 🔐 Security Best Practices

- ✅ **Never commit `.env`** – It's in `.gitignore` by default
- ✅ **Use strong passwords** – For database and remember-me key
- ✅ **Enable HTTPS in production** – Set `REMEMBER_ME_SECURE_COOKIE_ENABLED=true`
- ✅ **Restrict registration** – Set `REGISTRATION_ENABLED=false` if invite-only
- ✅ **Keep dependencies updated** – Run `mvn versions:display-dependency-updates`
- ✅ **Backup database regularly** – MariaDB volumes contain all user data

## 💾 Backup & Restore

### Backup

```bash
# Full database backup
docker compose exec mariadb mysqldump -u chroniqo -p chroniqo > chroniqo_backup_$(date +%Y%m%d).sql

# Backup Docker volumes
docker run --rm -v chroniqo_mariadb-data:/data -v $(pwd):/backup ubuntu tar czf /backup/mariadb-data-backup.tar.gz -C /data .
```

### Restore

```bash
# Restore from SQL dump
docker compose exec -T mariadb mysql -u chroniqo -p chroniqo < chroniqo_backup_YYYYMMDD.sql

# Restore Docker volume
docker run --rm -v chroniqo_mariadb-data:/data -v $(pwd):/backup ubuntu tar xzf /backup/mariadb-data-backup.tar.gz -C /data
```

## 🤝 Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request.

**Development Setup:**
See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed local development instructions.

## 📄 License

This project is licensed under the **GNU Affero General Public License v3.0 (AGPL-3.0)**.

You are free to use, modify, and self-host this software. If you use it to provide a network service, you must make the complete source code — including any modifications — available to your users under the same license.

For commercial use without these obligations, contact the author via GitHub for a commercial license. See [LICENSE](LICENSE) for full terms.

## 🙏 Support

- **Issues:** [GitHub Issues](https://github.com/luxferre86/chroniqo/issues)
- **Discussions:** [GitHub Discussions](https://github.com/luxferre86/chroniqo/discussions)
- **Security:** See [SECURITY.md](SECURITY.md) for vulnerability reporting

## 🗺 Roadmap

- [ ] Mobile app (iOS/Android)
- [ ] API for third-party integrations
- [ ] Export to CSV/Excel
- [ ] Multi-user team management
- [ ] Reporting and analytics

---

**Made with ❤️ by [LuxFerre86](https://github.com/luxferre86)**

# Security Policy

## Supported Versions

We actively support the following versions with security updates:

| Version | Supported          |
| ------- | ------------------ |
| 0.x.x   | :white_check_mark: |

## Reporting a Vulnerability

We take security vulnerabilities seriously. If you discover a security issue in chroniqo, please help us by reporting it responsibly.

### 🔒 Please DO NOT

- **Do NOT** open a public GitHub issue for security vulnerabilities
- **Do NOT** discuss the vulnerability publicly until it has been fixed

### ✅ Please DO

**Preferred Method:** Use [GitHub Security Advisories](https://github.com/luxferre86/chroniqo/security/advisories/new)

**Alternative Method:** Email the maintainer directly
- **Email:** Use the email linked on the [GitHub profile](https://github.com/luxferre86)
- **Subject:** `[SECURITY] Chroniqo Vulnerability Report`

### What to Include

Please provide as much information as possible:

1. **Description** of the vulnerability
2. **Steps to reproduce** the issue
3. **Impact** – What could an attacker do?
4. **Affected versions** (if known)
5. **Suggested fix** (if you have one)
6. **Your contact information** (for follow-up questions)

### Response Timeline

- **Acknowledgment:** Within 48 hours of receipt
- **Initial Assessment:** Within 1 week
- **Fix Development:** Depends on severity (critical issues prioritized)
- **Disclosure:** After fix is deployed and users have had time to update

## Security Best Practices for Users

When deploying chroniqo, please follow these security recommendations:

### 🔐 Configuration

- ✅ **Use strong passwords** – For database, remember-me key, and user accounts
- ✅ **Enable HTTPS** – Set `REMEMBER_ME_SECURE_COOKIE_ENABLED=true` in production
- ✅ **Restrict registration** – Set `REGISTRATION_ENABLED=false` if invite-only
- ✅ **Keep secrets secret** – Never commit `.env` to version control
- ✅ **Use Gmail App Passwords** – Not your regular Gmail password

### 🔒 Deployment

- ✅ **Use reverse proxy** – Nginx or Traefik with proper TLS configuration
- ✅ **Firewall rules** – Only expose necessary ports (typically 80/443 for web, not 3306 for DB)
- ✅ **Regular updates** – Keep Docker images and dependencies up to date
- ✅ **Network isolation** – Use Docker networks to isolate database from public internet
- ✅ **Backup regularly** – Protect against data loss and ransomware

### 🛡️ Monitoring

- ✅ **Check logs** – Review `docker compose logs` for suspicious activity
- ✅ **Monitor failed logins** – Chroniqo includes brute-force protection, but monitor logs
- ✅ **Update dependencies** – Run `mvn versions:display-dependency-updates` regularly

## Known Security Features

chroniqo includes the following security measures:

- ✅ **BCrypt password hashing** (strength 12)
- ✅ **Brute-force protection** (5 attempts → 15 min lockout)
- ✅ **Email verification** for new accounts
- ✅ **Password reset tokens** with 1-hour expiry
- ✅ **Remember-me tokens** with SHA-256 hashing
- ✅ **SQL injection protection** via JPA/Hibernate
- ✅ **XSS protection** via Vaadin framework
- ✅ **CSRF protection** via Spring Security

## Security Hardening Checklist

Before deploying to production, ensure you have:

- [ ] Changed all default passwords in `.env`
- [ ] Generated strong `REMEMBER_ME_KEY` (32+ chars)
- [ ] Set `REMEMBER_ME_SECURE_COOKIE_ENABLED=true`
- [ ] Configured valid `APP_BASE_URL` (not localhost)
- [ ] Enabled HTTPS via reverse proxy
- [ ] Restricted database access (not exposed to internet)
- [ ] Set up regular backups
- [ ] Reviewed logs for initial errors/warnings
- [ ] Tested email functionality (verification & password reset)
- [ ] Tested login/logout and remember-me functionality

## Vulnerability Disclosure Policy

When a security vulnerability is reported and fixed:

1. **Fix Development** – We develop and test a fix
2. **Release** – We release a patched version
3. **User Notification** – We notify users via release notes and security advisory
4. **Public Disclosure** – After 30 days (or when most users have updated), we publish details

## Security Hall of Fame

We thank the following security researchers for responsibly disclosing vulnerabilities:

_(No vulnerabilities reported yet)_

---

**Thank you for helping keep chroniqo secure!** 🛡️

# Changelog

## [2026.03.16] - 2026-03-11

### Fixed
- changed app name consistent in lowercase


### Maintenance
- Merge pull request #29 from LuxFerre86/fix/app-name-in-lowercase

## [2026.03.15] - 2026-03-11

### Fixed
- missing equal sign in .env.example


### Maintenance
- Merge pull request #28 from LuxFerre86/fix/typo-env-example

## [2026.03.14] - 2026-03-11

### Added
- user registration is now disabled by default and can be enabled/dsiabled via env argument


### Maintenance
- Merge pull request #27 from LuxFerre86/feature/registration-disbaled-by-default

## [2026.03.13] - 2026-03-11

### Added
- weekly target hours can now be configured in settings with values 0-80


### Maintenance
- Merge pull request #26 from LuxFerre86/feature/weekly-target-hourse-bound-validation

## [2026.03.12] - 2026-03-11

### Fixed
- avoid too much user details in error notifications/logs


### Maintenance
- Merge pull request #25 from LuxFerre86/fix/avoid-too-much-user-details-in-exceptions-and-logs

## [2026.03.11] - 2026-03-11

### Fixed
- avoid full email addresses in app logs


### Maintenance
- Merge pull request #24 from LuxFerre86/fix/app-log-privacy

## [2026.03.10] - 2026-03-11

### Added
- added account lock after 5 unsuccessful login attempts


### Maintenance
- Merge pull request #23 from LuxFerre86/feature/improved-login-security
- chore: move user services

## [2026.03.9] - 2026-03-09

### Maintenance
- Merge pull request #22 from LuxFerre86/chore/go-public
- chore: added Trivy Scan for PR build
- chore: removed unused dependencies
- chore: added LICENSE and README.md
- chore: CONTRIBUTING.md
- chore: improved .gitignore
- chore: remember-me config as env parameters

## [2026.03.8] - 2026-03-08

### Fixed
- using jackson-core in 3.1.0 due to CVE-2026-29062


### Maintenance
- Merge pull request #21 from LuxFerre86/fix/CVE-2026-29062

## [2026.03.7] - 2026-03-08

### Added
- improved docker compose wit healthcheck


### Maintenance
- Merge pull request #20 from LuxFerre86/feature/improve-docker-compose

## [2026.03.6] - 2026-03-08

### Added
- improved PR and release pipelines


### Maintenance
- Merge pull request #19 from LuxFerre86/feature/improve-pr-and-release-pipelines

## [2026.03.5] - 2026-03-08

### Added
- improved weekly progress visualization


### Maintenance
- Merge pull request #18 from LuxFerre86/feature/improve-weekly-progress-visualization

## [2026.03.4] - 2026-03-08

### Added
- improved workday determination


### Maintenance
- Merge pull request #17 from LuxFerre86/feature/improve-workday-determination

## [2026.03.3] - 2026-03-08

### Added
- improved test coverage


### Maintenance
- Merge pull request #16 from LuxFerre86/feature/test-coverage

## [2026.03.2] - 2026-03-03

### Fixed
- show always balance and hours worked zero if a minus value was calculated / also not show worked hours and balance if weekend or absence


### Maintenance
- Merge pull request #15 from LuxFerre86/fix/calculate-correct-balance-and-worked-hours

## [2026.03.1] - 2026-03-03

### Added
- Refactoring of TimeEntryService and AbsenceService and create a wrapping TimeTrackingService feat: merged UserService and AuthenticationService


### Maintenance
- Merge pull request #14 from LuxFerre86/feature/service-layer-refactoring-part-2

## [2026.03.0] - 2026-03-01

### Added
- replaces yearService, monthService and dashboardService with new summaryService


### Fixed
- code inspections


### Maintenance
- Merge pull request #13 from LuxFerre86/feature/service-layer-refactoring

## [2026.02.11] - 2026-02-28

### Added
- successful authentication via remember-me leads to an update of the last login timestamp


### Maintenance
- Merge pull request #12 from LuxFerre86/feature/remember-me-with-last-login-audit

## [2026.02.10] - 2026-02-27

### Added
- all texts and dates on views consistently using UK locale


### Maintenance
- Merge pull request #11 from LuxFerre86/feature/texts-english

## [2026.02.9] - 2026-02-27

### Added
- upgrade spring-boot to 4.0.3 and vaadin to 25.0.6


### Maintenance
- Merge pull request #10 from LuxFerre86/feature/dependency-upgrade

## [2026.02.8] - 2026-02-27

### Added
- add remember-me check box to login view


### Maintenance
- Merge pull request #9 from LuxFerre86/feature/remember-me

## [2026.02.7] - 2026-02-23

### Added
- showing app version in navbar


### Maintenance
- Merge pull request #8 from LuxFerre86/feature/navbar-with-app-version

## [2026.02.6] - 2026-02-22

### Fixed
- make statistic card responsive for monthly view


### Maintenance
- Merge pull request #7 from LuxFerre86/feature/statistic-card-responsive

## [2026.02.5] - 2026-02-22

### Added
- added new Dashboard view as root view


### Maintenance
- Merge pull request #6 from LuxFerre86/feature/dashboard

## [2026.02.4] - 2026-02-22

### Fixed
- loginView is shown without exception


### Maintenance
- Merge pull request #5 from LuxFerre86/feature/login-and-register

## [2026.02.3] - 2026-02-22

### Maintenance
- Merge pull request #4 from LuxFerre86/feature/login-and-register
- Added new authentication feature with login, register, password reset and password forget functionality.

## [2026.02.2] - 2026-02-18

### Fixed
- statistics also considering today's time entry


### Maintenance
- Merge pull request #3 from LuxFerre86/feature/time-stats

## [2026.02.1] - 2026-02-15

### Added
- init UI locale from users browser locale with Locale.UK fallback


### Maintenance
- Merge pull request #2 from LuxFerre86/feature/user-locale

## [2026.02.0] - 2026-02-15

### Added
- improved GitHub production release


### Maintenance
- Merge pull request #1 from LuxFerre86/feature/responsive-design
- Redesign
- Initial commit
- Initial commit
- Initial commit
- Initial commit
- Initial commit
- Initial commit
- Initial commit

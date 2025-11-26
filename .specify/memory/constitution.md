<!--
================================================================================
SYNC IMPACT REPORT
================================================================================
Version Change: 1.0.0 → 1.1.0 (Minor)
Modified Principles: None
Added Sections:
  - Core Principles: VI. Documentation Language Standards (NEW)
Removed Sections: None

Templates Requiring Updates:
  - .specify/templates/plan-template.md: ✅ Compatible (Constitution Check section exists)
  - .specify/templates/spec-template.md: ⚠ Pending - May need zh-TW examples
  - .specify/templates/tasks-template.md: ✅ Compatible (Phase structure supports principles)

Follow-up TODOs:
  - Review spec-template.md for zh-TW compliance examples
================================================================================
-->

# ETF Holdings Tracker Constitution

## Core Principles

### I. Code Quality Standards

**Non-Negotiable Rules**:
- All code MUST follow established coding conventions for the language (Java, TypeScript, etc.)
- Methods MUST NOT exceed 50 lines; classes MUST NOT exceed 500 lines
- Cyclomatic complexity MUST NOT exceed 10 per method
- All public APIs MUST have comprehensive documentation (Javadoc/JSDoc)
- Code duplication MUST be refactored when detected (DRY principle)
- All commits MUST pass static analysis (linting, formatting) before merge

**Rationale**: Maintainability and readability are critical for long-term project health.
Smaller, focused units reduce cognitive load and enable easier testing and debugging.

### II. Testing Discipline

**Non-Negotiable Rules**:
- All new features MUST include unit tests with minimum 80% code coverage
- All public API changes MUST include integration tests
- Test-Driven Development (TDD) is RECOMMENDED: write failing tests before implementation
- All tests MUST be independent and repeatable (no external service dependencies in unit tests)
- Mock external dependencies (APIs, databases) in unit tests; use real dependencies only in integration tests
- All critical business logic MUST have edge case coverage

**Test Categories**:
- **Unit Tests**: Isolated component tests, fast execution (<100ms per test)
- **Integration Tests**: API endpoints, database operations, external service interactions
- **Contract Tests**: API contract validation for external integrations

**Rationale**: Comprehensive testing prevents regression bugs and enables confident refactoring.
Testing at appropriate levels ensures both speed and reliability.

### III. User Experience Consistency

**Non-Negotiable Rules**:
- API responses MUST follow consistent JSON structure across all endpoints
- Error responses MUST include: error code, message, timestamp, and request correlation ID
- All date/time values MUST use ISO 8601 format (UTC timezone for storage)
- Pagination MUST be consistent across all list endpoints (page, size, totalElements, totalPages)
- API versioning MUST be explicit (URL path or header-based)
- All user-facing messages MUST support internationalization (i18n)

**Response Format Standard**:
```json
{
  "success": true,
  "data": {...},
  "meta": {"timestamp": "...", "correlationId": "..."},
  "errors": null
}
```

**Rationale**: Consistent user experience reduces integration friction and
improves developer productivity when consuming APIs.

### IV. Performance Requirements

**Non-Negotiable Rules**:
- API response time MUST be <500ms for p95 under normal load
- Database queries MUST be optimized with proper indexing (no full table scans for common operations)
- Memory usage MUST NOT exceed defined limits (configurable per environment)
- Batch operations MUST support pagination to prevent memory overflow
- External API calls MUST implement timeout (default: 10 seconds) and retry logic
- Heavy computations MUST be offloaded to background jobs when applicable

**Performance Targets**:
- API latency: p50 <100ms, p95 <500ms, p99 <1000ms
- Throughput: Support 100 concurrent requests per instance
- Data refresh: ETF holdings data MUST be refreshable within acceptable time windows

**Rationale**: Reliable performance ensures system usability and user trust.
Clear targets enable objective performance validation.

### V. Observability & Reliability

**Non-Negotiable Rules**:
- All services MUST emit structured logs (JSON format) with correlation IDs
- Critical operations MUST include metrics (latency, error rate, throughput)
- Health check endpoints MUST be implemented for all services
- Configuration MUST be externalized (environment variables or config files)
- Sensitive data MUST NOT appear in logs or error messages
- All external integrations MUST have circuit breaker patterns

**Logging Levels**:
- ERROR: System failures requiring immediate attention
- WARN: Potentially harmful situations
- INFO: High-level application flow
- DEBUG: Detailed diagnostic information (disabled in production)

**Rationale**: Observability enables rapid troubleshooting and proactive issue detection.
Reliability patterns prevent cascading failures.

### VI. Documentation Language Standards

**Non-Negotiable Rules**:
- This constitution document MUST remain in English only
- All specifications (`specs/`) MUST be written in Traditional Chinese (zh-TW)
- All implementation plans (`plan.md`) MUST be written in Traditional Chinese (zh-TW)
- All user-facing documentation (README, guides, API docs) MUST be written in Traditional Chinese (zh-TW)
- Code comments and inline documentation MAY use English for technical terms
- Commit messages SHOULD use English for consistency with global tooling
- Variable names, function names, and code identifiers MUST use English

**Document Categories**:
- **English Only**: Constitution, code, commit messages, technical configurations
- **Traditional Chinese (zh-TW)**: Specifications, plans, user guides, API documentation, error messages for end users

**Rationale**: Traditional Chinese ensures clear communication for the primary user base.
English in code and constitution maintains compatibility with development tools and global standards.

## Development Workflow

### Code Review Requirements
- All code changes MUST be reviewed by at least one team member
- Reviews MUST verify compliance with constitution principles
- Automated checks (tests, linting, coverage) MUST pass before review

### Branch Strategy
- Feature branches MUST be created from `main`
- Branch naming: `feature/###-description`, `bugfix/###-description`, `hotfix/###-description`
- Merge MUST use squash commits with meaningful commit messages

### Quality Gates
- Pre-commit: Linting, formatting validation
- Pre-merge: All tests pass, code coverage threshold met, peer review approved
- Post-merge: Integration tests, deployment verification

## Governance

### Amendment Procedure
1. Propose changes with rationale in a dedicated PR
2. Review period: minimum 48 hours for team feedback
3. Approval: Requires consensus or project lead decision
4. Documentation: Update this constitution with version increment and changelog

### Versioning Policy
- **MAJOR**: Backward-incompatible governance/principle removals or redefinitions
- **MINOR**: New principle/section added or materially expanded guidance
- **PATCH**: Clarifications, wording, typo fixes, non-semantic refinements

### Compliance Review
- All PRs/reviews MUST verify compliance with constitution principles
- Complexity violations MUST be justified in the PR description
- Runtime development guidance available in project documentation

### Exceptions
- Emergency hotfixes MAY bypass certain gates with post-facto review
- All exceptions MUST be documented and reviewed within 24 hours

**Version**: 1.1.0 | **Ratified**: 2025-11-26 | **Last Amended**: 2025-11-26

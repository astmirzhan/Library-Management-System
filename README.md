# Library Management System

## 📖 Overview

A web application for managing a library's catalog and borrowing workflow, built with **Spring MVC (without Spring Boot)** and **plain JDBC** (no ORM). It serves three roles:

- **Readers** — browse the catalog, filter/search, borrow books (with a self-chosen due date), track loans, write and edit reviews.
- **Librarians** — manage the book inventory (add/edit/delete books and copies), approve/reject borrow requests, register returns with copy condition, track overdue loans.
- **Admins** — analytics dashboard, manage librarian accounts, view-only access to the catalog.

## 🛠 Tech Stack

| Layer          | Technology                                                                 |
|----------------|----------------------------------------------------------------------------|
| Language       | Java 11                                                                    |
| Web            | Spring MVC 5 (Spring Core IoC/DI, DispatcherServlet)                        |
| Security       | Spring Security 5 (role-based URL rules + `@PreAuthorize`, CSRF), BCrypt    |
| Persistence    | Plain JDBC (no ORM), PostgreSQL, **custom thread-safe connection pool**     |
| AOP            | Spring AOP / AspectJ (`LoggingAspect`)                                      |
| View           | Thymeleaf + custom CSS + Bootstrap grid (responsive)                       |
| Validation     | Spring Validation (`@Valid`, JSR-303) + HTML5 client-side                  |
| Logging        | Log4j2 (console + rolling file)                                            |
| i18n           | Spring MessageSource + LocaleChangeInterceptor (EN / RU, in-app switcher)  |
| Testing        | JUnit 5 + Mockito + JaCoCo                                                 |
| Build / Runtime| Maven (WAR) → Tomcat 9 via Cargo Maven plugin                              |

## ✨ Features

- Book catalog with **search** (title / author / ISBN), **genre / author / availability filters**, and **pagination**
- Borrowing lifecycle: **request (reader-chosen due date) → librarian approval → return confirmation (copy condition) → overdue fine**
- Librarian can **reject** requests (order → `REJECTED`) and manage inventory (add/edit/delete books, add copies)
- Book reviews and average ratings (create + edit; only for books you borrowed)
- **Role-based access** enforced by both Spring Security and a Spring `AuthInterceptor`
- **Internationalization** (English / Russian) with an in-app language switcher (`?lang=en|ru`)
- **CSRF protection** on all state-changing forms
- Session-based authentication integrated with the Spring Security context; passwords hashed with **BCrypt**

## 📁 Package Structure

```
com.library
├── aspect        # LoggingAspect (@Aspect — AOP logging of service calls)
├── config        # WebConfig, AppInitializer, SecurityConfig, SecurityWebInitializer,
│                 #   DatabaseConnection (Singleton), ConnectionPool (custom pool)
├── controller    # @Controller endpoints + GlobalExceptionHandler (@ControllerAdvice)
├── dto           # Form DTOs with JSR-303 validation (RegisterForm, ProfileForm)
├── interceptor   # AuthInterceptor, LoggingInterceptor
├── model         # POJO entities (User, Book, BookCopy, BorrowRecord, ...)
├── util          # ConfigLoader (Singleton), PasswordUtil (BCrypt)
├── dao           # DAO interfaces (BaseDAO<T,ID> + one interface per entity)
│   └── impl      # @Repository JDBC implementations
└── service       # Service interfaces (User, Book, Borrow, Review)
    └── impl       # @Service implementations
```

## 🗄 Database

PostgreSQL, **10 tables** in 3NF with foreign keys throughout:

- Core: `user`, `book`, `book_copy`, `borrow_record`, `review`, `author`, `genre`, `publisher`
- Junction (many-to-many): `book_author`, `book_genre`

Enum types: `user_role`, `borrow_status`, `copy_condition`.

## 🚀 Getting Started

### Prerequisites

- JDK 11+
- Maven 3.6+
- PostgreSQL 12+ (running locally on port 5432)

### Database setup

A PostgreSQL database is expected on `localhost:5432`. Connection settings (URL, user, password) and the
connection-pool size live in `src/main/resources/application.properties`.

### Build & Run

```bash
mvn clean package        # compiles, runs tests, builds target/library-management.war
mvn test                 # run unit tests
mvn jacoco:report        # coverage report → target/site/jacoco/index.html
mvn cargo:run            # downloads Tomcat 9 and deploys the app
```

Then open **http://localhost:8080/** and sign in.

> Note: the app runs on **Tomcat 9** (Servlet 3.1+). Tomcat 7 is not supported — Spring 5's static-resource handler
> requires `HttpServletResponse.setContentLengthLong()`, which only exists in Servlet 3.1+.

## 👤 Demo Accounts

| Role      | Email            | Password       |
|-----------|------------------|----------------|
| Admin     | `admin@lib.org`  | `admin123`     |
| Librarian | `jane@lib.org`   | `librarian123` |
| Reader    | `alice@mail.com` | `reader123`    |

> Demo credentials are intentionally committed for local evaluation only — this is an educational project with no
> production database.

## ✅ Implemented Requirements

- **Spring MVC** controllers (`@Controller`, `@RequestMapping`) + `DispatcherServlet` via `AppInitializer`
- **Layered architecture**: Controller → Service → DAO → Database, each layer as **interface + implementation**
- **DAO pattern**: `BaseDAO<T, ID>` + per-entity interfaces with `@Repository` JDBC implementations (`dao.impl`)
- **Design patterns**: DAO, Singleton (`DatabaseConnection`, `ConfigLoader`), **Decorator** (`PooledConnection` in the connection pool)
- **Custom thread-safe connection pool** (two `BlockingQueue`s; `PooledConnection.close()` returns the connection to the pool)
- **Plain JDBC** with `PreparedStatement` everywhere (SQL-injection safe)
- **Spring Security**: role-based URL access (`/admin/**` → ADMIN, `/librarian/**` → LIBRARIAN/ADMIN) + method-level `@PreAuthorize`
- **CSRF protection** enabled; tokens auto-injected into all Thymeleaf forms
- **Password hashing** with BCrypt (`PasswordUtil` / jBCrypt)
- **AOP**: `LoggingAspect` logs service-layer calls, timing and exceptions
- **Spring Interceptors**: `AuthInterceptor` (session/role checks) + `LoggingInterceptor`
- **Validation**: server-side **Spring Validation** (`@Valid` DTOs with `@NotBlank`/`@Email`/`@Size`) + HTML5 client-side
- **Pagination** for catalog, users and borrowing records
- **i18n**: English / Russian message bundles with an in-app language switcher
- **Unit tests**: JUnit 5 + Mockito, **> 50% coverage** on both the Service and DAO layers; **JaCoCo** report
- **Global exception handling** via `@ControllerAdvice` with a user-friendly error page
- **Logging** with Log4j2 across all layers (console + rolling file)
- **Configuration** externalized in `application.properties` (DB, connection pool, business rules: borrowing limit, borrow period, fines, pagination)
- **JavaDoc** on public Service and DAO methods

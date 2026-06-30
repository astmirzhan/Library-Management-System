# Library Management System

## 📖 Overview

A web application for managing a library's catalog and borrowing workflow, built with **Spring MVC (without Spring Boot)
** and **plain JDBC**. It serves three roles: **Readers** browse the catalog and request books, **Librarians** approve
borrow requests and manage inventory, and **Admins** manage users and view system statistics.

## 🛠 Tech Stack

| Layer       | Technology                                                                     |
|-------------|--------------------------------------------------------------------------------|
| Language    | Java 11                                                                        |
| Web         | Spring MVC 5 (Spring Core IoC/DI, DispatcherServlet)                           |
| Persistence | Plain JDBC (no ORM), PostgreSQL                                                |
| View        | Thymeleaf + custom CSS                                                         |
| Logging     | Log4j2                                                                         |
| Validation  | HTML5 (client) + manual service-layer checks; hibernate-validator on classpath |
| Build       | Maven (WAR)                                                                    |
| Runtime     | Tomcat 9 via Cargo Maven plugin                                                |
| Security    | BCrypt password hashing (jBCrypt)                                              |
| i18n        | Spring MessageSource + LocaleChangeInterceptor (en / ru)                       |

## ✨ Features

- Book catalog with **search**, **genre / availability filters**, and **pagination**
- Borrowing lifecycle: **request → librarian approval → return** (with overdue fine calculation)
- Role-based access (Reader / Librarian / Admin) enforced via a Spring **Interceptor**
- Book reviews and average ratings
- Internationalization (English / Russian, switch via `?lang=ru`)
- Session-based authentication with hashed passwords

## 📁 Package Structure

```
com.library
├── config        # Spring config (WebConfig, AppInitializer), DatabaseConnection
├── controller    # @Controller endpoints + GlobalExceptionHandler (@ControllerAdvice)
├── service       # Business logic (User, Book, Borrow, Review services)
├── dao           # Data access (BaseDAO + one @Repository DAO per entity)
├── model         # POJO entities (User, Book, BookCopy, BorrowRecord, ...)
├── interceptor   # AuthInterceptor, LoggingInterceptor
└── util          # ConfigLoader (Singleton), PasswordUtil (BCrypt)
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

PostgreSQL database is already configured and running locally for this demo. For production setup, see
`application.properties` for connection details.

### Build & Run

```bash
mvn clean package        # builds target/library-management.war
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
- **Layered architecture**: Controller → Service → DAO → Database
- **DAO pattern** with `BaseDAO<T, ID>` interface and `@Repository` implementations
- **Design patterns**: DAO + Singleton (`DatabaseConnection`, `ConfigLoader` as `@Bean`)
- **Plain JDBC** with `PreparedStatement` everywhere (SQL-injection safe)
- **Password hashing** with BCrypt (`PasswordUtil`)
- **Pagination** for the catalog (`page` parameter, limit/offset)
- **Spring Interceptors**: `AuthInterceptor` (session/role checks) + `LoggingInterceptor`
- **Global exception handling** via `@ControllerAdvice` with user-friendly error page
- **Logging** with Log4j2 across all layers (console + rolling file)
- **Validation**: HTML5 client-side + manual server-side checks with clear messages
- **i18n**: English / Russian message bundles
- **Configuration** externalized in `application.properties` (DB + business rules: borrowing limit, borrow period,
  fines, pagination)
- **JavaDoc** on public Service and DAO methods

## 📝 Notes / Known Limitations

- Unit tests with 50%+ coverage planned for final defense (JUnit 5 + Mockito + JaCoCo are already configured in
  `pom.xml`).

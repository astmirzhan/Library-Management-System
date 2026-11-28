<div align="center">

<img src="docs/banner.svg" alt="Library Management System: catalog, borrowing and reviews for readers, librarians and admins" width="100%" />

# Library Management System

**A full-stack library web app with three roles: readers borrow and review books, librarians run the inventory and loans, admins see the numbers. Built on Spring MVC and plain JDBC, no Spring Boot, no ORM.**

<p align="center">
  <img src="https://img.shields.io/badge/Java-11-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 11" />
  <img src="https://img.shields.io/badge/Spring_MVC-5-6DB33F?style=for-the-badge&logo=spring&logoColor=white" alt="Spring MVC 5" />
  <img src="https://img.shields.io/badge/Spring_Security-BCrypt_·_CSRF-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" alt="Spring Security" />
  <img src="https://img.shields.io/badge/PostgreSQL-plain_JDBC-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL with plain JDBC" />
  <img src="https://img.shields.io/badge/Thymeleaf-EN_·_RU-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white" alt="Thymeleaf, English and Russian" />
  <img src="https://img.shields.io/badge/Tests-JUnit_5_·_Mockito-25A162?style=for-the-badge&logo=junit5&logoColor=white" alt="JUnit 5 and Mockito" />
</p>

</div>

---

## 📚 What it is

A web app that covers the whole life of a library book: in the catalog, requested, approved, returned (with its
condition checked) and reviewed. Each role sees only what it needs.

| Role | What they can do |
| --- | --- |
| 📖 **Reader** | Search and filter the catalog, request a book with their own due date, track loans, write and edit reviews for books they borrowed |
| 🗂️ **Librarian** | Add, edit and delete books and copies, approve or reject requests, register returns with copy condition, track overdue loans |
| 📊 **Admin** | Analytics dashboard, manage librarian accounts, view-only catalog |

## 📸 Screenshots

<img src="docs/readme/screens.png" alt="Reader discover page and catalog, librarian dashboard, admin dashboard" width="100%" />

## ✨ Features

- **🔎 Catalog**: search by title, author or ISBN; filter by genre, author and availability; pagination everywhere.
- **🔁 Borrowing**: up to 5 active loans, 14 days by default and never more than 60; overdue fines per day.
- **⭐ Reviews and ratings**: only readers who actually borrowed a book can review it.
- **🌐 English and Russian**: in-app language switcher (`?lang=en|ru`).
- **🔒 Security**: role-based URLs plus method-level `@PreAuthorize`, CSRF on every form, BCrypt passwords.
- **🧯 Friendly errors**: one `@ControllerAdvice` turns exceptions into a readable error page.

## 🔁 A book's journey

```mermaid
%%{init: {"theme": "base", "themeVariables": {"primaryColor": "#fffdf8", "primaryTextColor": "#17231d", "primaryBorderColor": "#285c46", "lineColor": "#285c46", "secondaryColor": "#f5f3ed", "tertiaryColor": "#fdf0da", "fontFamily": "monospace"}}}%%
stateDiagram-v2
  direction LR
  [*] --> Requested: reader picks a due date
  Requested --> Rejected: librarian rejects
  Requested --> Borrowed: librarian approves
  Borrowed --> Overdue: due date passes
  Borrowed --> Returned: librarian confirms return
  Overdue --> Returned: return + daily fine
  Returned --> Reviewed: reader leaves a review
  Rejected --> [*]
  Reviewed --> [*]
```

## 🏗️ How it's built

```mermaid
%%{init: {"theme": "base", "themeVariables": {"primaryColor": "#fffdf8", "primaryTextColor": "#17231d", "primaryBorderColor": "#285c46", "lineColor": "#285c46", "secondaryColor": "#e9a23b", "tertiaryColor": "#f5f3ed", "fontFamily": "monospace"}}}%%
flowchart LR
  B["🌐 Browser<br/>Thymeleaf pages"] --> SEC["🔒 Spring Security<br/>roles · CSRF"]
  SEC --> INT["AuthInterceptor<br/>LoggingInterceptor"]
  INT --> C["@Controller"]
  C --> S["@Service<br/>business rules"]
  S --> D["@Repository DAO<br/>PreparedStatement"]
  D --> POOL["Custom connection pool<br/>2 BlockingQueues"]
  POOL --> DB[("🐘 PostgreSQL<br/>10 tables · 3NF")]
  AOP["LoggingAspect<br/>AOP"] -.-> S
```

| Layer | Technology |
| --- | --- |
| Language | Java 11 |
| Web | Spring MVC 5, DispatcherServlet configured in code (`AppInitializer`) |
| Security | Spring Security 5, BCrypt, CSRF, `@PreAuthorize` |
| Persistence | Plain JDBC with `PreparedStatement`, PostgreSQL, custom thread-safe connection pool |
| Cross-cutting | Spring AOP (`LoggingAspect`), interceptors, Log4j2 (console + rolling file) |
| View | Thymeleaf, custom CSS, Bootstrap grid |
| Validation | JSR-303 DTOs with `@Valid`, plus HTML5 checks |
| Build and run | Maven (WAR) → Tomcat 9 via the Cargo plugin |

**Design patterns**: DAO (`BaseDAO<T, ID>` + one interface per entity), Singleton (`DatabaseConnection`,
`ConfigLoader`), Decorator (`PooledConnection.close()` returns the connection to the pool instead of closing it).
Every layer is an interface plus an implementation: Controller → Service → DAO → Database.

<details>
<summary><b>📁 Package structure</b></summary>

```
com.library
├── aspect        LoggingAspect
├── config        WebConfig, AppInitializer, SecurityConfig, SecurityWebInitializer,
│                 DatabaseConnection, ConnectionPool
├── controller    @Controller endpoints + GlobalExceptionHandler
├── dto           RegisterForm, ProfileForm (JSR-303)
├── interceptor   AuthInterceptor, LoggingInterceptor
├── model         User, Book, BookCopy, BorrowRecord, Review, Author, Genre, Publisher
├── util          ConfigLoader, PasswordUtil (BCrypt)
├── dao           BaseDAO<T,ID> + interfaces
│   └── impl      JDBC implementations
└── service       User, Book, Borrow, Review services
    └── impl
```

</details>

## 🗄️ Database

PostgreSQL, **10 tables in 3NF** with foreign keys throughout.

| Group | Tables |
| --- | --- |
| Core | `user`, `book`, `book_copy`, `borrow_record`, `review`, `author`, `genre`, `publisher` |
| Many-to-many | `book_author`, `book_genre` |
| Enum types | `user_role`, `borrow_status`, `copy_condition` |

## ✅ Tests

**82 tests** with JUnit 5 and Mockito across the service layer (books, borrowing, reviews) and the DAO layer,
with a JaCoCo coverage report. Coverage is above 50% on both the service and DAO layers.

## 🛠️ Run it

You need JDK 11+, Maven 3.6+ and PostgreSQL 12+ on `localhost:5432`. Connection settings, the pool size and the
borrowing rules live in `src/main/resources/application.properties`.

```bash
mvn clean package     # compile, test, build target/library-management.war
mvn test              # unit tests
mvn jacoco:report     # coverage → target/site/jacoco/index.html
mvn cargo:run         # download Tomcat 9 and deploy
```

Then open **http://localhost:8080/** and sign in with a demo account:

| Role | Email | Password |
| --- | --- | --- |
| Admin | `admin@lib.org` | `admin123` |
| Librarian | `jane@lib.org` | `librarian123` |
| Reader | `alice@mail.com` | `reader123` |

> Demo credentials are committed on purpose for local evaluation; this is an educational project with no production database.
> The app needs **Tomcat 9** (Servlet 3.1+): Spring 5's static resource handler calls `setContentLengthLong()`, which Tomcat 7 lacks.

<div align="center">

**Library Management System** · MIT · every book comes back 📚

</div>

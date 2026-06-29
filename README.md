# PromptVault

PromptVault is a Spring Boot web application for storing, organising and reviewing AI prompts. Standard users can keep a private library of prompts, share selected prompts with the community and submit any of their prompts to a simulated AI assistant. Administrators manage the user base, the prompt categories and the policy keywords that automatically flag potentially sensitive prompts for review.

The application uses server side rendering with Thymeleaf, a MySQL database for persistence and a simulated AI service, so no external AI API or paid key is required to run it.

## Quick start (no database setup)

If you just want to try the app on your own machine, you do **not** need to install or configure MySQL. The project ships with a built in `demo` profile that runs against an embedded in-memory database and seeds the demo accounts and sample data automatically.

```bash
# 1. Make sure you are on the branch that contains the fixes
git checkout fixes
git pull

# 2. Run the app with the demo profile (Java 17 and Maven required)
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

Then open `http://localhost:8080/login` and sign in with:

| Role  | Username | Password      |
|-------|----------|---------------|
| Admin | `admin`  | `admin123`    |
| User  | `alice`  | `password123` |
| User  | `bob`    | `password123` |

That is all that is needed to log in and explore every feature. The demo database lives in memory, so it resets each time you restart the app. For the graded submission with persistent MySQL storage, follow [Database setup](#database-setup) and [How to run](#how-to-run) below.

> Tip: while running in demo mode you can also open `http://localhost:8080/h2-console` to inspect the embedded database (JDBC URL `jdbc:h2:mem:promptvault`, user `sa`, no password).

## Table of contents

- [Quick start (no database setup)](#quick-start-no-database-setup)
- [Key features](#key-features)
- [Technology stack](#technology-stack)
- [Project structure](#project-structure)
- [Domain model](#domain-model)
- [Prerequisites](#prerequisites)
- [Database setup](#database-setup)
- [Configuration](#configuration)
- [How to run](#how-to-run)
- [Demo accounts](#demo-accounts)
- [Sample data](#sample-data)
- [Using the application](#using-the-application)
- [How prompt flagging works](#how-prompt-flagging-works)
- [Running the tests](#running-the-tests)
- [Security notes](#security-notes)
- [Troubleshooting](#troubleshooting)

## Key features

### Administrator features
- Secure login and logout.
- View every registered user with username, email, role and account status.
- Enable or disable any standard user account. The currently signed in administrator cannot disable their own account.
- Full create, read, update and delete management of prompt categories. A category that still has prompts assigned to it cannot be deleted, which protects referential integrity.
- Full create, read, update and delete management of policy keywords. These keywords drive automatic flagging.
- View all prompts that have been flagged for containing a policy keyword, together with the owner, the matched keyword and the prompt text.

### Standard user features
- Self service registration and secure login and logout.
- Create, view, edit and delete their own prompts.
- Mark each prompt as private (visible only to the owner) or shared (visible to all users in the community vault).
- Browse the community vault of prompts that other users have shared.
- Submit any owned prompt to the simulated AI assistant and receive a generated response.
- View a personal submission history and clear it on demand.
- Receive an on screen warning whenever a prompt is detected to contain sensitive content.

## Technology stack

- Java 17
- Spring Boot 4.1.0 (Spring MVC, Spring Data JPA, Spring Security, Spring Validation)
- Thymeleaf server side templates
- MySQL 8 (the application connects with the MySQL Connector/J driver)
- Hibernate as the JPA provider
- Maven as the build tool
- H2 in memory database for the test profile
- JUnit 5 for testing

## Project structure

```
promptvault/
├── database/
│   ├── schema.sql              SQL script that creates the database and all tables
│   └── seed-data.sql           SQL script that inserts the demo data
├── src/
│   ├── main/
│   │   ├── java/com/promptvault/
│   │   │   ├── PromptvaultApplication.java     Application entry point
│   │   │   ├── component/DataSeeder.java        Seeds demo data on first start
│   │   │   ├── config/SecurityConfig.java       Spring Security configuration
│   │   │   ├── controller/                      Web controllers (MVC)
│   │   │   ├── entity/                           JPA entities
│   │   │   ├── repository/                       Spring Data JPA repositories
│   │   │   └── service/                          Business logic and helpers
│   │   └── resources/
│   │       ├── application.properties           Main configuration (MySQL)
│   │       └── templates/                        Thymeleaf HTML templates
│   └── test/
│       ├── java/com/promptvault/                 Test sources
│       └── resources/application.properties     Test configuration (H2)
└── pom.xml
```

## Domain model

The application is built around five entities.

| Entity | Description |
| --- | --- |
| User | An account with a username, hashed password, email, role (ADMIN or USER) and an active flag. |
| Category | A grouping that every prompt belongs to, with a name and description. |
| Prompt | A prompt owned by a user. Holds the title, text, visibility (PRIVATE or SHARED), flag status, the matched keyword, the simulated AI response and the submission date. |
| PolicyKeyword | A word or phrase that, when found in a prompt, causes the prompt to be flagged for review. |
| SubmissionHistory | A record created each time a prompt is submitted to the simulated AI, storing the response and the timestamp. |

## Prerequisites

Make sure the following are installed before you begin.

- Java Development Kit 17 or newer (`java -version` should report 17 or above).
- Apache Maven 3.8 or newer (`mvn -version`). The project also includes the Maven wrapper, so `./mvnw` can be used instead of `mvn` if you prefer.
- MySQL Server 8 or newer, running and reachable on `localhost:3306`.

## Database setup

You have two options. The application can create the schema automatically, or you can run the supplied scripts manually.

### Option A: let the application create the schema (simplest)

1. Create an empty database and make sure the credentials match the configuration described below:

   ```sql
   CREATE DATABASE promptvault CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. Start the application. Because `spring.jpa.hibernate.ddl-auto=update` is set, Hibernate creates all tables on the first run, and the `DataSeeder` component then inserts the demo data automatically.

### Option B: run the SQL scripts manually

The `database` folder contains two scripts.

1. `database/schema.sql` creates the `promptvault` database and every table.
2. `database/seed-data.sql` inserts the same demo data the application would seed on its own.

Run them in order, for example:

```bash
mysql -u root -p < database/schema.sql
mysql -u root -p < database/seed-data.sql
```

The seed script stores passwords as PBKDF2 hashes in exactly the format the application produces, so the demo accounts log in normally.

## Configuration

Database settings live in `src/main/resources/application.properties`. Update the username and password to match your local MySQL installation.

```properties
server.port=8080

spring.datasource.url=jdbc:mysql://localhost:3306/promptvault?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=prompting2026

spring.jpa.hibernate.ddl-auto=update
```

If your MySQL root password differs, change `spring.datasource.password` accordingly. The application listens on port 8080 by default; change `server.port` if that port is in use.

## How to run

From the project root:

```bash
# using a locally installed Maven (default MySQL profile)
mvn spring-boot:run

# or using the Maven wrapper
./mvnw spring-boot:run
```

If you have not set up MySQL and just want to try the application, run the demo profile instead, which uses an embedded database and needs no configuration:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

Alternatively, build a runnable jar and start it:

```bash
mvn clean package
java -jar target/promptvault-0.0.1-SNAPSHOT.jar
```

Once the log shows `Started PromptvaultApplication`, open a browser at:

```
http://localhost:8080/login
```

## Demo accounts

The seeded accounts below are available immediately after the first run or after running the seed script.

| Role | Username | Password |
| --- | --- | --- |
| Administrator | `admin` | `admin123` |
| User | `alice` | `password123` |
| User | `bob` | `password123` |

You can also create new standard user accounts through the registration page.

## Sample data

On first start the application seeds:

- 3 users: one administrator and two standard users.
- 6 categories: Coding, Research, Cybersecurity, Legal, HR and Personal productivity.
- 8 policy keywords: password, api key, secret, credit card, private key, confidential, medical record and student number.
- 6 prompts owned by the two standard users, with a mix of private and shared visibility. Two of them are deliberately flagged because they contain a policy keyword.
- 2 submission history entries so the history view is populated.

All seeding is guarded by existence checks, so restarting the application does not create duplicate data.

## Using the application

1. Open `http://localhost:8080/login`.
2. To explore the administrator side, log in as `admin`. You land on the admin dashboard where you can manage users, categories and policy keywords, and review flagged prompts.
3. To explore the user side, log in as `alice` or `bob`, or register a new account. You land on your personal dashboard where you can create prompts, submit them to the simulated AI, browse shared prompts and review your submission history.

## How prompt flagging works

Whenever a prompt is created, edited or submitted to the simulated AI, its text is compared against the list of policy keywords. The check is case insensitive and matches whole or partial text. If any keyword is found:

- the prompt is marked as flagged and the matched keyword is recorded,
- the user sees an on screen warning that the prompt may contain sensitive information,
- the prompt appears in the administrator flagged prompts view.

Administrators maintain the keyword list, so the flagging behaviour can be tuned at any time without code changes.

## Running the tests

The test profile uses an in memory H2 database, so the tests run without a MySQL server.

```bash
mvn test
```

The test suite loads the full Spring application context, which verifies that all beans, controllers, repositories and the data seeder wire together correctly.

## Security notes

- Passwords are never stored in plain text. They are hashed with PBKDF2 (HMAC SHA-256, 120000 iterations and a per user random salt).
- Disabled accounts cannot log in.
- Every controller checks the session for an authenticated user, and administrator only pages additionally require the ADMIN role.

## Troubleshooting

- Cannot connect to the database, or you do not want to install MySQL at all: run the app with the demo profile (`mvn spring-boot:run -Dspring-boot.run.profiles=demo`). It uses an embedded database, needs zero configuration and seeds the demo accounts automatically.
- Access denied for the database user: confirm the username and password in `application.properties` match your MySQL credentials.
- Unknown database `promptvault`: create the database first, or run `database/schema.sql`.
- Port 8080 already in use: change `server.port` in `application.properties` to a free port.
- The login form just reloads or "the security blocks me": make sure you are running the latest code on the `fixes` branch (`git checkout fixes && git pull`), then rebuild with `mvn clean spring-boot:run`. Use the exact credentials from the table above; passwords are case sensitive.
- The demo accounts do not log in after a manual import: make sure you used `database/seed-data.sql`, which contains the correct password hashes, rather than inserting plain text passwords.

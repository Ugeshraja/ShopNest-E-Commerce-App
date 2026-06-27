# ShopNest 🪺 — Premium E-Commerce Platform

ShopNest is a complete, production-ready, full-stack Java e-commerce web application. It features a modern user catalog, shopping cart flow, checkout system, customer order history, and a restricted administrative dashboard for catalog management.

Built with **Spring Boot 3.x**, **Spring Security 6**, **Hibernate ORM**, **Thymeleaf**, and **Bootstrap 5**.

---

## 🚀 Key Features

### 👤 Customer Experience
*   **Secure Sign Up & Sign In**: Account registration and BCrypt password encryption utilizing Spring Security.
*   **Interactive Catalog**: Browse products, search dynamically by keywords, and filter by categories.
*   **Pill-Style Navigation**: Modern category filter sidebar with micro-interactions.
*   **Active Shopping Cart**: Add items, adjust purchase quantities, and remove products dynamically.
*   **Secure Checkout**: Billing address collection with transaction summary calculations.
*   **Order Confirmation**: Automatic stock deduction, pricing snapshot, and receipt generation.
*   **Purchase History**: Access historical orders showing status tags (Pending, Confirmed, Shipped, Delivered).

### 🛠️ Administrative Control
*   **Dashboard Analytics**: Monitor metrics showing total registered users, catalog count, orders, and total revenue.
*   **Inventory Control**: Add new products (with direct image URLs), edit descriptions/prices, and delete listings.
*   **Order Tracking**: View all customer transactions and update order states through dynamic status dropdowns.

---

## 💻 Tech Stack

*   **Framework**: Spring Boot 3.3.0
*   **Security**: Spring Security 6.x (Form Login & Session management)
*   **View Engine**: Thymeleaf 3.1.x
*   **Database (Local)**: MySQL Server 8.x
*   **Database (Cloud/Production)**: PostgreSQL (Aiven / Render)
*   **ORM Layer**: Spring Data JPA + Hibernate
*   **Build Tool**: Maven 3.x
*   **Styling**: Bootstrap 5 + Custom Glassmorphism CSS Stylesheets

---

## 📂 Project Structure

```text
ShopNest/
├── src/main/java/com/shopnest/
│   ├── ShopNestApplication.java       # Boot Entry Point
│   ├── config/                        # Security & Web MVC configurations
│   ├── controller/                    # Routing Controllers
│   ├── model/                         # JPA Database Entities
│   ├── repository/                    # JPA CRUD Interfaces
│   ├── service/                       # Transactions Business Logic
│   └── dto/                           # Data Request Bindings
├── src/main/resources/
│   ├── templates/                     # Thymeleaf HTML Views
│   ├── static/                        # CSS, JS, and Images assets
│   └── application.properties         # Properties Configurations
├── pom.xml                            # Maven Dependencies Configuration
└── README.md                          # Project Documentation
```

---

## 🛠️ Local Setup & Execution

### 1. Prerequisites
Ensure you have installed:
*   **Java Development Kit (JDK) 17+**
*   **Apache Maven**
*   **MySQL Server** (running on port `3306`)

### 2. Database Set Up
Log in to your local MySQL Command line interface and execute:
```sql
CREATE DATABASE IF NOT EXISTS shopnest;
```
*(Note: Hibernate is configured to automatically generate tables and columns on startup).*

### 3. Match Credentials
Open `src/main/resources/application.properties` and verify your local database password matches:
```properties
spring.datasource.password=yourpassword  <-- CHANGE THIS
```

### 4. Build & Run
Open your terminal in the root of the project and execute:
```bash
# Package the application
mvn clean package -DskipTests

# Start the web server
java -jar target/shopnest-0.0.1-SNAPSHOT.jar
```

Now open your web browser and navigate to: **[http://localhost:8080](http://localhost:8080)**

---

## ☁️ Deployment Guide (Render + Aiven PostgreSQL)

This codebase is configured to detect environment variables and automatically switch from local MySQL to cloud PostgreSQL databases.

### 1. Database Connection Format
Aiven / Render PostgreSQL databases use connection strings starting with `postgres://`. **Java JDBC strictly requires `jdbc:` prefix.**

Configure the following environment variables in your **Render Web Service (Environment Tab)** settings:

| Variable Name | Value Format |
| :--- | :--- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<Host>:<Port>/<Database>?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | *Your Database Username* |
| `SPRING_DATASOURCE_PASSWORD` | *Your Database Password* |
| `SPRING_DATASOURCE_DRIVER` | `org.postgresql.Driver` |

### 2. Automatic Port Binding
Spring Boot will automatically bind to Render's dynamic port using `server.port=${PORT:8080}`. No manual port settings are required.

---

## 🔐 Default Demo Accounts

Upon initial startup, the database is auto-seeded with test catalog items and two default accounts:

*   **Customer User Experience**:
    *   **Email**: `user@shopnest.com`
    *   **Password**: `user123`
*   **System Administrator**:
    *   **Email**: `admin@shopnest.com`
    *   **Password**: `admin123` *(grants access to `/admin` dashboard panel)*

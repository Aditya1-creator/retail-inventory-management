# Retail Inventory Management System

A web-based Retail Inventory Management System built with **Java Spring Boot** covering product catalog, multi-warehouse inventory, order fulfillment, and supplier management.

## Tech Stack

- **Backend:** Java 17, Spring Boot 4.1.1, Maven, Spring Data JPA, Spring Security + JWT, MySQL
- **Frontend:** React, Axios, React Router (separate project)

## Prerequisites

- JDK 17+
- MySQL 8.x running on `localhost:3306`
- Maven (or use the included `mvnw` wrapper)

## Setup & Run

```bash
# 1. Clone the project
# 2. Create the database (auto-created if using the default URL)
# 3. Provide MySQL credentials for the current shell session:
#    $env:DB_USERNAME="root"
#    $env:DB_PASSWORD="your_mysql_password"

# 4. Run the application
./mvnw spring-boot:run

# 5. Run tests
./mvnw test
```

The server starts at `http://localhost:8082` by default, matching the frontend API URL.

From the project root, ` .\Start-Backend.ps1 ` prompts privately for your MySQL password. Use ` .\Start-Backend.ps1 -Demo ` only for temporary in-memory H2 data.

Run ` .\mvnw.cmd verify ` for the full backend checks and executable JAR. The final review passed 43 backend tests, including 15 HTTP integration cases. Live MySQL checks also passed, including three concurrent order/cancellation rounds and persistence across backend restart. Mockito is configured as a startup agent for reliable testing on current JDKs.

## API Endpoints

All routes are prefixed with `/api`. Protected routes require `Authorization: Bearer <token>`.

### Authentication (Public)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login and get JWT token |

### Products (ADMIN: full, STAFF: read-only)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/products` | List all products |
| GET | `/api/products/{id}` | Get product by ID |
| POST | `/api/products` | Create product |
| PUT | `/api/products/{id}` | Update product |
| DELETE | `/api/products/{id}` | Delete product |

### Suppliers (ADMIN: full, STAFF: read-only)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/suppliers` | List all suppliers |
| GET | `/api/suppliers/{id}` | Get supplier by ID |
| POST | `/api/suppliers` | Create supplier |
| PUT | `/api/suppliers/{id}` | Update supplier |
| DELETE | `/api/suppliers/{id}` | Delete supplier |

### Warehouses (ADMIN: full, STAFF: read-only)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/warehouses` | List all warehouses |
| GET | `/api/warehouses/{id}` | Get warehouse by ID |
| POST | `/api/warehouses` | Create warehouse |
| PUT | `/api/warehouses/{id}` | Update warehouse |
| DELETE | `/api/warehouses/{id}` | Delete warehouse |

### Stock (ADMIN: add/deduct, ALL: view)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/stock` | List all stock records |
| POST | `/api/stock/add` | Add stock to a product in a warehouse |
| POST | `/api/stock/deduct` | Deduct stock from a product in a warehouse |
| GET | `/api/stock/low` | List products at or below reorder level |

### Orders (ALL: view/create, ADMIN: cancel)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/orders` | List all orders |
| GET | `/api/orders/{id}` | Get order by ID |
| POST | `/api/orders` | Create a new order (deducts stock) |
| PUT | `/api/orders/{id}/cancel` | Cancel an order (restores stock) |

### Dashboard
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/summary` | Get dashboard summary |

## Presentation Security Configuration

The current project demonstration exposes the inventory API routes without a login screen so that the React frontend can be used directly. JWT, BCrypt password hashing, and the user/role model remain in the backend for a future secured deployment. Do not describe the current demonstration API as role-protected unless authentication is re-enabled in `SecurityConfig` and a login flow is added to the frontend.

## Architecture

```
React UI → Controller → Service → Repository → MySQL
```

- **Controller:** Receives HTTP requests, returns JSON responses
- **Service:** Business logic with @Transactional for atomicity
- **Repository:** Spring Data JPA interfaces
- **Entity:** JPA-mapped database classes
- **DTO:** Request/response objects (entities are never exposed directly)

## Database Schema (7 Tables)

| Table | Description |
|-------|-------------|
| users | Application users with roles (ADMIN/STAFF) |
| suppliers | Supplier contact information |
| products | Product catalog with SKU, price, reorder level |
| warehouses | Warehouse locations |
| stocks | Stock quantity per product per warehouse |
| orders | Customer orders with status tracking |
| order_items | Individual items within an order |

## Business Rules

- Stock can never go negative
- Creating an order atomically deducts stock from specified warehouses
- Cancelling an order restores stock to the original warehouses
- Low-stock products are those whose total stock across all warehouses ≤ reorderLevel
- Passwords are hashed with BCrypt and never returned in responses
- JWT tokens expire in 24 hours

## Project Structure

```
src/main/java/com/hcl/Inventory/
├── config/          SecurityConfig, CorsConfig
├── controller/      AuthController, ProductController, SupplierController,
│                    WarehouseController, StockController, OrderController,
│                    SummaryController
├── dto/             Request and Response DTOs for all modules
├── entity/          User, Product, Supplier, Warehouse, Stock, Order, OrderItem
├── exception/       ResourceNotFoundException, InsufficientStockException,
│                    GlobalExceptionHandler, ErrorResponse
├── repository/      JPA repository interfaces
├── security/        JwtUtil, JwtAuthFilter, CustomUserDetailsService
└── service/         AuthService, ProductService, SupplierService,
                     WarehouseService, StockService, OrderService, SummaryService
```

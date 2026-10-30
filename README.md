# Retail Inventory Management System

This repository contains a Retail Inventory Management System with a React frontend and a Spring Boot backend.

**Live demo:** [retail-inventory-management.onrender.com](https://retail-inventory-management.onrender.com/)

Hosted frontend, MySQL-backed APIs and all nine stock/order workflow check groups were verified on 30 October 2026. Saved inventory and cancelled orders were also verified after a user-confirmed Render service restart. The free server can take a few minutes to wake after inactivity; allow extra time before a presentation.

## Free Online Deployment

See [the deployment guide](DEPLOYMENT.md) for Render Free with Aiven Free MySQL. The Docker build packages React and Spring Boot into one web service, with the database hosted separately. Deployment requires accounts with both providers and private database settings in Render.

## Project Objective

The project helps shops and warehouses manage products, suppliers, warehouses, stock, and customer orders digitally. It reduces manual stock errors and gives a clear view of available inventory.

## Presentation Content

Use the following simple 8-slide structure for the project presentation.


Slide 1
**Retail Inventory Management System**



### Slide 2 Problem and Objective

**Problem**

- Manual inventory records can cause stock mistakes.
- It is difficult to manage products in multiple warehouses.
- Orders can be created when stock is low.

**Objective**

- Build a simple system to manage products, suppliers, warehouses, stock, and orders digitally.

### Slide 3 Main Features

- Product management
- Supplier and warehouse management
- Stock tracking for each warehouse
- Add and deduct stock
- Order creation and cancellation
- Low-stock information
- Dashboard summary

### Slide 4 Detailed Technology Stack

| Layer | Technology | Purpose |
|---|---|---|
| Frontend | React and Vite | User interface and pages |
| Styling | CSS | Dashboard, sidebar, forms, and tables |
| Backend | Java and Spring Boot | APIs and business logic |
| Database | MySQL | Stores project data |
| Database Access | Spring Data JPA and Hibernate | Connects backend with database |
| Security | Spring Security and JWT | Authentication code is available; inventory APIs are public for the current no-login demo |
| Validation | Jakarta Validation | Checks input before saving |
| Build Tool | Maven | Manages backend dependencies and builds |
| Testing | JUnit and Spring Boot Test | Tests backend modules |
| Development Tools | VS Code and Postman | Coding and API testing |

**Simple explanation:** React creates the frontend interface. Spring Boot handles the business logic and APIs. MySQL stores the data. JPA and Hibernate connect Java with MySQL. Login and role enforcement are future work for this demo.

### Slide 5 Working Flow

```text
Supplier -> Product -> Warehouse -> Add Stock -> Create Order
                                                |
                                                v
                                         Stock Deducted
```

```text
Order Cancelled -> Stock Restored
```

Explain that the system checks stock before an order is created and prevents negative stock.

### Slide 6 System Architecture

```text
React Frontend
      |
      v
Spring Boot Backend
      |
      v
MySQL Database
```

- React provides the user interface.
- Spring Boot manages the APIs and business rules.
- MySQL stores products, suppliers, warehouses, stock, and orders.

### Slide 7 Result and Current Status

- Backend modules for products, suppliers, warehouses, stock, orders, dashboard, and authentication are implemented.
- MySQL startup, live workflow checks and persistence across backend restart have been verified.
- Spring Boot startup and real HTTP integration checks passed with the isolated H2 test database.
- 50 backend unit and integration tests passed.
- The React frontend interface is restored and builds successfully.

### Slide 8 Future Scope and Conclusion

**Future scope**

- Add optional role-based login when required.
- Add role-based access for admin and staff.
- Add barcode scanning, reports, and low-stock notifications.

**Conclusion**

The project makes inventory work more organized by tracking stock across warehouses, preventing negative stock, and supporting order management.

## How to Run the Project

Start MySQL first. Then use two PowerShell windows. From this folder, ` .\Start-Backend.ps1 ` prompts privately for the MySQL password and starts the backend on port 8082. Use ` .\Start-Backend.ps1 -Demo ` only for temporary in-memory H2 data.

### 1 Start the Backend with MySQL

```powershell
cd E:\retail_inventory\Inventory
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_mysql_password"
mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--server.port=8082"
```

Backend URL: `http://localhost:8082`

### 2 Start the React Frontend

```powershell
cd E:\retail_inventory\inventory-frontend
npm run dev
```

Open the Vite URL shown in the terminal, normally `http://localhost:5173/`.

## Current Frontend Status

The React frontend is connected to the MySQL-backed Spring Boot API. Products, suppliers, warehouses, stock adjustments, orders, dashboard data, and order cancellation use live API requests. For the no-login presentation flow, inventory APIs are public; JWT code remains available for future login support.

## Verification Summary

- Backend automated tests: 50 passed, 0 failures, 0 errors, 0 skipped.
- Frontend automated tests: 13 passed.
- MySQL connection, live workflows and persistence across backend restart: verified.
- Three consecutive MySQL concurrency rounds: verified competing orders reject overselling and competing cancellations restore stock once.
- Spring Boot H2 startup and HTTP integration: verified.
- React production build: verified.

See `SUBMISSION_CHECK.md` for fixes, test coverage, current limits and repeatable checks. Live browser control was disabled during the final pass; React workflows were verified with automated DOM tests.

## Product archive and deletion

Use **Archive** to remove a product from the active catalogue while preserving stock and all order history. Select **Include archived products** to find it, then **Restore** to use it for new orders again. Archived products remain visible in stock records and can receive stock returned by an existing order cancellation. They are excluded from active-product counts, low-stock alerts and new-order selection. The API also rejects new orders for archived products.

Permanent **Delete** is available only for a product with no order history and no remaining stock. Empty stock rows are removed automatically. Products referenced by confirmed or cancelled orders return a specific instruction to use Archive. SKUs remain reserved while products are archived.

The archive flag defaults to false when added to the database, preserving existing products as active during deployment.

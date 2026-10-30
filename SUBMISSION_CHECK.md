# Submission verification

Review date: 30 October 2026

## Verified

- Backend: 47 automated tests passed with no failures, errors or skipped tests.
- The backend tests include 16 real HTTP integration cases with an isolated H2 database.
- The deployment health endpoint checks database connectivity separately from dashboard aggregation; tests cover ready, invalid and unavailable database connections.
- Verified stock deduction, cancellation, repeated cancellation, transaction rollback, low-stock aggregation, decimal prices, overflow rejection, CORS, registration and login.
- Concurrent tests verify that competing deductions cannot oversell, simultaneous cancellations restore once, and simultaneous first additions retain all units.
- Backend executable JAR built successfully under `Inventory/target`.
- Frontend: 13 automated tests passed, covering React form entry, decimal prices, optional fields, stock, orders, error feedback and API-client routes.
- Production frontend build completed successfully.
- Compatible npm dependency patches resolved the reported vulnerabilities.

## Start for final submission

From this folder, run ` .\Start-Backend.ps1 ` in PowerShell. Enter the MySQL password in the hidden local prompt. MySQL must be running. The default backend port is 8082.

In another PowerShell window, enter `inventory-frontend` and run `npm run dev`. Open `http://localhost:5173/`.

To use temporary in-memory data instead, run `.\Start-Backend.ps1 -Demo`. These records disappear when that backend stops.

## Reproduce checks

- Backend: from `Inventory`, run `.\mvnw.cmd verify`.
- Frontend: from `inventory-frontend`, run `npm test`, then `npm run build`.

## Final MySQL verification

MySQL root credentials were reset successfully and the backend connected to MySQL. The live checks passed catalog creation, stock, normal order/cancellation, repeated cancellation and rollback. The MySQL concurrent-order test exposed a repeatable-read issue, corrected by adding locking stock reads. After restarting the corrected build, all nine live check groups passed, including three consecutive rounds of competing orders and competing cancellations. A review product created before the backend restart remained available afterward, confirming persistence. The frontend server returned HTTP 200 and the backend accepted its origin through CORS.

Review-only records are labelled with a REVIEW prefix. All successful orders created by the final verification were cancelled and their quantities restored; pre-existing user records were not altered.

Live browser control is disabled in Work settings. React interactions were checked with automated DOM tests; there is no new visual browser verification in this pass.

Presentation APIs remain public, and the weekly chart still contains clearly labelled demonstration values. The application is intended for an academic local demonstration, not an authenticated public deployment.

Existing temporary supplier, warehouse and product data was backed up to `demo-data-before-verification.json` before the database switch.

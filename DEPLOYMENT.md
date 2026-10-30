# Free public demonstration deployment

Live app: https://retail-inventory-management.onrender.com/

## Architecture

Browser -> HTTPS public URL -> Render web service serving React and /api -> Aiven MySQL

The root Dockerfile tests and builds React, tests and builds Java 17, and packages the frontend into Spring Boot static resources. Only one public web service is needed. The production frontend calls `/api` on its own origin instead of the reviewer's localhost.

## Free hosting and its limits

Select Render's Free web-service plan and Aiven's Free MySQL plan explicitly. Do not select a paid plan or start a paid trial. Render sleeps after 15 minutes without traffic and may need about a minute to wake. Open the link before a live review. Aiven's free plan currently has 1 GB storage and may power down inactive services. Check current account eligibility and limits before creating resources.

## 1 Create the free MySQL service

Create an account at https://console.aiven.io/ and create a MySQL service on the Free plan. Use fictional demonstration data and keep the existing laptop database separate. From the service connection details, collect the host, port, database name, username, password and project CA certificate. These must be entered into Render privately, never committed to GitHub.

## 2 Deploy the app on Render

Create a Render account at https://dashboard.render.com/ using GitHub. Create a Blueprint from `Aditya1-creator/retail-inventory-management`, branch `main`. The root render.yaml selects a Docker web service on `plan: free` and checks readiness at `/api/health`. This endpoint validates a database connection without loading dashboard aggregates. It returns HTTP 200 with `status: UP` for a valid connection and HTTP 503 with `status: DOWN` if the database is unavailable. The production server binds explicitly to `0.0.0.0` and Render's `PORT`.

After the free MySQL service is ready, open [Deploy this repository on Render](https://dashboard.render.com/blueprint/new?repo=https://github.com/Aditya1-creator/retail-inventory-management). Keep the branch as `main` and the web-service plan as Free.

Enter these environment variables in Render:

| Variable | Value |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_URL` | `jdbc:mysql://HOST:PORT/DATABASE?sslMode=VERIFY_IDENTITY&serverTimezone=UTC` using Aiven's host, port and database |
| `DB_USERNAME` | Aiven's database username |
| `DB_PASSWORD` | Aiven's database password |
| `MYSQL_CA_CERT` | Entire Aiven project CA certificate in PEM format, including BEGIN and END lines |

The entrypoint creates a temporary Java truststore from the CA certificate, allowing hostname and CA verification. Do not disable TLS verification. Render supplies `PORT`; the backend reads it automatically. No database password is present in the frontend bundle or Docker image.

## 3 Verify the hosted app

Wait for the deployment's health check to pass, then open its returned HTTPS URL. Load all six UI sections. Add a sample supplier, warehouse and product, receive stock, create an order and cancel it. Confirm that the quantities change correctly and data survives an application redeploy. A deployment is complete only after its actual public URL and database-backed workflow have been checked.

The existing API verification script also supports the hosted URL. From PowerShell in the repository folder, run the following against the separate demonstration database. It creates `REVIEW-` sample records and exercises stock safeguards, order rollback and three rounds of competing orders and cancellations:

```powershell
$env:INVENTORY_API_URL = 'https://YOUR-SERVICE.onrender.com/api'
try { node ./verify-live-api.mjs }
finally { Remove-Item Env:INVENTORY_API_URL -ErrorAction SilentlyContinue }
```

## Build checks

```text
docker build -t retail-inventory:review .
```

Both test suites run during the container build. The runtime uses a non-root user and a memory-limited JVM setting suited to a small academic demo. The local development process remains unchanged: Start-Backend.ps1 and npm run dev.

Verified on 30 October 2026: the Blueprint passed validation against Render's official JSON schema, the Java 17 production-profile package passed local checks, and all 47 backend tests passed. The React suite previously passed all 13 tests. Render successfully built and deployed the Docker image from commit `ac56272` with the dedicated database readiness check.

On the actual HTTPS app, the homepage, JavaScript and CSS returned HTTP 200. The frontend bundle used the relative `/api` address, all six dashboard data endpoints returned JSON when requested concurrently, and `/api/health` returned `UP`. All nine live workflow groups passed against the hosted database, including decimal prices, stock safeguards, order/cancellation, rollback and three rounds of competing orders and cancellations. A write carrying the public frontend's Origin header also succeeded. Review records use a `REVIEW-` prefix, successful test orders were cancelled, and test stock was restored to 10 units. After a user-confirmed Render service restart, the saved product and price, supplier, warehouse, stock quantity of 10 and all four cancelled test orders were verified again, confirming hosted restart persistence. A new visual browser review was not performed in this pass.

The first concurrent dashboard load after waking took about 15 seconds. Free-host startup can take a few minutes, so open the app ahead of a review.

## Public demo scope

This is the requested no-login academic demo. Anyone with its URL can modify inventory. Use sample records only. The weekly chart remains labelled demo content. Authentication and user roles remain future work. The local MySQL root password and existing local database records must not be copied into the deployment.

## References

- https://render.com/docs/free
- https://render.com/docs/blueprint-spec
- https://aiven.io/docs/products/mysql/concepts/mysql-free-tier
- https://aiven.io/docs/platform/concepts/tls-ssl-certificates

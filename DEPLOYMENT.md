# Free public demonstration deployment

## Architecture

Browser -> HTTPS public URL -> Render web service serving React and /api -> Aiven MySQL

The root Dockerfile tests and builds React, tests and builds Java 17, and packages the frontend into Spring Boot static resources. Only one public web service is needed. The production frontend calls `/api` on its own origin instead of the reviewer's localhost.

## Free hosting and its limits

Select Render's Free web-service plan and Aiven's Free MySQL plan explicitly. Do not select a paid plan or start a paid trial. Render sleeps after 15 minutes without traffic and may need about a minute to wake. Open the link before a live review. Aiven's free plan currently has 1 GB storage and may power down inactive services. Check current account eligibility and limits before creating resources.

## 1 Create the free MySQL service

Create an account at https://console.aiven.io/ and create a MySQL service on the Free plan. Use fictional demonstration data and keep the existing laptop database separate. From the service connection details, collect the host, port, database name, username, password and project CA certificate. These must be entered into Render privately, never committed to GitHub.

## 2 Deploy the app on Render

Create a Render account at https://dashboard.render.com/ using GitHub. Create a Blueprint from `Aditya1-creator/retail-inventory-management`, branch `main`. The root render.yaml selects a Docker web service on `plan: free` and checks readiness at `/api/summary`.

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

## Build checks

```text
docker build -t retail-inventory:review .
```

Both test suites run during the container build. The runtime uses a non-root user and a memory-limited JVM setting suited to a small academic demo. The local development process remains unchanged: Start-Backend.ps1 and npm run dev.

## Public demo scope

This is the requested no-login academic demo. Anyone with its URL can modify inventory. Use sample records only. The weekly chart remains labelled demo content. Authentication and user roles remain future work. The local MySQL root password and existing local database records must not be copied into the deployment.

## References

- https://render.com/docs/free
- https://render.com/docs/blueprint-spec
- https://aiven.io/docs/products/mysql/concepts/mysql-free-tier
- https://aiven.io/docs/platform/concepts/tls-ssl-certificates

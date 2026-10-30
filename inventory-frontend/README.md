# Retail Inventory Frontend

React and Vite frontend for the Retail Inventory Management System.

## Features

- Dashboard with live product, warehouse, low-stock, and order information
- Create, edit, and delete products, suppliers, and warehouses
- Add and deduct stock by warehouse
- Create and cancel orders
- Loading, empty-state, success, and backend-error feedback
- Collapsible sidebar and presentation-friendly dashboard visuals

## Run the Full Application

Start the Spring Boot backend first at `http://localhost:8082`, then run:

```powershell
cd E:\retail_inventory\inventory-frontend
npm install
npm run dev
```

Open the Vite URL shown in the terminal, normally `http://localhost:5173/`.

## Backend Connection

The frontend sends requests to `http://localhost:8082/api` by default. To use another backend URL, create a `.env` file with:

```text
VITE_API_URL=http://localhost:8082/api
```

## Build

```powershell
npm run build
```

## Automated checks

Run `npm test` for the 13 React workflow and API-client tests. The production build and all frontend tests passed in the final review. The development server uses port 5173 strictly so it cannot silently move to an origin the backend does not allow.

## Presentation Mode

The backend inventory APIs are intentionally public for the current no-login presentation flow. JWT code remains in the backend and can be re-enabled when a login interface is added.

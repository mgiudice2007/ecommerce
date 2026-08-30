# API - Ecommerce de Pasajes de Avión

Base URL: `http://localhost:8080`

Autenticación por **sesión (cookie JSESSIONID)**, no JWT. En Insomnia dejá activado
el manejo de cookies (por defecto, dentro del mismo workspace) para que la sesión
se mantenga entre requests después del login.

## Usuarios de prueba (seed inicial, solo si la base está vacía al arrancar)

- Admin: `username: admin`, `password: admin123`
- No hay pasajero precargado: registrar uno con `POST /api/auth/registro/pasajero`

Vuelos de ejemplo ya cargados (aerolínea id 1 "Aerolineas Demo"):
- Vuelo 1: Buenos Aires → Madrid, ECONOMICA, $950
- Vuelo 2: Buenos Aires → Miami, EJECUTIVA, $700 (10% descuento)

## Auth — `/api/auth`

### POST `/api/auth/registro/pasajero` (público)
```json
{
  "username": "mile",
  "mail": "mile@test.com",
  "password": "123456",
  "nombre": "Milena",
  "apellido": "Giudice"
}
```

### POST `/api/auth/registro/administrador` (requiere sesión con rol ADMINISTRADOR)
```json
{
  "username": "admin2",
  "mail": "admin2@test.com",
  "password": "123456",
  "nombre": "Ana",
  "apellido": "Admin",
  "permisos": ["GESTION_VUELOS"]
}
```

### POST `/api/auth/login` (público)
```json
{ "username": "mile", "password": "123456" }
```

### POST `/api/auth/logout`
Sin body.

### GET `/api/auth/me`
Requiere sesión.

## Aerolíneas — `/api/aerolineas`

- `GET /api/aerolineas` — público
- `GET /api/aerolineas/{id}` — público
- `POST /api/aerolineas` — ADMIN
  ```json
  { "nombre": "Aerolineas Argentinas" }
  ```
- `PUT /api/aerolineas/{id}` — ADMIN, mismo body que POST
- `DELETE /api/aerolineas/{id}` — ADMIN

## Vuelos — `/api/vuelos`

- `GET /api/vuelos?origen=BUE&destino=MAD&clase=ECONOMICA&precioMin=100&precioMax=1000` — público, todos los query params opcionales
- `GET /api/vuelos/{id}` — público
- `POST /api/vuelos` — ADMIN
  ```json
  {
    "origen": "Buenos Aires",
    "destino": "Madrid",
    "fechaSalida": "2026-12-01T10:00:00",
    "precio": 500.00,
    "asientosDisponibles": 100,
    "descuento": 0,
    "clase": "ECONOMICA",
    "aerolineaId": 1
  }
  ```
  Valores de `clase`: `ECONOMICA`, `EJECUTIVA`, `PRIMERA`
- `PUT /api/vuelos/{id}` — ADMIN, mismo body
- `DELETE /api/vuelos/{id}` — ADMIN

## Carrito — `/api/carrito` (requiere sesión con rol PASAJERO)

- `GET /api/carrito`
- `POST /api/carrito/items`
  ```json
  { "vueloId": 1, "cantidad": 2 }
  ```
- `PUT /api/carrito/items/{itemId}`
  ```json
  { "cantidad": 3 }
  ```
- `DELETE /api/carrito/items/{itemId}`
- `POST /api/carrito/checkout` — sin body, genera la reserva

## Reservas — `/api/reservas` (requiere sesión con rol PASAJERO)

- `GET /api/reservas` — historial
- `GET /api/reservas/{id}`
- `POST /api/reservas/{id}/cancelar` — sin body

## Flujo típico de prueba

1. `POST /api/auth/login` con `admin` / `admin123` → probar endpoints ADMIN (crear vuelos/aerolíneas).
2. `POST /api/auth/registro/pasajero` con un usuario nuevo, luego `POST /api/auth/login` con ese usuario → probar carrito y reservas usando `vueloId` 1 o 2.
3. `POST /api/carrito/items` → `POST /api/carrito/checkout` → `GET /api/reservas`

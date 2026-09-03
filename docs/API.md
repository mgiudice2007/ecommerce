# API - Ecommerce de Pasajes de Avión

Base URL: `http://localhost:8080`

Autenticación por **JWT** (`Authorization: Bearer <token>`), stateless. El token se obtiene
en `POST /api/auth/login` y expira a las 24hs. No hay cookies de sesión ni estado en el servidor:
cada request protegido debe mandar el header `Authorization`.

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

### POST `/api/auth/registro/administrador` (requiere token con rol ADMINISTRADOR)
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
Devuelve:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "usuario": { "id": 1, "username": "mile", "mail": "mile@test.com", "nombre": "Milena", "apellido": "Giudice", "rol": "PASAJERO" }
}
```
Guardar `token` y mandarlo como `Authorization: Bearer <token>` en los siguientes requests.

### POST `/api/auth/logout`
Requiere token. Sin body. Con JWT stateless es un no-op del lado servidor (devuelve 204):
no hay invalidación real, el token sigue siendo válido hasta que expira. El "logout" real
es que el cliente descarte el token.

### GET `/api/auth/me`
Requiere token.

## Aerolíneas — `/api/aerolineas`

- `GET /api/aerolineas` — público
- `GET /api/aerolineas/{id}` — público
- `POST /api/aerolineas` — requiere token con rol ADMINISTRADOR
  ```json
  { "nombre": "Aerolineas Argentinas" }
  ```
- `PUT /api/aerolineas/{id}` — ADMIN, mismo body que POST
- `DELETE /api/aerolineas/{id}` — ADMIN

## Vuelos — `/api/vuelos`

- `GET /api/vuelos?origen=BUE&destino=MAD&clase=ECONOMICA&precioMin=100&precioMax=1000` — público, todos los query params opcionales
- `GET /api/vuelos/{id}` — público
- `POST /api/vuelos` — requiere token con rol ADMINISTRADOR
  ```json
  {
    "origen": "Buenos Aires",
    "destino": "Madrid",
    "fechaSalida": "2026-12-15T10:00:00",
    "precio": 500.00,
    "asientosDisponibles": 100,
    "descuento": 0,
    "clase": "ECONOMICA",
    "aerolineaId": 1
  }
  ```
  Valores de `clase`: `ECONOMICA`, `EJECUTIVA`, `PRIMERA`. `fechaSalida` debe ser una fecha futura.
- `PUT /api/vuelos/{id}` — ADMIN, mismo body
- `DELETE /api/vuelos/{id}` — ADMIN

## Carrito — `/api/carrito` (requiere token con rol PASAJERO)

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
- `POST /api/carrito/checkout` — sin body, genera la orden

## Ordenes — `/api/ordenes` (requiere token con rol PASAJERO)

- `GET /api/ordenes` — historial
- `GET /api/ordenes/{id}`
- `POST /api/ordenes/{id}/cancelar` — sin body

## Flujo típico de prueba

1. `POST /api/auth/login` con `admin` / `admin123` → guardar el `token` → probar endpoints ADMIN (crear vuelos/aerolíneas) mandando `Authorization: Bearer <token>`.
2. `POST /api/auth/registro/pasajero` con un usuario nuevo, luego `POST /api/auth/login` con ese usuario → guardar su `token` → probar carrito y ordenes usando `vueloId` 1 o 2.
3. `POST /api/carrito/items` → `POST /api/carrito/checkout` → `GET /api/ordenes`

## Colección de Insomnia

`docs/insomnia_collection.json` trae el flujo completo de arriba ya armado en requests
organizados en carpetas (Auth → Aerolíneas → Vuelos → Carrito → Ordenes → Casos de error →
Cleanup), con los tokens e IDs encadenados automáticamente entre requests vía variables de
entorno (`token_admin`, `token_pasajero`, `aerolinea_id`, `vuelo_id`, `item_id`, `orden_id`).
Importarlo en Insomnia con File → Import.

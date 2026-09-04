# API - Ecommerce de Pasajes de Avión

Base URL: `http://localhost:8080`

Autenticación por **JWT** (`Authorization: Bearer <token>`), stateless. El token se obtiene
en `POST /api/auth/login` y expira a las 24hs. No hay cookies de sesión ni estado en el servidor.

## Modelo: marketplace multi-vendedor

Cualquier usuario con rol `VENDEDOR` publica sus propios vuelos (no hay una "aerolínea" fija
cargada por un admin). Un mismo vuelo se vende en varias clases al mismo tiempo, cada una con
su propio stock y precio (`Disponibilidad`).

Roles (`Rol`): `COMPRADOR`, `VENDEDOR`, `ADMIN`.

## Usuarios de prueba (seed inicial, solo si la base está vacía al arrancar)

| username | password | rol |
|---|---|---|
| `admin` | `admin123` | ADMIN |
| `vendedor` | `vendedor123` | VENDEDOR |
| `comprador` | `comprador123` | COMPRADOR |

Catálogo semilla: 6 aeropuertos (EZE, AEP, COR, MDZ, MAD, MIA), 3 categorías (Cabotaje,
Regional, Internacional), 3 clases (Economica, Ejecutiva, Primera), 2 vuelos de ejemplo del
usuario `vendedor` con su `Disponibilidad` cargada.

## Auth — `/api/auth`

### POST `/api/auth/registro` (público)
Registro genérico — el rol es un campo del body, solo acepta `COMPRADOR` o `VENDEDOR`
(mandar `ADMIN` acá devuelve `400`).
```json
{
  "username": "mile",
  "mail": "mile@test.com",
  "password": "123456",
  "nombre": "Milena",
  "apellido": "Giudice",
  "rol": "COMPRADOR"
}
```

### POST `/api/auth/registro/administrador` (requiere token con rol ADMIN)
Mismo body, pero fuerza `rol=ADMIN` del lado del servidor sin importar lo que mandes.

### POST `/api/auth/login` (público)
```json
{ "username": "comprador", "password": "comprador123" }
```
Devuelve `{ "token": "...", "usuario": { id, username, mail, nombre, apellido, rol } }`.

### POST `/api/auth/logout`
Requiere token. No-op del lado servidor (JWT stateless), devuelve `204`.

### GET `/api/auth/me`
Requiere token.

## Catálogo — público, sin token

- `GET /api/categorias`
- `GET /api/aeropuertos` — la clave es el código IATA (3 letras), no un id numérico
- `GET /api/clases`

## Vuelos — `/api/vuelos`

- `GET /api/vuelos` — público, filtros opcionales combinables: `origen`, `destino`,
  `categoriaId`, `claseId`, `precioMin`, `precioMax`, `vendedorId`, más `page`/`size` para
  paginar (respuesta `Page<VueloResponse>`: `content`, `totalElements`, `totalPages`, etc.)
- `GET /api/vuelos/{id}` — público, incluye `disponibilidades[]` y `hayStock`
- `POST /api/vuelos` — requiere token con rol `VENDEDOR` o `ADMIN`. El vuelo nace **sin
  asientos** — el cupo se carga aparte con `Disponibilidad`.
  ```json
  {
    "numeroVuelo": "AR1500",
    "descripcion": "Buenos Aires a Madrid, directo",
    "categoriaId": 1,
    "origenIata": "EZE",
    "destinoIata": "MAD",
    "fechaSalida": "2026-12-15T10:00:00",
    "fechaLlegada": "2026-12-15T22:30:00",
    "precio": 1200.0,
    "descuento": 10
  }
  ```
- `PUT /api/vuelos/{id}` — solo el vendedor dueño (o un ADMIN) puede modificarlo; con el token
  de otro vendedor devuelve `400`.
- `DELETE /api/vuelos/{id}` — **baja lógica** (pasa a `estado=ELIMINADO`, no borra la fila —
  así no rompe las órdenes que ya lo referencian). Desaparece del listado pero sigue
  respondiendo por id.

## Disponibilidades (stock por clase) — `/api/disponibilidades`

- `GET /api/disponibilidades?vueloId={id}` — público, lista los cupos de un vuelo
- `GET /api/disponibilidades/{id}` — público
- `POST /api/disponibilidades` — requiere token `VENDEDOR`/`ADMIN` dueño del vuelo. Repetir la
  misma clase para el mismo vuelo da `400` (hay un `UNIQUE(vuelo_id, clase_id)`).
  ```json
  { "vueloId": 1, "claseId": 1, "asientosTotales": 30, "precio": 1200.0 }
  ```
- `PUT /api/disponibilidades/{id}` — no deja bajar `asientosTotales` por debajo de lo ya
  vendido.

## Carrito — `/api/carrito` (requiere token con rol COMPRADOR)

- `GET /api/carrito`
- `POST /api/carrito/items` — se elige vuelo **y clase** en un solo id (`disponibilidadId`).
  Pedir más asientos de los que hay disponibles da `400`.
  ```json
  { "disponibilidadId": 1, "cantidad": 2 }
  ```
- `PUT /api/carrito/items/{itemId}` — `{ "cantidad": 3 }`
- `DELETE /api/carrito/items/{itemId}`
- `POST /api/carrito/checkout` — sin body. Operación transaccional: valida el stock de todos
  los ítems, descuenta asientos de cada `Disponibilidad`, crea la `Orden` con el precio
  **congelado** al momento de la compra, y vacía el carrito. Si algo falla, se revierte entero.

## Órdenes — `/api/ordenes` (requiere token con rol COMPRADOR)

- `GET /api/ordenes` — historial, solo las del usuario del token
- `GET /api/ordenes/{id}` — la orden de otro usuario da `404` (no `403`, para no confirmar que
  ese id existe)
- `POST /api/ordenes/{id}/cancelar` — devuelve los asientos a la `Disponibilidad` correspondiente

## Flujo típico de prueba

1. `GET /api/categorias`, `/api/aeropuertos`, `/api/clases` para tener los ids del catálogo.
2. `POST /api/auth/login` con `vendedor`/`vendedor123` → crear un vuelo → cargar su
   `Disponibilidad` (clase + stock + precio).
3. `POST /api/auth/login` con `comprador`/`comprador123` → agregar esa `disponibilidadId` al
   carrito → `POST /api/carrito/checkout` → `GET /api/ordenes`.

## Colección de Insomnia

`docs/insomnia_collection.json` trae el flujo completo de arriba en carpetas numeradas
(0-Catálogo, 1-Auth, 2-Vuelos, 3-Disponibilidades, 4-Carrito, 5-Órdenes), con los tokens e ids
encadenados automáticamente entre requests (tag `{% response %}` de Insomnia — no hace falta
copiar nada a mano). Importarla con File → Import.

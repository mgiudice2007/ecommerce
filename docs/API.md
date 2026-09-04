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
    "precio": 1200.0
  }
  ```
  `precioConDescuento` en la respuesta ya no viene de un campo fijo en el vuelo: se calcula
  solo, buscando si hay un `Descuento` vigente hoy (ver más abajo). Sin descuento vigente,
  `precioConDescuento` = `precio`.
- `PUT /api/vuelos/{id}` — solo el vendedor dueño (o un ADMIN) puede modificarlo; con el token
  de otro vendedor devuelve `400`.
- `DELETE /api/vuelos/{id}` — **baja lógica** (pasa a `estado=ELIMINADO`, no borra la fila —
  así no rompe las órdenes que ya lo referencian). Desaparece del listado pero sigue
  respondiendo por id.

## Descuentos (promociones por vuelo) — `/api/descuentos`

El precio final de un vuelo (y de cada `Disponibilidad`, por clase) depende del `Descuento`
**vigente hoy** para ese vuelo, si hay alguno — no de un campo fijo. "Vigente" = `activo=true`
y la fecha de hoy está entre `fechaDesde` y `fechaHasta`.

- `GET /api/descuentos?vueloId={id}` — público, lista todos los descuentos del vuelo (vigentes o no)
- `GET /api/descuentos/{id}` — público
- `POST /api/descuentos` — requiere token `VENDEDOR`/`ADMIN` dueño del vuelo.
  ```json
  {
    "vueloId": 1,
    "tipoDescuento": "PORCENTAJE",
    "valor": 20,
    "fechaDesde": "2026-09-01",
    "fechaHasta": "2026-09-30",
    "activo": true
  }
  ```
  `tipoDescuento`: `PORCENTAJE` (0-100, se resta como %) o `MONTO_FIJO` (se resta directo del
  precio). No se puede crear un descuento `activo=true` cuyo rango de fechas se superponga con
  otro descuento activo del mismo vuelo → `400` (así nunca hay ambigüedad sobre cuál aplica).
- `PUT /api/descuentos/{id}` — mismo body y mismas validaciones.

## Fotos — `/api/fotos`

El binario de la imagen se guarda en la base (`LONGBLOB`), subido como `multipart/form-data`.

- `GET /api/fotos?vueloId={id}` — público, solo metadata (`id`, `nombreArchivo`, `orden`,
  `tamanioBytes`) — **no** trae el binario, para no inflar la respuesta.
- `GET /api/fotos/{id}/contenido` — público, devuelve el binario con el `Content-Type` según la
  extensión del archivo (`.jpg`/`.jpeg`/`.png`/`.gif`).
- `POST /api/fotos` (multipart) — requiere token `VENDEDOR`/`ADMIN` dueño del vuelo. Campos:
  `vueloId`, `archivo` (el binario), `orden` (opcional — si no viene, se calcula solo como la
  siguiente posición). Solo acepta archivos con `Content-Type` que empiece con `image/`.
- `DELETE /api/fotos/{id}` — dueño del vuelo o ADMIN.

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
(0-Catálogo, 1-Auth, 2-Vuelos, 3-Disponibilidades, 4-Descuentos, 5-Fotos, 6-Carrito, 7-Órdenes),
con los tokens e ids encadenados automáticamente entre requests (tag `{% response %}` de
Insomnia — no hace falta copiar nada a mano). Importarla con File → Import.

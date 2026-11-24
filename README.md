# Automata Vehicle Capture

**Captura y digitalización inteligente de información vehicular.** Aplicación web mobile-first que permite
fotografiar las fichas físicas de vehículos, extraer sus datos automáticamente (OCR/IA), validarlos a mano
desde el teléfono y generar el archivo de registro que exige la empresa portuaria.

Proyecto de **Automata SPA**. Estado: **esqueleto funcional con datos de demostración** (ver [Estado actual](#estado-actual)).

---

## Informe del proyecto

### 1. Problema que resuelve

Una empresa contratista traslada vehículos importados dentro de las instalaciones de una empresa portuaria en
Iquique, Chile. Hoy el proceso es manual:

1. Llega el vehículo con una ficha física.
2. Una trabajadora fotografía la ficha con su celular.
3. Más tarde revisa cada foto y **transcribe a mano** los datos a un archivo de registro exigido por el puerto.
4. Mantiene ese registro actualizado y lo entrega.

Esto implica carga administrativa, trabajo duplicado, pérdida de tiempo y riesgo de errores de digitación.

### 2. Qué hace la solución

Elimina la transcripción manual. La trabajadora completa el proceso desde el celular, en el lugar:

| Etapa | Qué ocurre |
|---|---|
| **Captura** | Toma una foto (o elige de la galería), una o varias fichas seguidas. Se conserva la imagen original como respaldo. |
| **Procesamiento** | El sistema lee el texto de la foto (OCR), reconoce los campos, normaliza valores y detecta campos vacíos, ilegibles o de baja confianza. |
| **Validación** | Ve una ficha digital junto a la foto original, corrige lo que haga falta y confirma. **Los datos de la IA nunca pasan solos a definitivos: siempre requieren validación humana.** |
| **Almacenamiento** | Cada vehículo es un registro independiente, con su foto, datos, fechas, estado, responsable y trazabilidad. |
| **Exportación** | Genera el archivo para la empresa portuaria solo con registros validados, sin duplicados, y registra qué ya fue exportado. |

**Estados de un registro:** `En cola → Procesando → Requiere revisión / Pendiente de validación → Validado → Exportado`
(más `Falló el procesamiento`, con reintento).

### 3. Reglas de negocio implementadas

- Cada ficha fotografiada es un registro individual.
- Una foto puede estar incompleta o ilegible; el OCR puede equivocarse, por eso la persona siempre puede corregir.
- Solo los registros **validados** entran a una exportación; los exportados **no se vuelven a ofrecer** (sin duplicados).
- El VIN / número de chasis se usa como criterio de **posible duplicado** (advertencia, decide una persona).
- Las **fotos originales son inmutables**: no existe operación de borrado ni reemplazo.
- Un registro exportado ya no se puede modificar; editar uno validado lo devuelve a validación.
- Toda operación importante queda en un **historial de auditoría** (capturado, procesado, editado con antes/después, validado, exportado).

### 4. Tecnologías

| Capa | Tecnología | Por qué |
|---|---|---|
| Frontend | **React 18 + TypeScript + Vite** | SPA rápida, tipada, responsive y mobile-first |
| Backend | **Java 21 + Spring Boot 3** (API REST, arquitectura por capas) | Robusto, mantenible, ecosistema maduro |
| Persistencia | **PostgreSQL** (producción) · H2 (desarrollo local) · **Flyway** (migraciones) | Transaccional, auditable; H2 evita instalar nada para desarrollar |
| Archivos | Puerto `BlobStorage` (hoy disco local) | Imágenes fuera de la base de datos; migrable a S3/MinIO sin tocar la lógica |
| OCR / extracción | Puertos `OcrProvider` + `FieldExtractor` | Cambiar de proveedor (Google Vision, AWS Textract, Tesseract, LLM con visión) sin modificar el negocio |
| Exportación | **Apache POI** (.xlsx) | Generación de Excel; preparado para plantilla configurable |
| Infraestructura | **Docker / Docker Compose**, configuración por variables de entorno, perfiles por ambiente | Preparado para despliegue en cloud |

### 5. Arquitectura

Monolito modular (sin microservicios innecesarios para esta escala), con las integraciones externas aisladas
detrás de interfaces.

```
┌───────────────────────── Frontend React (móvil) ─────────────────────────┐
│  Dashboard │ Captura │ Vehículos │ Detalle/Edición │ Exportaciones       │
└──────────────────────────────┬───────────────────────────────────────────┘
                               │ REST (JSON / multipart)
┌──────────────────────────────▼───────────────────────────────────────────┐
│                          Backend Spring Boot                              │
│  web        controladores REST + manejo de errores (ProblemDetail)        │
│  service    captura · procesamiento async · revisión/validación · export  │
│  domain     entidades y reglas de transición de estado                    │
│  puertos →  OcrProvider │ FieldExtractor │ BlobStorage │ (Exporter)       │
│  adaptadores  OCR dummy │ extractor etiqueta:valor │ disco local │ POI     │
└───────────────┬───────────────────────────────┬──────────────────────────┘
                │                               │
          PostgreSQL / H2                 Almacén de archivos
   (registros, extracciones,         (fotos originales y exportaciones)
    auditoría, exportaciones)
```

**Flujo de procesamiento:** la captura guarda la foto y crea el registro; tras confirmarse la transacción se
dispara el procesamiento asíncrono: `imagen → OcrProvider → FieldExtractor → normalización → estado`.
El registro termina en *Requiere revisión* (campo obligatorio faltante o confianza baja) o *Pendiente de validación*.

### 6. Modelo de datos

| Entidad | Contenido |
|---|---|
| `vehicle_record` | Un vehículo: estado, VIN/marca/modelo (columnas para búsqueda y duplicados), valores de la ficha (JSON), fechas, validador |
| `capture_image` | Foto original: ruta de almacenamiento, hash SHA-256, tipo y tamaño. Inmutable |
| `extraction` | Resultado crudo de cada lectura OCR (texto, valores, confianza por campo, errores) |
| `audit_event` | Historial append-only de operaciones (quién, cuándo, qué cambió) |
| `export_batch` / `export_item` | Cada exportación y una copia (snapshot) de lo que salió en ella |

Los campos variables de la ficha viven en un JSON validado contra un esquema, para soportar distintos formatos
de ficha sin migraciones de base de datos.

### 7. Estado actual

> **Esqueleto funcional con datos DUMMY.** El flujo completo funciona de punta a punta, pero el OCR es
> simulado, los campos de la ficha son ficticios y el Excel de salida es de ejemplo. Se reemplazan cuando se
> disponga de las fichas reales y del formato oficial de exportación. No se inventaron estructuras reales.

| Dummy | Se reemplaza por | Dónde |
|---|---|---|
| OCR simulado | Google Vision / Textract / Tesseract (+ LLM) | nueva clase `OcrProvider`; `app.ocr.provider` |
| Campos ficticios de la ficha | Esquema real por formato de ficha | `FieldSchemaProvider` |
| Parser "Etiqueta: valor" | Extractor por plantilla | `FieldExtractor` |
| Columnas de salida de ejemplo | Formato oficial del Excel (plantilla) | `app.export.columns` + `XlsxExporter` |
| Usuario fijo `demo`, sin login | Spring Security + JWT, roles | `CurrentUser` |

### 8. Plan por fases

| Fase | Alcance | Estado |
|---|---|---|
| **0. Descubrimiento** | Fichas reales, archivo oficial de exportación, prueba comparativa de motores OCR | Pendiente de documentación |
| **1. MVP** | Captura, OCR, ficha editable, persistencia, validación manual, exportación básica, auditoría | Flujo base hecho con datos dummy |
| **2. Automatización** | Procesamiento por lotes, duplicados avanzados, plantillas de exportación configurables, historial completo | Pendiente |
| **3. Escalabilidad** | Múltiples formatos de ficha, optimización OCR, usuarios y permisos, producción | Pendiente |

### 9. Información pendiente / riesgos

- **Falta (bloqueante):** fichas reales (cuántos formatos, campos obligatorios), el archivo oficial de exportación
  (columnas, orden, plantilla; si es acumulativo o uno por entrega), volumen diario, usuarios y roles, regla
  exacta ante VIN repetido, conectividad en terreno.
- **Calidad de la foto** (luz, ángulo, reflejos): es el principal riesgo de precisión. Mitigación: guías de captura y recaptura.
- **Variabilidad de las fichas:** diseño extensible por plantilla.
- **Sin conexión en terreno:** pendiente cola offline (PWA) con sincronización posterior.
- **Privacidad:** si el OCR es un servicio cloud, las imágenes viajan a un tercero; se debe validar con la empresa portuaria.

---

## Guía técnica

### Ejecutar en desarrollo (sin Docker)

Requisitos: Java 21, Maven, Node 20+.

```bash
# Terminal 1 — backend (http://localhost:8080). H2 en archivo: ./backend/data y ./backend/storage
cd backend
mvn spring-boot:run

# Terminal 2 — frontend (http://localhost:5173)
cd frontend
npm install
npm run dev
```

Para probar desde el celular (misma WiFi) abre `http://<IP-del-PC>:5173`. Con cualquier foto funciona: el OCR
dummy genera una ficha ficticia **determinista según la imagen** (misma foto → mismos datos), con campos
faltantes, ilegibles y de baja confianza para ejercitar la revisión.

Tests: `cd backend && mvn test` (recorre captura → procesamiento → corrección → validación → exportación).

### Con Docker (PostgreSQL)

```bash
cp .env.example .env     # define POSTGRES_PASSWORD
docker compose up --build
# app en http://localhost:8081
```

> Los archivos Docker están escritos pero **aún no se han probado**.

### Estructura del repositorio

```
backend/   Spring Boot 3 · Java 21 · JPA + Flyway · Apache POI
  domain/      entidades y reglas de transición de estado (VehicleRecord)
  processing/  puertos OcrProvider / FieldExtractor + implementaciones dummy + FieldSchemaProvider
  storage/     puerto BlobStorage (hoy disco local; mañana S3/MinIO)
  service/     captura, procesamiento async, revisión/validación, exportación
  web/         controladores REST + manejo de errores (ProblemDetail)
frontend/  React 18 · TypeScript · Vite   (pages/: Dashboard, Capture, Vehicles, VehicleDetail, Exports)
```

### API

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/captures` | Sube una foto (multipart `file`). 201 nuevo / 200 si esa foto ya existía |
| GET | `/api/vehicles?status=&q=` | Lista con filtros |
| GET/PUT | `/api/vehicles/{id}` | Detalle / corrección de datos |
| POST | `/api/vehicles/{id}/validate` | Validación humana |
| POST | `/api/vehicles/{id}/retry` | Reintenta un procesamiento fallido |
| GET | `/api/vehicles/{id}/image` | Foto original |
| GET | `/api/dashboard` · `/api/schema` | Indicadores · esquema de campos |
| POST/GET | `/api/exports` | Generar (ids opcionales) / historial |
| GET | `/api/exports/{id}/download` | Descargar el .xlsx |

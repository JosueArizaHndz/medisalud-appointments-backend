# Medisalud - Sistema de Gestión de Citas Médicas

## 📋 Descripción del Proyecto

Sistema RESTful para la gestión integral de citas médicas de la clínica Medisalud, desarrollado con Spring Boot 3 y arquitectura hexagonal. Permite administrar doctores, pacientes y citas médicas con validaciones de negocio, detección de conflictos horarios, consulta de disponibilidad y cancelación con penalizaciones.

## 🏗️ Arquitectura

El proyecto utiliza **Arquitectura Hexagonal (Ports & Adapters)** para separar claramente las responsabilidades:

```
┌─────────────────────────────────────────────────────────────┐
│                    INFRASTRUCTURE (Adapters)                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │ REST Controllers│ │ JPA Repositories │ │ Config/Exceptions│  │
│  └──────────────┘  └──────────────┘  └──────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│              APPLICATION (Business Logic)                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │   Services    │  │    DTOs      │  │   Commands/Query │  │
│  └──────────────┘  └──────────────┘  └──────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│                   DOMAIN (Core Business)                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │   Models      │  │    Enums     │  │    Ports         │  │
│  └──────────────┘  └──────────────┘  └──────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Capas del Proyecto

| Capa | Ubicación | Responsabilidad |
|------|-----------|-----------------|
| **Domain** | `domain/` | Modelos, enums, interfaces de puertos (contratos) |
| **Application** | `application/` | Servicios, DTOs, comandos y queries |
| **Infrastructure** | `infrastructure/` | Controladores REST, repositorios JPA, configuración |

### Patrón CQRS Light

- **Command Ports**: CreateDoctorCommand, CreatePatientCommand, CreateAppointmentCommand, etc.
- **Query Ports**: DoctorQueryPort, PatientQueryPort, AppointmentQueryPort

## 🛠️ Tecnologías

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| **Java** | 21 | Lenguaje base |
| **Spring Boot** | 3.5.16 | Framework principal |
| **Spring Data JPA** | - | Persistencia con Hibernate |
| **Hibernate** | 6.6.53 | ORM |
| **H2 Database** | - | Base de datos (desarrollo) |
| **SpringDoc OpenAPI** | 2.8.9 | Documentación Swagger |
| **Lombok** | - | Reducción de boilerplate |
| **Jakarta Validation** | - | Validaciones |
| **Gradle** | 8.14.5 | Build tool |
| **JUnit 5** | - | Testing |

## 📁 Estructura del Proyecto

```
appointments/
├── src/main/java/com/medisalud/appointments/
│   ├── AppointmentsApplication.java          # Punto de entrada
│   ├── domain/
│   │   ├── enums/
│   │   │   └── AppointmentStatus.java        # Estados de cita
│   │   ├── model/
│   │   │   ├── Appointment.java              # Entidad cita
│   │   │   ├── Doctor.java                   # Entidad doctor
│   │   │   ├── Patient.java                  # Entidad paciente
│   │   │   └── Penalty.java                  # Entidad penalización
│   │   ├── port/
│   │   │   ├── in/                           # Command/Query ports
│   │   │   │   ├── CreateDoctorCommand.java
│   │   │   │   ├── CreatePatientCommand.java
│   │   │   │   ├── CreateAppointmentCommand.java
│   │   │   │   ├── CancelAppointmentCommand.java
│   │   │   │   ├── RescheduleAppointmentCommand.java
│   │   │   │   ├── AvailableSlotsQuery.java
│   │   │   │   ├── ListAppointmentsQuery.java
│   │   │   │   └── ... (otros commands/queries)
│   │   │   └── out/                          # Repository ports
│   │   │       ├── DoctorRepositoryPort.java
│   │   │       ├── PatientRepositoryPort.java
│   │   │       ├── AppointmentRepositoryPort.java
│   │   │       └── PenaltyRepositoryPort.java
│   │   └── util/
│   │       └── UUIDConverter.java            # Utilidad UUID
│   ├── application/
│   │   ├── dto/                              # Response DTOs
│   │   │   ├── ApiResponse.java
│   │   │   ├── AppointmentResponse.java
│   │   │   ├── DoctorResponse.java
│   │   │   ├── PatientResponse.java
│   │   │   └── AvailableSlot.java
│   │   └── service/
│   │       ├── DoctorServiceInterface.java
│   │       ├── PatientServiceInterface.java
│   │       ├── AppointmentServiceInterface.java
│   │       └── AppointmentService.java       # Implementación
│   └── infrastructure/
│       ├── adapter/
│       │   ├── in/rest/                      # REST Controllers
│       │   │   ├── DoctorController.java
│       │   │   ├── PatientController.java
│       │   │   └── AppointmentController.java
│       │   └── out/persistence/              # JPA Repositories
│       │       ├── JpaDoctorRepository.java
│       │       ├── JpaPatientRepository.java
│       │       ├── JpaAppointmentRepository.java
│       │       └── JpaPenaltyRepository.java
│       ├── config/
│       │   ├── DataSeeder.java               # Datos de prueba
│       │   └── SwaggerConfig.java            # CORS/Swagger
│       └── exception/
│           ├── GlobalExceptionHandler.java   # Manejo de errores
│           ├── ResourceNotFoundException.java
│           └── ErrorResponse.java
├── src/test/java/                            # Tests unitarios e integración
├── build.gradle                              # Dependencias Gradle
└── README.md                                 # Este archivo
```

## 🚀 Cómo Ejecutar el Proyecto

### Prerrequisitos

- **Java JDK 21+**
- **Gradle 8.14.5+** (incluido como wrapper)

### Ejecutar la Aplicación

```bash
# Windows
.\gradlew bootRun

# Linux/Mac
./gradlew bootRun
```

La aplicación se ejecutará en: **http://localhost:8080**

### Compilar sin Tests

```bash
.\gradlew build -x test
```

### Ejecutar Tests

```bash
# Todos los tests
.\gradlew test

# Test específico
.\gradlew test --tests "AppointmentCancellationTest"
```

## � Contenedorización con Docker

### Prerrequisitos

- **Docker** instalado y en ejecución

### Construir la Imagen

```bash
# Construir imagen Docker
docker build -t medisalud .
```

### Ejecutar el Contenedor

```bash
# Ejecutar contenedor
docker run -p 8080:8080 medisalud
```

### Verificar Funcionamiento

Una vez ejecutado el contenedor, acceder a:

- **Aplicación:** http://localhost:8080
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **H2 Console:** http://localhost:8080/h2-console

### Comandos Útiles

```bash
# Construir y ejecutar en un solo comando
docker build -t medisalud . && docker run -p 8080:8080 medisalud

# Ejecutar con nombre personalizado
docker run --name medisalud-app -p 8080:8080 medisalud

# Ver logs del contenedor
docker logs medisalud-app

# Detener contenedor
docker stop medisalud-app

# Eliminar contenedor
docker rm medisalud-app

# Eliminar imagen
docker rmi medisalud
```

### Configuración Docker

El proyecto incluye un **Dockerfile multi-stage** que:

- ✅ Usa Eclipse Temurin JDK 21 para build
- ✅ Usa Eclipse Temurin JRE 21 para runtime (imagen ligera)
- ✅ Optimiza capas con Gradle wrapper
- ✅ Excluye tests en la construcción
- ✅ Configura JVM para contenedores
- ✅ Crea directorio `/app/data` para base de datos H2
- ✅ Expone puerto 8080

### Notas Importantes

- La base de datos H2 usa modo file-based (`jdbc:h2:file:./data/medisalud`)
- Los datos se almacenan dentro del contenedor (se pierden al eliminarlo)
- Para persistencia de datos, usar volúmenes de Docker:
  ```bash
  docker run -p 8080:8080 -v medisalud-data:/app/data medisalud
  ```
- **No se migró a PostgreSQL** - se mantiene H2 para desarrollo

## �📚 API Documentation

### Swagger UI

Accede a la documentación interactiva de la API:

```
http://localhost:8080/swagger-ui.html
```

JSON OpenAPI:

```
http://localhost:8080/api-docs
```

### H2 Console (Base de Datos)

```
http://localhost:8080/h2-console
```

**Configuración:**
- JDBC URL: `jdbc:h2:file:./data/medisalud`
- User Name: `sa`
- Password: (dejar vacío)

## 🔌 Endpoints Principales

### Doctores

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/doctors` | Listar todos los doctores |
| GET | `/doctors/{id}` | Obtener doctor por ID |
| POST | `/doctors` | Crear doctor |
| PUT | `/doctors/{id}` | Actualizar doctor |
| DELETE | `/doctors/{id}` | Eliminar doctor |

### Pacientes

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/patients` | Listar todos los pacientes |
| GET | `/patients/{id}` | Obtener paciente por ID |
| POST | `/patients` | Crear paciente |
| PUT | `/patients/{id}` | Actualizar paciente |
| DELETE | `/patients/{id}` | Eliminar paciente |

### Citas Médicas

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/appointments` | Listar citas (con filtros) |
| GET | `/appointments/{id}` | Obtener cita por ID |
| GET | `/appointments/doctor/{doctorId}` | Citas por doctor |
| GET | `/appointments/patient/{patientId}` | Citas por paciente |
| GET | `/appointments/date` | Citas por fecha |
| POST | `/appointments` | Crear cita |
| PATCH | `/appointments/{id}/cancel` | Cancelar cita |
| PATCH | `/appointments/{id}/reschedule` | Reprogramar cita |
| GET | `/appointments/availability` | Consultar disponibilidad |

### Filtros en Listado de Citas

```
GET /appointments?doctorId=xxx&patientId=xxx&status=PROGRAMADA&startDate=2026-07-15T08:00&endDate=2026-07-20T18:00
```

## 📝 Ejemplos de Request/Response

### Crear Doctor

**Request:**
```http
POST /doctors
Content-Type: application/json

{
  "name": "Dr. Juan Pérez",
  "email": "juan.perez@medisalud.com",
  "specialty": "GENERAL",
  "licenseNumber": "MED-12345",
  "maxPatients": 100
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Doctor creado exitosamente",
  "data": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "name": "Dr. Juan Pérez",
    "email": "juan.perez@medisalud.com",
    "specialty": "GENERAL",
    "licenseNumber": "MED-12345",
    "maxPatients": 100,
    "active": true,
    "createdAt": "2026-07-02T10:00:00",
    "updatedAt": "2026-07-02T10:00:00"
  },
  "timestamp": "2026-07-02T10:00:00"
}
```

### Crear Paciente

**Request:**
```http
POST /patients
Content-Type: application/json

{
  "name": "María García",
  "identityDocument": "1234567890",
  "phone": "3001234567",
  "email": "maria.garcia@email.com",
  "birthDate": "1985-03-15"
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Paciente creado exitosamente",
  "data": {
    "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
    "name": "María García",
    "identityDocument": "1234567890",
    "phone": "3001234567",
    "email": "maria.garcia@email.com",
    "birthDate": "1985-03-15",
    "medicalRecordNumber": "PAC-000001",
    "active": true,
    "createdAt": "2026-07-02T10:05:00",
    "updatedAt": "2026-07-02T10:05:00"
  },
  "timestamp": "2026-07-02T10:05:00"
}
```

### Crear Cita Médica

**Request:**
```http
POST /appointments
Content-Type: application/json

{
  "patientId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "doctorId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "appointmentDate": "2026-07-10T10:00:00",
  "notes": "Consulta de control"
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Cita creada exitosamente",
  "data": {
    "id": "c3d4e5f6-a7b8-9012-cdef-123456789012",
    "patientId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
    "patientName": "María García",
    "doctorId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "doctorName": "Dr. Juan Pérez",
    "doctorSpecialty": "GENERAL",
    "appointmentDate": "2026-07-10T10:00:00",
    "status": "PROGRAMADA",
    "notes": "Consulta de control",
    "cancellationDate": null,
    "createdAt": "2026-07-02T10:10:00",
    "updatedAt": "2026-07-02T10:10:00"
  },
  "timestamp": "2026-07-02T10:10:00"
}
```

### Cancelar Cita

**Request:**
```http
PATCH /appointments/c3d4e5f6-a7b8-9012-cdef-123456789012/cancel
```

**Response 200 (cancelación temprana):**
```json
{
  "success": true,
  "message": "Cita cancelada exitosamente",
  "data": {
    "status": "CANCELADA",
    "cancellationDate": "2026-07-02T10:15:00"
  },
  "timestamp": "2026-07-02T10:15:00"
}
```

### Consultar Disponibilidad

**Request:**
```http
GET /appointments/availability?doctorId=a1b2c3d4-e5f6-7890-abcd-ef1234567890&fechaInicio=2026-07-10&fechaFin=2026-07-14
```

**Response 200:**
```json
{
  "success": true,
  "message": "Disponibilidad obtenida exitosamente",
  "data": [
    {
      "doctorId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "date": "2026-07-10",
      "timeSlot": "08:00",
      "available": true
    }
  ],
  "timestamp": "2026-07-02T10:20:00"
}
```

## 📋 Reglas de Negocio Implementadas

### Validaciones de Doctores
- ✅ Nombre obligatorio (3-100 caracteres)
- ✅ Email válido (opcional)
- ✅ Teléfono mínimo 7 dígitos (opcional)
- ✅ Especialidad obligatoria
- ✅ Licencia médica única
- ✅ Máximo de pacientes > 0

### Validaciones de Pacientes
- ✅ Nombre obligatorio (2-100 caracteres)
- ✅ Documento de identidad obligatorio y único
- ✅ Teléfono obligatorio
- ✅ Email válido (opcional)
- ✅ Fecha de nacimiento válida (opcional)
- ✅ Número de historia médica auto-generado

### Reglas de Citas Médicas
- ✅ Fecha futura obligatoria
- ✅ Horarios laborales: Lun-Vie 08:00-18:00, Sáb 08:00-13:00, Dom cerrado
- ✅ Franjas de 30 minutos (08:00, 08:30, 09:00, etc.)
- ✅ Conflicto de doctor: no puede tener 2 citas en 30 min
- ✅ Conflicto de paciente: no puede tener 2 citas en 30 min
- ✅ Doctor y paciente deben existir

### Cancelación de Citas
- ✅ Cambia estado a CANCELADA
- ✅ Registra fecha/hora de cancelación
- ✅ Penalización si cancelación < 2 horas antes
- ✅ Sin penalización si cancelación ≥ 2 horas antes
- ✅ No permite cancelar citas ya canceladas
- ✅ No permite cancelar citas finalizadas
- ✅ Slot liberado vuelve a estar disponible

### Reprogramación de Citas
- ✅ Valida existencia de cita
- ✅ No permite reprogramar citas CANCELADAS/FINALIZADAS
- ✅ Aplica mismas reglas de creación (horarios, conflictos)
- ✅ Cancela cita anterior con lógica de penalización
- ✅ Crea nueva cita PROGRAMADA
- ✅ Preserva notas de la cita original
- ✅ Operación transaccional completa

## 🧪 Tests

### Ejecutar Todos los Tests

```bash
.\gradlew test
```

### Tests por Módulo

| Módulo | Archivos de Test | Cantidad |
|--------|-----------------|----------|
| Validaciones | `CreateDoctorCommandValidationTest` | 7 |
| | `CreatePatientCommandValidationTest` | 9 |
| | `CreateAppointmentCommandValidationTest` | 6 |
| Conflictos | `AppointmentConflictTest` | 5 |
| Horarios | `AppointmentHoursValidationTest` | 12 |
| Disponibilidad | `AvailableSlotsTest` | 8 |
| Cancelación | `AppointmentCancellationTest` | 10 |
| Reprogramación | `AppointmentRescheduleTest` | 14 |
| Filtrado | `AppointmentFilteringTest` | 14 |
| **Total** | | **85+** |

## 📦 Colección Postman

Importa la colección Postman para probar la API fácilmente:

**Archivo:** `medisalud-appointments.postman_collection.json`

La colección incluye:
- Doctores (CRUD)
- Pacientes (CRUD)
- Citas Médicas (CRUD + cancelación + reprogramación)
- Consultas de disponibilidad
- Filtros de citas

## 🔧 Configuración

### Base de Datos

**Desarrollo (H2 File):**
```properties
spring.datasource.url=jdbc:h2:file:./data/medisalud
spring.datasource.driver-class-name=org.h2.Driver
```

**Tests (H2 In-Memory):**
```properties
spring.datasource.url=jdbc:h2:mem:testdb
```

### Puerto

La aplicación se ejecuta en el puerto **8080** por defecto.

## 📊 Estados de una Cita

| Estado | Descripción |
|--------|-------------|
| PROGRAMADA | Cita creada, pendiente |
| CONFIRMADA | Cita confirmada |
| EN_PROCESO | Cita en desarrollo |
| FINALIZADA | Cita completada |
| CANCELADA | Cita cancelada |
| NO_ASISTIO | Paciente no asistió |

## ⚠️ Manejo de Errores

Todos los errores siguen el formato:

```json
{
  "timestamp": "2026-07-02T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "El email ya está registrado",
  "path": "/patients"
}
```

| Código | Significado |
|--------|-------------|
| 400 | Bad Request - Validación fallida |
| 404 | Not Found - Recurso no existe |
| 409 | Conflict - Conflicto de estado |
| 500 | Internal Server Error |

## 📝 Notas

- Los datos de ejemplo (3 doctores) se cargan automáticamente al iniciar
- La base de datos H2 se persiste en `./data/medisalud.mv.db`
- Para limpiar la base de datos: eliminar carpeta `data/` y reiniciar
- Lombok se usa para generar getters, setters, builders automáticamente

## 📄 Licencia

Proyecto académico - Método Ceiba

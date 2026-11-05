# EduCoders — Evidencias de Sprint 2
Fecha: 2026-11-05

## Backend
- mvn clean test: 158 pruebas aprobadas.
- VisualizadorHorarioServiceImplTest: 12 pruebas aprobadas.
- Errores: 0.

## Frontend
- PaginaVisualizadorHorario.test.jsx: 2/2 aprobadas.
- EstadosVisualizadorHorario.test.jsx: 4/4 aprobadas.
- npm run build: correcto.
- npm test general: 157 aprobadas y 1 fallida
  en matriculasService.test.js, fuera del visualizador.

## Seguridad HTTP
- ADMIN consulta sección: 200
- ADMIN consulta docente: 200
- DOCENTE consulta horario propio: 200
- ESTUDIANTE consulta horario propio: 200
- APODERADO lista estudiantes vinculados: 200
- APODERADO consulta estudiante vinculado: 200
- APODERADO consulta sección administrativa: 403
- DOCENTE consulta sección administrativa: 403
- ESTUDIANTE consulta docente administrativo: 403
- Solicitud sin JWT: 401
- JWT inválido: 401
- DOCENTE intenta consultar horario de estudiante: 403
- ESTUDIANTE intenta consultar horario de docente: 403
- APODERADO intenta consultar horario de docente: 403

## Pendiente de verificar
- JWT realmente vencido.
- Estudiante ajeno al apoderado.
- Docente inactivo en horario real.
- Matriz poblada desde MySQL del año activo.
- Navegación a edición cuando programación está abierta.
- Mensajes y pruebas adicionales del backlog original.

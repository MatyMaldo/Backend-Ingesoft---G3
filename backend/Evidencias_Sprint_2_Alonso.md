# EduCoders — Evidencias de Sprint 2

**Fecha:** 05/11/2026  
**Responsable:** Alonso Gutiérrez  
**Estado:** Desarrollo y pruebas completados; integración con HU-GEMIH-02 pendiente.

## 1. Resultados de pruebas

| Componente | Resultado |
|---|---|
| Backend — Maven | 166/166 pruebas aprobadas |
| Frontend — Vitest | 166/166 pruebas aprobadas |
| Archivos de pruebas frontend | 29/29 aprobados |
| Pruebas específicas de horarios | 10/10 aprobadas |
| Compilación Vite | Exitosa |

**Resultado:** No se registraron fallos en las ejecuciones finales.

## 2. Funcionalidades verificadas

- Consulta de horarios desde los cinco perfiles previstos.
- Autorización mediante JWT y restricciones por rol.
- Validación de acceso a estudiantes vinculados al apoderado.
- Consulta de horarios de secciones y docentes.
- Identificación del año académico activo mediante fechas.
- Año académico activo confirmado: 2026, del 01/03/2026 al 15/12/2026.
- Visualización de estados sin bloques programados.
- Representación de bloques horarios mediante pruebas automatizadas.
- Navegación condicional hacia el editor cuando la programación está abierta.
- Transmisión de grado y sección mediante parámetros de navegación.

## 3. Seguridad

Se comprobaron las restricciones de acceso para los perfiles administrador, docente, estudiante y apoderado, además de solicitudes sin autenticación.

Se incluyeron pruebas adicionales para JWT vencido y acceso de apoderados a estudiantes no vinculados.

## 4. Validaciones de integración

Los cinco visualizadores fueron comprobados con el backend real en escenarios sin bloques programados.

El año académico 2026 se confirmó como activo mediante los registros existentes. Los horarios poblados fueron comprobados mediante pruebas automatizadas.

## 5. Limitaciones pendientes

- No se comprobó visualmente una matriz poblada con bloques reales del año académico 2026.
- Falta comprobar mediante HTTP el escenario de un docente realmente inactivo.
- La prueba de acceso a un estudiante ajeno cuenta con cobertura automatizada, pero no con una comprobación HTTP utilizando datos reales.
- La integración completa con el editor HU-GEMIH-02, a cargo de Renzo, sigue pendiente de validación.
- No se modificaron los datos compartidos de MySQL ni el código del editor HU-GEMIH-02.

## 6. Publicación en GitHub

Los cambios del Sprint 2 fueron publicados y fusionados en `main` mediante:

- Backend: Pull Request #39.
- Frontend: Pull Request #20.

## 7. Conclusión

Las funcionalidades de visualización de horarios correspondientes al trabajo de Alonso se implementaron y superaron las pruebas automatizadas previstas.

La integración final con el editor HU-GEMIH-02 y las verificaciones que requieren datos reales adicionales quedan identificadas como pendientes.
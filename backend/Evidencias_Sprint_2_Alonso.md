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

## Validaciones adicionales - 05/11/2026

### Estado de programacion
- Seccion 3, ano 2026: HTTP 200; programacionAbierta=false.
- Acceso al estado: administrador 200, sin JWT 401,
  docente 403, estudiante 403, apoderado 403.
- Frontend: 2 pruebas de navegacion condicional aprobadas.

### Ano academico activo
- Secciones 3 y 4 (2026): HTTP 200, SIN_BLOQUES.
- Secciones 1 y 2 (2027): HTTP 400.
- No se modificaron registros de MySQL.

### Apoderado y estudiantes
- Luis (ID 5): HTTP 200, SIN_BLOQUES, 0 bloques.
- Ana (ID 6): HTTP 200, SIN_BLOQUES, 0 bloques.
- Estudiante inexistente (99999): HTTP 404.
- Estudiante ajeno: cubierto por prueba unitaria;
  prueba HTTP pendiente por falta de datos.

### Docentes
- Juan Perez (ID 2): HTTP 200, SIN_BLOQUES.
- Maria Gomez (ID 3): HTTP 200, SIN_BLOQUES.
- Docente inactivo: prueba HTTP pendiente.

### Seguridad
- Nueve verificaciones adicionales de roles: aprobadas.
- JWT vencido: prueba unitaria aprobada.
- HTTP con JWT vencido: pendiente.

### Pruebas automaticas
- Backend: 159 pruebas aprobadas, BUILD SUCCESS.
- Frontend: 8 pruebas especificas aprobadas.
- Vite: compilacion correcta.

### Matriz semanal poblada
- Prueba unitaria con bloque horario: aprobada.
- Integracion real para 2026: pendiente, ya que el
  unico bloque en MySQL pertenece a 2027.

## Resultado final de pruebas - 05/11/2026

### Backend
- mvn clean test: 161 pruebas ejecutadas.
- Fallos: 0. Errores: 0. Omitidas: 0.
- Resultado: BUILD SUCCESS.
- VisualizadorHorarioServiceImplTest: 13 pruebas aprobadas.
- JWT expirado: prueba de servicio y prueba HTTP 401 aprobadas.

### Frontend
- PaginaVisualizadorHorario.test.jsx: 2 pruebas aprobadas.
- EstadosVisualizadorHorario.test.jsx: 4 pruebas aprobadas.
- EstadoProgramacion.test.jsx: 4 pruebas aprobadas.
- Total: 10 pruebas aprobadas.
- npm run build: compilacion correcta.
- Navegacion desde un bloque a /horarios en programacion abierta: aprobada.
- Bloques de solo lectura en programacion cerrada: aprobada.

### Limitaciones pendientes
- No se ha probado un docente inactivo real mediante HTTP.
- No hay bloques reales del anio activo 2026 para una matriz poblada.
- El acceso a estudiantes ajenos tiene prueba unitaria,
  pero no una prueba HTTP con datos reales.
- Falta verificar que el editor abra el bloque seleccionado.


### Navegacion con parametros al editor
- El visualizador administrativo envia grado y seccion a /horarios.
- Ejemplo: /horarios?grado=1&seccion=A.
- Pruebas frontend: 10 aprobadas.
- Compilacion Vite: correcta.
- Pendiente: seleccion automatica del bloque y guardado real.
- No se modifico HU-GEMIH-02 ni MySQL.

## Cierre tecnico verificado - Sprint 2

### Resultados finales
- Backend: 166 de 166 pruebas aprobadas con Maven.
- Frontend: 166 de 166 pruebas aprobadas en 29 archivos.
- Horarios frontend: 10 de 10 pruebas aprobadas.
- Compilacion Vite: exitosa.
- Cinco visualizadores comprobados con el backend real.
- Ano academico activo: 2026, periodo 01/03/2026 al 15/12/2026.
- Se comprobaron los cinco roles previstos.
- Se validaron escenarios de matriz semanal con bloques mediante pruebas automatizadas.

### Limitaciones y dependencias
- No existen bloques reales del ano academico activo 2026 para una comprobacion visual de la matriz poblada.
- La integracion de navegacion y edicion completa con HU-GEMIH-02 depende de los cambios del editor a cargo de Renzo.
- La comprobacion HTTP de un docente inactivo real permanece pendiente.
- No se modificaron los datos compartidos de MySQL ni el codigo del editor HU-GEMIH-02.

### Alcance de las evidencias
Los conteos de 159 y 161 pruebas en secciones anteriores corresponden a ejecuciones intermedias. El resultado final verificado es de 166 pruebas backend y 166 pruebas frontend aprobadas.

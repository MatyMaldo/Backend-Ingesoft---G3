package com.educoders.Seguridad_Auditoria.presentacion;

import com.educoders.Seguridad_Auditoria.dominio.Administrador;
import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Seguridad_Auditoria.servicio.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Verificación manual (no corre en {@code mvn test}) del contrato HTTP de la carga masiva de usuarios contra la BD
 * local: el JSON se interpreta (enum, fechas), solo el administrador entra, y el trabajo asíncrono publica su
 * resultado. NO guarda ningún usuario: todas las filas enviadas son inválidas a propósito (DNI de 3 dígitos), así que
 * se rechazan en la validación. Se activa con {@code -Dbench.http=true}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = "bench.http", matches = "true")
class BenchCargaMasivaUsuariosHttpTest {

    private static final String CUERPO = """
            {"filas": [
              {"linea": 2, "tipoUsuario": "APODERADO", "nombre": "Rosa", "apellidoPaterno": "Quispe", "apellidoMaterno": null,
               "dni": "123", "email": "rosa@http-prueba.test", "telefono": "981001001",
               "fechaNacimiento": "1985-03-12", "dniApoderado": null, "codigo": null},
              {"linea": 3, "tipoUsuario": "ESTUDIANTE", "nombre": "Ana", "apellidoPaterno": "Pérez", "apellidoMaterno": "Ruiz",
               "dni": "456", "email": "ana@http-prueba.test", "telefono": null,
               "fechaNacimiento": "2012-05-10", "dniApoderado": "123", "codigo": "EST001"}
            ]}
            """;

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired JwtService jwtService;

    @Test
    void contratoHttp() throws Exception {
        Administrador admin = usuarioRepository.findAll().stream()
                .filter(u -> u instanceof Administrador).map(u -> (Administrador) u).findFirst().orElseThrow();
        Docente docente = usuarioRepository.findAll().stream()
                .filter(u -> u instanceof Docente).map(u -> (Docente) u).findFirst().orElseThrow();
        String tokenAdmin = "Bearer " + jwtService.generarToken(admin, "ADMINISTRADOR");
        String tokenDocente = "Bearer " + jwtService.generarToken(docente, "DOCENTE");

        System.out.println("BENCHH|sin_token|status=" + mvc.perform(post("/api/v1/usuarios/carga-masiva/async")
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO)).andReturn().getResponse().getStatus());
        System.out.println("BENCHH|docente|status=" + mvc.perform(post("/api/v1/usuarios/carga-masiva/async")
                .header("Authorization", tokenDocente)
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO)).andReturn().getResponse().getStatus());

        MvcResult vacio = mvc.perform(post("/api/v1/usuarios/carga-masiva/async")
                .header("Authorization", tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"filas\": []}")).andReturn();
        System.out.println("BENCHH|lista_vacia|status=" + vacio.getResponse().getStatus() + "|" + vacio.getResponse().getContentAsString());

        MvcResult inicio = mvc.perform(post("/api/v1/usuarios/carga-masiva/async")
                .header("Authorization", tokenAdmin).header("Idempotency-Key", "bench-http-1")
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO)).andReturn();
        String json = inicio.getResponse().getContentAsString();
        System.out.println("BENCHH|inicio|status=" + inicio.getResponse().getStatus() + "|" + json);
        String jobId = json.replaceAll(".*\"jobId\":\"([^\"]+)\".*", "$1");

        // Mismo envío con la misma clave: devuelve el mismo trabajo (no duplica).
        MvcResult repetido = mvc.perform(post("/api/v1/usuarios/carga-masiva/async")
                .header("Authorization", tokenAdmin).header("Idempotency-Key", "bench-http-1")
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO)).andReturn();
        System.out.println("BENCHH|idempotencia|mismo_job="
                + repetido.getResponse().getContentAsString().contains("\"jobId\":\"" + jobId + "\""));

        for (int i = 0; i < 40; i++) {
            Thread.sleep(150);
            MvcResult estado = mvc.perform(get("/api/v1/operaciones-masivas/" + jobId)
                    .header("Authorization", tokenAdmin)).andReturn();
            String cuerpo = estado.getResponse().getContentAsString();
            if (cuerpo.contains("COMPLETADO") || cuerpo.contains("FALLIDO")) {
                System.out.println("BENCHH|estado_final|status=" + estado.getResponse().getStatus() + "|" + cuerpo);
                return;
            }
        }
        System.out.println("BENCHH|estado_final|NO_TERMINO");
    }
}

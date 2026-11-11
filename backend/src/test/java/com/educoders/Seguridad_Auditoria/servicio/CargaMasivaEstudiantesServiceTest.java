package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Infraestructura_Servicios.exception.ReglaNegocioException;
import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.persistencia.ApoderadoRepository;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.CargaMasivaEstudiantesResponse;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La carga masiva valida unicidad en bloque (pocas consultas) y conserva los mensajes por fila.
 */
@ExtendWith(MockitoExtension.class)
class CargaMasivaEstudiantesServiceTest {

    private static final String ENCABEZADO =
            "nombre;apellidoPaterno;apellidoMaterno;dni;email;telefono;fechaNacimiento;apoderadoId\n";

    @Mock private EstudianteService estudianteService;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ApoderadoRepository apoderadoRepository;

    private CargaMasivaEstudiantesService servicio;

    @BeforeEach
    void preparar() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        servicio = new CargaMasivaEstudiantesService(
                new EstudianteCsvParser(), estudianteService,
                usuarioRepository, apoderadoRepository, validator);
    }

    @Test
    void cargaValida_registraTodasLasFilasConPocasConsultas() {
        simularApoderado(7L);

        CargaMasivaEstudiantesResponse respuesta = servicio.cargarEstudiantes(csv(
                "Ana;Perez;;11111111;ana@x.com;;2012-05-10;7",
                "Luis;Gomez;;22222222;luis@x.com;;2011-03-02;7"));

        assertEquals(2, respuesta.estudiantesRegistrados());
        verify(estudianteService, times(2)).registrarEstudianteEnLote(any(), any(Apoderado.class));
        // Una consulta por tipo de dato, no una por fila.
        verify(usuarioRepository, times(1)).findDnisExistentesPorTipo(anyCollection(), eq(Estudiante.class));
        verify(usuarioRepository, times(1)).findEmailsExistentes(anyCollection());
        verify(usuarioRepository, times(1)).findUsernamesExistentes(anyCollection());
        verify(apoderadoRepository, times(1)).findAllById(any());
    }

    @Test
    void dniQueYaExisteEnBd_informaLaFila() {
        simularApoderado(7L);
        when(usuarioRepository.findDnisExistentesPorTipo(anyCollection(), eq(Estudiante.class)))
                .thenReturn(List.of("22222222"));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () ->
                servicio.cargarEstudiantes(csv(
                        "Ana;Perez;;11111111;ana@x.com;;2012-05-10;7",
                        "Luis;Gomez;;22222222;luis@x.com;;2011-03-02;7")));

        assertEquals("Error en la fila 3: Ya existe un estudiante registrado con el DNI indicado.",
                ex.getMessage());
    }

    @Test
    void correoYaExisteEnBd_ignoraMayusculas() {
        simularApoderado(7L);
        when(usuarioRepository.findEmailsExistentes(anyCollection())).thenReturn(List.of("ANA@x.com"));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () ->
                servicio.cargarEstudiantes(csv("Ana;Perez;;11111111;ana@x.com;;2012-05-10;7")));

        assertEquals("Error en la fila 2: Ya existe un usuario registrado con el correo indicado.",
                ex.getMessage());
    }

    @Test
    void dniDuplicadoDentroDelMismoCsv_seDetecta() {
        simularApoderado(7L);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () ->
                servicio.cargarEstudiantes(csv(
                        "Ana;Perez;;11111111;ana@x.com;;2012-05-10;7",
                        "Luis;Gomez;;11111111;luis@x.com;;2011-03-02;7")));

        assertEquals("Error en la fila 3: Ya existe un estudiante registrado con el DNI indicado.",
                ex.getMessage());
    }

    @Test
    void usernameDuplicadoDentroDelMismoCsv_seDetecta() {
        simularApoderado(7L);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () ->
                servicio.cargarEstudiantes(csv(
                        "Ana;Perez;;11111111;ana@x.com;;2012-05-10;7",
                        "Ana;Lopez;;22222222;ana@otro.com;;2011-03-02;7")));

        assertEquals("Error en la fila 3: Ya existe un usuario registrado con ese identificador.",
                ex.getMessage());
    }

    @Test
    void apoderadoInexistente_informaLaFilaYNoRegistra() {
        when(apoderadoRepository.findAllById(any())).thenReturn(List.of());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () ->
                servicio.cargarEstudiantes(csv("Ana;Perez;;11111111;ana@x.com;;2012-05-10;99")));

        assertEquals("Error en la fila 2: No se encontró el apoderado con identificador 99.",
                ex.getMessage());
        verify(estudianteService, never()).registrarEstudianteEnLote(any(), any());
    }

    private void simularApoderado(Long id) {
        Apoderado apoderado = new Apoderado();
        apoderado.setId(id);
        when(apoderadoRepository.findAllById(any())).thenReturn(List.of(apoderado));
    }

    private MockMultipartFile csv(String... filas) {
        String contenido = ENCABEZADO + String.join("\n", filas) + "\n";
        return new MockMultipartFile(
                "archivo", "estudiantes.csv", "text/csv", contenido.getBytes(StandardCharsets.UTF_8));
    }
}

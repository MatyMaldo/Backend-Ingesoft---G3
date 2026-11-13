package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Seguridad_Auditoria.dominio.Apoderado;
import com.educoders.Seguridad_Auditoria.dominio.Estudiante;
import com.educoders.Seguridad_Auditoria.persistencia.ApoderadoRepository;
import com.educoders.Seguridad_Auditoria.persistencia.EstudianteRepository;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.EstudianteResponse;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarEstudianteRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * El administrador no define contraseñas: el estudiante nace sin contraseña y la establece él mismo
 * con el código de verificación (HU-SAT-03).
 */
@ExtendWith(MockitoExtension.class)
class EstudianteServiceImplTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private ApoderadoRepository apoderadoRepository;
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks
    private EstudianteServiceImpl service;

    @Test
    void registrarEstudiante_creaLaCuentaActivaYSinContrasena() {
        Apoderado apoderado = new Apoderado();
        apoderado.setId(7L);
        when(apoderadoRepository.findById(7L)).thenReturn(Optional.of(apoderado));
        when(estudianteRepository.save(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));

        EstudianteResponse respuesta = service.registrarEstudiante(new RegistrarEstudianteRequest(
                "Sofia", "Estudiante", null, "11000021", "sofia@example.com", null,
                "EST016", LocalDate.of(2013, 2, 11), 7L));

        ArgumentCaptor<Estudiante> captor = ArgumentCaptor.forClass(Estudiante.class);
        verify(estudianteRepository).save(captor.capture());
        assertNull(captor.getValue().getPasswordHash());
        assertEquals("sofia", captor.getValue().getUsername());
        assertEquals("EST016", respuesta.codigo());
        assertTrue(respuesta.activo());
    }
}

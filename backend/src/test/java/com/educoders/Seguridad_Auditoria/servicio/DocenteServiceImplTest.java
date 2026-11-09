package com.educoders.Seguridad_Auditoria.servicio;

import com.educoders.Seguridad_Auditoria.dominio.Docente;
import com.educoders.Seguridad_Auditoria.persistencia.DocenteRepository;
import com.educoders.Seguridad_Auditoria.persistencia.UsuarioRepository;
import com.educoders.Seguridad_Auditoria.presentacion.dto.RegistrarDocenteRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * El administrador no define contraseñas: el docente nace sin contraseña y la establece él mismo
 * con el código de verificación (HU-SAT-03).
 */
@ExtendWith(MockitoExtension.class)
class DocenteServiceImplTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private DocenteRepository docenteRepository;

    @InjectMocks
    private DocenteServiceImpl service;

    @Test
    void registrarDocente_creaLaCuentaActivaYSinContrasena() {
        when(docenteRepository.save(any(Docente.class))).thenAnswer(inv -> inv.getArgument(0));

        service.registrarDocente(new RegistrarDocenteRequest(
                "Marco", "Docente", null, "11000011", "marco@example.com", "910000011"));

        ArgumentCaptor<Docente> captor = ArgumentCaptor.forClass(Docente.class);
        verify(docenteRepository).save(captor.capture());
        assertNull(captor.getValue().getPasswordHash());
        assertEquals("marco", captor.getValue().getUsername());
        assertTrue(captor.getValue().isActivo());
    }
}


package com.educoders.Gestion_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.LibretaNotasDetalleResponse;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Component
public class LibretaNotasPdfGenerator {

    private static final float MARGEN = 50;

    private static final PDFont FUENTE =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    private static final PDFont FUENTE_NEGRITA =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    public byte[] generarPdf(
            LibretaNotasDetalleResponse libreta) throws IOException {

        if (libreta == null) {
            throw new IllegalArgumentException(
                    "La libreta no puede ser nula");
        }

        try (PDDocument documento = new PDDocument();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {

            try (Escritor escritor = new Escritor(documento)) {

                // Encabezado
                escritor.escribir("EDUCODERS - LIBRETA DE NOTAS", true);
                escritor.espacio();

                escritor.escribir(
                        "Libreta ID: " + texto(libreta.idLibreta()), false);

                escritor.escribir(
                        "Estudiante ID: " + texto(libreta.idEstudiante()), false);

                escritor.escribir(
                        "Año académico: " + texto(libreta.anioAcademico()), false);

                escritor.escribir(
                        "Bimestre: " + texto(libreta.nombreBimestre()), false);

                escritor.escribir(
                        "Fecha de generación: " +
                                texto(libreta.fechaGeneracion()), false);

                escritor.espacio();
                escritor.escribir("CALIFICACIONES POR COMPETENCIA", true);
                escritor.espacio();

                List<LibretaNotasDetalleResponse.NotaPorCompetencia> notas =
                        libreta.notasCompetencias();

                if (notas == null || notas.isEmpty()) {
                    escritor.escribir(
                            "No hay calificaciones registradas.", false);
                } else {
                    for (var nota : notas) {

                        escritor.escribir(
                                "Curso: " + texto(nota.nombreCurso()), true);

                        escritor.escribir(
                                "Competencia: " +
                                        texto(nota.nombreCompetencia()), false);

                        escritor.escribir(
                                "Calificación: " +
                                        texto(nota.valorNota()), false);

                        if (nota.descripcion() != null &&
                                !nota.descripcion().isBlank()) {
                            escritor.escribir(
                                    "Descripción: " +
                                            nota.descripcion(), false);
                        }

                        if (nota.conclusionDescriptiva() != null &&
                                !nota.conclusionDescriptiva().isBlank()) {
                            escritor.escribir(
                                    "Conclusión descriptiva: " +
                                            nota.conclusionDescriptiva(), false);
                        }

                        escritor.espacio();
                    }
                }

                escritor.espacio();
                escritor.escribir("COMENTARIOS DEL TUTOR", true);

                escritor.escribir(
                        texto(libreta.comentariosFinalesTutor()), false);
            }

            documento.save(salida);
            return salida.toByteArray();
        }
    }

    private static String texto(Object valor) {
        if (valor == null || valor.toString().isBlank()) {
            return "-";
        }
        return valor.toString();
    }

    // Clase auxiliar para escribir y gestionar páginas.
    private static class Escritor implements AutoCloseable {

        private final PDDocument documento;
        private PDPageContentStream contenido;
        private float posicionY;

        public Escritor(PDDocument documento) throws IOException {
            this.documento = documento;
            nuevaPagina();
        }

        private void nuevaPagina() throws IOException {

            if (contenido != null) {
                contenido.close();
            }

            PDPage pagina = new PDPage(PDRectangle.A4);
            documento.addPage(pagina);

            contenido = new PDPageContentStream(documento, pagina);
            posicionY = PDRectangle.A4.getHeight() - MARGEN;
        }

        public void espacio() {
            posicionY -= 10;
        }

        public void escribir(String texto, boolean negrita)
                throws IOException {

            PDFont fuente = negrita ? FUENTE_NEGRITA : FUENTE;
            float tamano = negrita ? 12 : 10;
            float anchoMaximo =
                    PDRectangle.A4.getWidth() - 2 * MARGEN;

            String restante = limpiar(texto);

            if (restante.isEmpty()) {
                return;
            }

            while (!restante.isEmpty()) {

                int fin = restante.length();

                while (fin > 1 &&
                        fuente.getStringWidth(
                                restante.substring(0, fin))
                                / 1000 * tamano > anchoMaximo) {
                    fin--;
                }

                if (fin < restante.length()) {
                    int espacio = restante.lastIndexOf(' ', fin - 1);
                    if (espacio > 0) {
                        fin = espacio;
                    }
                }

                String linea = restante.substring(0, fin).trim();

                if (posicionY < MARGEN + 20) {
                    nuevaPagina();
                }

                contenido.beginText();
                contenido.setFont(fuente, tamano);
                contenido.newLineAtOffset(MARGEN, posicionY);
                contenido.showText(linea);
                contenido.endText();

                posicionY -= tamano + 6;
                restante = restante.substring(fin).trim();
            }
        }

        private String limpiar(String valor) throws IOException {

            StringBuilder resultado = new StringBuilder();

            for (int i = 0; i < valor.length();) {
                int codigo = valor.codePointAt(i);
                i += Character.charCount(codigo);

                if (Character.isISOControl(codigo)) {
                    resultado.append(' ');
                    continue;
                }

                String caracter = new String(Character.toChars(codigo));

                try {
                    FUENTE.getStringWidth(caracter);
                    resultado.append(caracter);
                } catch (IllegalArgumentException ex) {
                    resultado.append('?');
                }
            }

            return resultado.toString()
                    .replaceAll("\\s+", " ")
                    .trim();
        }

        @Override
        public void close() throws IOException {
            if (contenido != null) {
                contenido.close();
            }
        }
    }
}

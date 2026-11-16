package com.educoders.Gestion_Academica_Motor_Evaluacion.servicio;

import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.LibretaNotasDetalleResponse;
import com.educoders.Gestion_Academica_Motor_Evaluacion.presentacion.dto.ProgresoCompetenciaAcumuladoResponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Genera el informe de progreso bimestral acumulado de EduCoders. */
@Component
public class LibretaNotasPdfGenerator {

    private static final float MARGEN = 42f;
    private static final float ANCHO = PDRectangle.A4.getWidth() - 2 * MARGEN;
    private static final float ALTO = PDRectangle.A4.getHeight();
    private static final float LIMITE_INFERIOR = 62f;
    private static final float ANCHO_COMPETENCIA = ANCHO - 4 * 41f;
    private static final float[] COLUMNAS = {
            ANCHO_COMPETENCIA, 41f, 41f, 41f, 41f
    };
    private static final PDFont NORMAL =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont NEGRITA =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private static final Color AZUL = new Color(26, 61, 96);
    private static final Color AZUL_CLARO = new Color(228, 237, 245);
    private static final Color FONDO = new Color(246, 248, 251);
    private static final Color TEXTO = new Color(38, 48, 59);
    private static final Color GRIS = new Color(104, 114, 126);
    private static final Color LINEA = new Color(216, 223, 230);

    /** Construye un PDF sin alterar las calificaciones almacenadas. */
    public byte[] generarPdf(LibretaNotasDetalleResponse libreta) throws IOException {
        if (libreta == null) {
            throw new IllegalArgumentException("La libreta no puede ser nula");
        }

        try (PDDocument documento = new PDDocument();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            try (Disenador pdf = new Disenador(documento, libreta.idLibreta())) {
                pdf.encabezado(libreta);
                pdf.datosGenerales(libreta);
                pdf.seccion("PROGRESO ACUMULADO POR COMPETENCIA");
                pdf.parrafo("Niveles de logro: AD (destacado), A (esperado), "
                        + "B (en proceso), C (en inicio).", 9f, GRIS);
                pdf.espacio(6f);
                pdf.tablaAcumulada(libreta.progresoAcumulado());

                pdf.seccion("CONCLUSIONES DEL BIMESTRE " + periodo(libreta.nombreBimestre()));
                pdf.conclusiones(libreta.notasCompetencias());

                pdf.seccion("RESUMEN DE ASISTENCIA");
                pdf.resumenAsistencia(libreta.inasistencias(), libreta.tardanzas());

                pdf.seccion("COMENTARIOS DEL TUTOR");
                pdf.parrafo(valor(libreta.comentariosFinalesTutor()), 10f, TEXTO);
                pdf.espacio(15f);
                pdf.parrafo("Documento académico informativo de EduCoders. "
                        + "El reporte muestra la información registrada hasta el "
                        + "bimestre indicado.", 8f, GRIS);
            }
            documento.save(salida);
            return salida.toByteArray();
        }
    }

    private static String valor(Object dato) {
        return dato == null || dato.toString().isBlank() ? "-" : dato.toString();
    }

    private static String periodo(String nombre) {
        if ("BIM_1".equals(nombre)) { return "I"; }
        if ("BIM_2".equals(nombre)) { return "II"; }
        if ("BIM_3".equals(nombre)) { return "III"; }
        if ("BIM_4".equals(nombre)) { return "IV"; }
        return valor(nombre);
    }

    /** Dibuja tablas y texto con ajuste de líneas y salto automático de página. */
    private static final class Disenador implements AutoCloseable {

        private final PDDocument documento;
        private final Long idLibreta;
        private PDPageContentStream contenido;
        private float y;
        private int numeroPagina;

        private Disenador(PDDocument documento, Long idLibreta) throws IOException {
            this.documento = documento;
            this.idLibreta = idLibreta;
            nuevaPagina();
        }

        private void nuevaPagina() throws IOException {
            if (contenido != null) {
                pieDePagina();
                contenido.close();
            }
            PDPage pagina = new PDPage(PDRectangle.A4);
            documento.addPage(pagina);
            contenido = new PDPageContentStream(documento, pagina);
            numeroPagina++;
            rectangulo(0f, ALTO - 12f, PDRectangle.A4.getWidth(), 12f, AZUL);
            y = ALTO - 43f;
            if (numeroPagina > 1) {
                escribir(MARGEN, y, "EDUCODERS  |  INFORME DE PROGRESO ACADEMICO",
                        NEGRITA, 9f, AZUL);
                y -= 26f;
            }
        }

        private void pieDePagina() throws IOException {
            linea(MARGEN, 48f, MARGEN + ANCHO, 48f, LINEA);
            escribir(MARGEN, 34f, "EduCoders  |  Libreta N. " + valor(idLibreta),
                    NORMAL, 8f, GRIS);
            String pagina = "Pagina " + numeroPagina;
            escribir(MARGEN + ANCHO - ancho(pagina, NORMAL, 8f), 34f,
                    pagina, NORMAL, 8f, GRIS);
        }

        private void espacio(float alto) throws IOException {
            asegurar(alto);
            y -= alto;
        }

        private void asegurar(float alto) throws IOException {
            if (y - alto < LIMITE_INFERIOR) {
                nuevaPagina();
            }
        }

        private void encabezado(LibretaNotasDetalleResponse libreta) throws IOException {
            asegurar(106f);
            rectangulo(MARGEN, y - 100f, ANCHO, 100f, AZUL);
            escribir(MARGEN + 17f, y - 25f, "EDUCODERS", NEGRITA, 17f, Color.WHITE);
            escribir(MARGEN + 17f, y - 47f, "INFORME DE PROGRESO ACADEMICO",
                    NEGRITA, 12f, Color.WHITE);
            String subtitulo = "Libreta parcial acumulativa  |  Corte "
                    + periodo(libreta.nombreBimestre()) + "  |  "
                    + valor(libreta.anioAcademico());
            escribir(MARGEN + 17f, y - 69f, subtitulo, NORMAL, 10f, Color.WHITE);
            y -= 117f;
            parrafo("Institucion: " + valor(libreta.nombreInstitucion()), 10f, AZUL);
        }

        private void datosGenerales(LibretaNotasDetalleResponse libreta) throws IOException {
            espacio(12f);
            asegurar(85f);
            float alto = 77f;
            rectangulo(MARGEN, y - alto, ANCHO, alto, FONDO);
            float x = MARGEN + 12f;
            float derecha = MARGEN + ANCHO / 2f + 12f;
            escribir(x, y - 17f, "Estudiante: " + valor(libreta.nombreEstudiante()),
                    NORMAL, 9f, TEXTO);
            escribir(x, y - 35f, "Seccion: " + valor(libreta.nombreSeccion()),
                    NORMAL, 9f, TEXTO);
            escribir(x, y - 53f, "Tutor: " + valor(libreta.nombreTutor()),
                    NORMAL, 9f, TEXTO);
            escribir(derecha, y - 17f, "Anio: " + valor(libreta.anioAcademico()),
                    NORMAL, 9f, TEXTO);
            escribir(derecha, y - 35f, "Bimestre: " + periodo(libreta.nombreBimestre()),
                    NORMAL, 9f, TEXTO);
            escribir(derecha, y - 53f, "Emitido: " + valor(libreta.fechaGeneracion()),
                    NORMAL, 9f, TEXTO);
            y -= alto + 3f;
        }

        private void seccion(String titulo) throws IOException {
            asegurar(36f);
            y -= 20f;
            escribir(MARGEN, y, titulo, NEGRITA, 11f, AZUL);
            y -= 7f;
            linea(MARGEN, y, MARGEN + ANCHO, y, LINEA);
            y -= 13f;
        }

        private void cabeceraTabla() throws IOException {
            asegurar(26f);
            rectangulo(MARGEN, y - 24f, ANCHO, 24f, AZUL);
            escribir(MARGEN + 8f, y - 16f, "AREA / COMPETENCIA",
                    NEGRITA, 9f, Color.WHITE);
            float x = MARGEN + ANCHO_COMPETENCIA;
            String[] titulos = {"I", "II", "III", "IV"};
            for (int i = 0; i < 4; i++) {
                centrar(x, y - 16f, 41f, titulos[i], NEGRITA, 9f, Color.WHITE);
                x += 41f;
            }
            y -= 24f;
        }

        private void cabeceraCurso(String curso) throws IOException {
            asegurar(23f);
            rectangulo(MARGEN, y - 21f, ANCHO, 21f, AZUL_CLARO);
            escribir(MARGEN + 8f, y - 14f, curso, NEGRITA, 9f, AZUL);
            y -= 21f;
        }

        private void tablaAcumulada(List<ProgresoCompetenciaAcumuladoResponse> progreso)
                throws IOException {
            if (progreso == null || progreso.isEmpty()) {
                parrafo("No hay calificaciones acumuladas registradas.", 10f, TEXTO);
                return;
            }
            Map<String, List<ProgresoCompetenciaAcumuladoResponse>> porCurso =
                    new LinkedHashMap<>();
            for (var fila : progreso) {
                porCurso.computeIfAbsent(valor(fila.nombreCurso()),
                        clave -> new ArrayList<>()).add(fila);
            }
            cabeceraTabla();
            boolean sombrear = false;
            for (var curso : porCurso.entrySet()) {
                String nombreCurso = limpiar(curso.getKey());
                cabeceraCurso(nombreCurso);
                for (var competencia : curso.getValue()) {
                    List<String> lineas = dividir(valor(competencia.nombreCompetencia()),
                            NORMAL, 9f, ANCHO_COMPETENCIA - 16f);
                    float alto = Math.max(26f, lineas.size() * 12f + 12f);
                    if (y - alto < LIMITE_INFERIOR) {
                        nuevaPagina();
                        cabeceraTabla();
                        cabeceraCurso(nombreCurso + " (continuacion)");
                    }
                    rectangulo(MARGEN, y - alto, ANCHO, alto,
                            sombrear ? FONDO : Color.WHITE);
                    float base = y - 15f;
                    for (String linea : lineas) {
                        escribir(MARGEN + 8f, base, linea, NORMAL, 9f, TEXTO);
                        base -= 12f;
                    }
                    String[] notas = {
                            valor(competencia.bimestre1()), valor(competencia.bimestre2()),
                            valor(competencia.bimestre3()), valor(competencia.bimestre4())
                    };
                    float x = MARGEN + ANCHO_COMPETENCIA;
                    for (int i = 0; i < 4; i++) {
                        centrar(x, y - alto / 2f - 3f, COLUMNAS[i + 1],
                                notas[i], NEGRITA, 10f, AZUL);
                        x += COLUMNAS[i + 1];
                    }
                    linea(MARGEN, y - alto, MARGEN + ANCHO, y - alto, LINEA);
                    y -= alto;
                    sombrear = !sombrear;
                }
            }
            espacio(5f);
        }

        private void conclusiones(List<LibretaNotasDetalleResponse.NotaPorCompetencia> notas)
                throws IOException {
            if (notas == null || notas.isEmpty()) {
                parrafo("No hay conclusiones registradas para este bimestre.", 10f, TEXTO);
                return;
            }
            for (var nota : notas) {
                asegurar(48f);
                String encabezado = valor(nota.nombreCurso()) + " / "
                        + valor(nota.nombreCompetencia()) + "  (" + valor(nota.valorNota()) + ")";
                parrafoConFuente(encabezado, 9.5f, AZUL, NEGRITA);
                if (nota.descripcion() != null && !nota.descripcion().isBlank()) {
                    parrafo("Desempeno: " + nota.descripcion(), 9f, TEXTO);
                }
                parrafo("Conclusion: " + valor(nota.conclusionDescriptiva()), 9f, TEXTO);
                espacio(7f);
            }
        }

        private void resumenAsistencia(long inasistencias, long tardanzas) throws IOException {
            asegurar(58f);
            float anchoTarjeta = (ANCHO - 14f) / 2f;
            rectangulo(MARGEN, y - 54f, anchoTarjeta, 54f, AZUL_CLARO);
            rectangulo(MARGEN + anchoTarjeta + 14f, y - 54f,
                    anchoTarjeta, 54f, FONDO);
            escribir(MARGEN + 12f, y - 18f, "INASISTENCIAS", NEGRITA, 9f, AZUL);
            escribir(MARGEN + 12f, y - 41f, String.valueOf(inasistencias),
                    NEGRITA, 16f, AZUL);
            float segunda = MARGEN + anchoTarjeta + 26f;
            escribir(segunda, y - 18f, "TARDANZAS", NEGRITA, 9f, AZUL);
            escribir(segunda, y - 41f, String.valueOf(tardanzas),
                    NEGRITA, 16f, AZUL);
            y -= 54f;
        }

        private void parrafo(String texto, float tamano, Color color) throws IOException {
            parrafoConFuente(texto, tamano, color, NORMAL);
        }

        private void parrafoConFuente(String texto, float tamano, Color color, PDFont fuente)
                throws IOException {
            List<String> lineas = dividir(texto, fuente, tamano, ANCHO);
            for (String linea : lineas) {
                asegurar(tamano + 5f);
                escribir(MARGEN, y, linea, fuente, tamano, color);
                y -= tamano + 5f;
            }
        }

        private void centrar(float x, float base, float anchoCelda, String texto,
                             PDFont fuente, float tamano, Color color) throws IOException {
            String limpio = limpiar(texto);
            escribir(x + Math.max(0f, (anchoCelda - ancho(limpio, fuente, tamano)) / 2f),
                    base, limpio, fuente, tamano, color);
        }

        private void escribir(float x, float base, String texto, PDFont fuente,
                              float tamano, Color color) throws IOException {
            String limpio = limpiar(texto);
            contenido.setNonStrokingColor(color);
            contenido.beginText();
            contenido.setFont(fuente, tamano);
            contenido.newLineAtOffset(x, base);
            contenido.showText(limpio);
            contenido.endText();
        }

        private void rectangulo(float x, float inferior, float ancho, float alto,
                                Color color) throws IOException {
            contenido.setNonStrokingColor(color);
            contenido.addRect(x, inferior, ancho, alto);
            contenido.fill();
        }

        private void linea(float x1, float y1, float x2, float y2, Color color)
                throws IOException {
            contenido.setStrokingColor(color);
            contenido.setLineWidth(0.7f);
            contenido.moveTo(x1, y1);
            contenido.lineTo(x2, y2);
            contenido.stroke();
        }

        private float ancho(String texto, PDFont fuente, float tamano) throws IOException {
            return fuente.getStringWidth(limpiar(texto)) * tamano / 1000f;
        }

        private List<String> dividir(String texto, PDFont fuente, float tamano, float maximo)
                throws IOException {
            String limpio = limpiar(valor(texto));
            List<String> resultado = new ArrayList<>();
            String actual = "";
            for (String palabra : limpio.split("\\s+")) {
                String candidato = actual.isEmpty() ? palabra : actual + " " + palabra;
                if (ancho(candidato, fuente, tamano) <= maximo) {
                    actual = candidato;
                } else {
                    if (!actual.isEmpty()) {
                        resultado.add(actual);
                        actual = "";
                    }
                    for (int i = 0; i < palabra.length(); i++) {
                        String unido = actual + palabra.charAt(i);
                        if (!actual.isEmpty() && ancho(unido, fuente, tamano) > maximo) {
                            resultado.add(actual);
                            actual = "";
                        }
                        actual += palabra.charAt(i);
                    }
                }
            }
            if (!actual.isEmpty()) {
                resultado.add(actual);
            }
            return resultado;
        }

        private String limpiar(String texto) throws IOException {
            StringBuilder salida = new StringBuilder();
            for (int i = 0; i < texto.length();) {
                int codigo = texto.codePointAt(i);
                i += Character.charCount(codigo);
                if (Character.isISOControl(codigo)) {
                    salida.append(' ');
                    continue;
                }
                String caracter = new String(Character.toChars(codigo));
                try {
                    NORMAL.getStringWidth(caracter);
                    salida.append(caracter);
                } catch (IllegalArgumentException ex) {
                    salida.append('?');
                }
            }
            return salida.toString().replaceAll("\\s+", " ").trim();
        }

        @Override
        public void close() throws IOException {
            if (contenido != null) {
                pieDePagina();
                contenido.close();
                contenido = null;
            }
        }
    }
}

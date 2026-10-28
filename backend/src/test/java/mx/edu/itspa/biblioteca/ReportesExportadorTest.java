package mx.edu.itspa.biblioteca;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;
import mx.edu.itspa.biblioteca.services.impl.ReportesExportador;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ReportesExportadorTest {
    @Test
    void csvConservaAcentosComillasYSaltosSinEjecutarFormulas() {
        String csv=new String(ReportesExportador.csv(List.of("titulo","paginas"),List.of(Map.of("titulo"," =HYPERLINK(\"prueba\")\nPátzcuaro","paginas",-5))),StandardCharsets.UTF_8);
        assertThat(csv).startsWith("\uFEFF").contains("\"' =HYPERLINK(\"\"prueba\"\")\nPátzcuaro\"").contains("\"-5\"");
    }

    @Test
    void xlsxEsZipOfficeYLosTextosSonCeldasSinFormulas() throws Exception {
        byte[] bytes=ReportesExportador.xlsx(List.of("titulo","cantidad"),List.of(Map.of("titulo","=1+2 <Pátzcuaro>","cantidad",7)));
        String hoja=null;int entradas=0;
        try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(bytes))){java.util.zip.ZipEntry entrada;while((entrada=zip.getNextEntry())!=null){entradas++;if(entrada.getName().equals("xl/worksheets/sheet1.xml"))hoja=new String(zip.readAllBytes(),StandardCharsets.UTF_8);}}
        assertThat(entradas).isEqualTo(6);
        assertThat(hoja).contains("t=\"inlineStr\"","=1+2 &lt;Pátzcuaro&gt;","<v>7</v>").doesNotContain("<f>");
    }

    @Test
    void pdfNativoPaginadoConservaDatosYTerminacionValida() {
        var filas=java.util.stream.IntStream.range(0,70).mapToObj(n->Map.<String,Object>of("titulo","Registro "+n+" Pátzcuaro (ITSPA)" )).toList();
        String pdf=new String(ReportesExportador.pdf("Reporte de catálogo",List.of("titulo"),filas),java.nio.charset.Charset.forName("windows-1252"));
        assertThat(pdf).startsWith("%PDF-1.4").contains("/Type /Pages /Count 5","Registro 69 Pátzcuaro \\(ITSPA\\)","startxref").endsWith("%%EOF\n");
    }
}

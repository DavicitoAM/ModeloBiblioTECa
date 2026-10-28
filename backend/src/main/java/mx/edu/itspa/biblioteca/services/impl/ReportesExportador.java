package mx.edu.itspa.biblioteca.services.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Exportadores nativos sin dependencia de Office; valores de usuario nunca se ejecutan como fórmulas. */
public final class ReportesExportador {
    private ReportesExportador() { }

    public static byte[] csv(List<String> columnas, List<Map<String, Object>> filas) {
        StringBuilder contenido = new StringBuilder("\uFEFF");
        contenido.append(String.join(",", columnas.stream().map(ReportesExportador::celdaCsv).toList())).append("\r\n");
        for (Map<String,Object> fila:filas) {
            contenido.append(String.join(",", columnas.stream().map(c -> celdaCsv(fila.get(c))).toList())).append("\r\n");
        }
        return contenido.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String celdaCsv(Object valor) {
        String texto = valor==null?"":String.valueOf(valor);
        String inicio = texto.stripLeading();
        if (!(valor instanceof Number) && (!inicio.isEmpty() && "=+-@".indexOf(inicio.charAt(0))>=0 || texto.startsWith("\t") || texto.startsWith("\r"))) texto = "'"+texto;
        return "\"" + texto.replace("\"","\"\"") + "\"";
    }

    public static byte[] xlsx(List<String> columnas, List<Map<String, Object>> filas) {
        StringBuilder hoja = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews><cols>");
        for (int i=0;i<columnas.size();i++) hoja.append("<col min=\"").append(i+1).append("\" max=\"").append(i+1).append("\" width=\"25\" customWidth=\"1\"/>");
        hoja.append("</cols><sheetData><row r=\"1\" ht=\"25\" customHeight=\"1\">");
        for (int i=0;i<columnas.size();i++) hoja.append(celdaXlsx(referencia(i,1),columnas.get(i),1));
        hoja.append("</row>");
        int numero=2;
        for (Map<String,Object> fila:filas) {
            hoja.append("<row r=\"").append(numero).append("\">");
            for(int i=0;i<columnas.size();i++) hoja.append(celdaXlsx(referencia(i,numero),fila.get(columnas.get(i)),0));
            hoja.append("</row>");numero++;
        }
        hoja.append("</sheetData>");
        if(!columnas.isEmpty())hoja.append("<autoFilter ref=\"A1:").append(referencia(columnas.size()-1,numero-1)).append("\"/>");
        hoja.append("</worksheet>");
        try(ByteArrayOutputStream bytes=new ByteArrayOutputStream();ZipOutputStream zip=new ZipOutputStream(bytes)) {
            entrada(zip,"[Content_Types].xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/><Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/></Types>");
            entrada(zip,"_rels/.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
            entrada(zip,"xl/workbook.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Reporte ITSPA\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            entrada(zip,"xl/_rels/workbook.xml.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/><Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>");
            entrada(zip,"xl/styles.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Noto Sans\"/></font><font><b/><color rgb=\"FFFFFFFF\"/><sz val=\"11\"/><name val=\"Noto Sans\"/></font></fonts><fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF1B396A\"/><bgColor indexed=\"64\"/></patternFill></fill></fills><borders count=\"1\"><border/></borders><cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs><cellXfs count=\"2\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"><alignment vertical=\"top\" wrapText=\"1\"/></xf><xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFill=\"1\" applyFont=\"1\"><alignment vertical=\"center\" wrapText=\"1\"/></xf></cellXfs><cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles></styleSheet>");
            entrada(zip,"xl/worksheets/sheet1.xml",hoja.toString());
            zip.finish();return bytes.toByteArray();
        }catch(IOException ex){throw new IllegalStateException("No se pudo generar el archivo XLSX.",ex);}
    }

    private static String celdaXlsx(String referencia,Object valor,int estilo) {
        String inicio="<c r=\""+referencia+"\" s=\""+estilo+"\"";
        if(valor instanceof Number)return inicio+"><v>"+valor+"</v></c>";
        if(valor instanceof Boolean b)return inicio+" t=\"b\"><v>"+(b?1:0)+"</v></c>";
        return inicio+" t=\"inlineStr\"><is><t xml:space=\"preserve\">"+xml(valor==null?"":String.valueOf(valor))+"</t></is></c>";
    }
    private static String referencia(int columna,int fila){StringBuilder s=new StringBuilder();for(int n=columna+1;n>0;n=(n-1)/26)s.insert(0,(char)('A'+(n-1)%26));return s.append(fila).toString();}
    private static String xml(String texto){return texto.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]","").replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");}
    private static void entrada(ZipOutputStream zip,String nombre,String texto)throws IOException{zip.putNextEntry(new ZipEntry(nombre));zip.write(texto.getBytes(StandardCharsets.UTF_8));zip.closeEntry();}

    /** PDF paginado de fichas: conserva todos los campos sin recortar columnas anchas. */
    public static byte[] pdf(String titulo,List<String> columnas,List<Map<String,Object>> filas) {
        List<String> lineas=new ArrayList<>();
        lineas.add("Registros: "+filas.size());lineas.add("");
        int n=0;
        for(Map<String,Object> fila:filas){lineas.add("Registro "+(++n));for(String columna:columnas)envolver(lineas,columna.replace('_',' ')+": "+(fila.get(columna)==null?"—":fila.get(columna)),105);lineas.add("");}
        if(filas.isEmpty())lineas.add("No existen registros para los filtros seleccionados.");
        int totalPaginas=Math.max(1,(lineas.size()+44)/45);
        List<byte[]> objetos=new ArrayList<>();
        objetos.add(bytes("<< /Type /Catalog /Pages 2 0 R >>"));
        StringBuilder hijos=new StringBuilder();for(int i=0;i<totalPaginas;i++)hijos.append(4+i*2).append(" 0 R ");
        objetos.add(bytes("<< /Type /Pages /Count "+totalPaginas+" /Kids ["+hijos+"] >>"));
        objetos.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"));
        for(int pagina=0;pagina<totalPaginas;pagina++) {
            int contenido=5+pagina*2;
            objetos.add(bytes("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 3 0 R >> >> /Contents "+contenido+" 0 R >>"));
            StringBuilder dibujo=new StringBuilder("0.106 0.224 0.416 rg 0 784 595 58 re f\n1 1 1 rg BT /F1 15 Tf 34 807 Td (").append(pdfTexto(titulo)).append(") Tj ET\n0.12 0.14 0.18 rg BT /F1 9 Tf 14 TL 34 756 Td\n");
            int fin=Math.min(lineas.size(),(pagina+1)*45);
            for(int i=pagina*45;i<fin;i++)dibujo.append('(').append(pdfTexto(lineas.get(i))).append(") Tj T*\n");
            dibujo.append("ET\n0.45 0.45 0.45 rg BT /F1 9 Tf 34 38 Td (ITSPA - Biblioteca     |     P").append(pdfTexto("ágina "+(pagina+1)+" de "+totalPaginas)).append(") Tj ET\n");
            byte[] datos=bytes(dibujo.toString());
            ByteArrayOutputStream stream=new ByteArrayOutputStream();
            try{stream.write(bytes("<< /Length "+datos.length+" >>\nstream\n"));stream.write(datos);stream.write(bytes("\nendstream"));}catch(IOException ex){throw new IllegalStateException(ex);}
            objetos.add(stream.toByteArray());
        }
        try(ByteArrayOutputStream resultado=new ByteArrayOutputStream()) {
            resultado.write(bytes("%PDF-1.4\n%âãÏÓ\n"));List<Integer> posiciones=new ArrayList<>();
            for(int i=0;i<objetos.size();i++){posiciones.add(resultado.size());resultado.write(bytes((i+1)+" 0 obj\n"));resultado.write(objetos.get(i));resultado.write(bytes("\nendobj\n"));}
            int xref=resultado.size();resultado.write(bytes("xref\n0 "+(objetos.size()+1)+"\n0000000000 65535 f \n"));
            for(int posicion:posiciones)resultado.write(bytes(String.format(Locale.ROOT,"%010d 00000 n \n",posicion)));
            resultado.write(bytes("trailer\n<< /Size "+(objetos.size()+1)+" /Root 1 0 R >>\nstartxref\n"+xref+"\n%%EOF\n"));return resultado.toByteArray();
        }catch(IOException ex){throw new IllegalStateException("No se pudo generar el PDF.",ex);}
    }
    private static void envolver(List<String> lineas,String texto,int ancho){for(String parrafo:texto.replace('\r',' ').split("\n",-1)){while(parrafo.length()>ancho){int fin=parrafo.lastIndexOf(' ',ancho);if(fin<ancho/2)fin=ancho;lineas.add(parrafo.substring(0,fin));parrafo="  "+parrafo.substring(fin).stripLeading();}lineas.add(parrafo);}}
    private static String pdfTexto(String texto){return texto.replace("\\","\\\\").replace("(","\\(").replace(")","\\)").replace('\n',' ').replace('\r',' ');}
    private static byte[] bytes(String s){return s.getBytes(Charset.forName("windows-1252"));}
}

package mx.edu.itspa.biblioteca.services.impl;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import mx.edu.itspa.biblioteca.repositorio.OperacionBibliotecaRepositorio;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Datos bibliográficos identificados como demostración, optativos y sin personas ni políticas. */
@Service
@ConditionalOnProperty(name="biblioteca.demo.enabled",havingValue="true")
public class CargaCatalogoDemostracion implements CommandLineRunner {
    private final OperacionBibliotecaRepositorio db;
    public CargaCatalogoDemostracion(OperacionBibliotecaRepositorio db){this.db=db;}
    @Override @Transactional public void run(String... args) {
        db.bloquearOperacion();long ubicacion=buscarOCrear("ubicaciones","nombre","Área de demostración",m("nombre","Área de demostración","seccion","Académica","estante","D-01","nivel","1","estatus",true));
        long idioma=buscarOCrear("idiomas","nombre","Español",m("nombre","Español","estatus",true));
        String[][] titulos={
            {"Fundamentos de programación","Informática","Conceptos de algoritmos, estructuras de control y resolución de problemas.","320"},
            {"Bases de datos relacionales","Informática","Diseño de esquemas, consultas SQL e integridad de la información.","280"},
            {"Cálculo para ingeniería","Matemáticas","Límites, derivadas e integrales aplicadas a problemas de ingeniería.","420"},
            {"Álgebra lineal aplicada","Matemáticas","Matrices, espacios vectoriales y modelos de transformación.","310"},
            {"Física: ciencia y tecnología","Ciencias","Mecánica, energía y fundamentos experimentales.","380"},
            {"Administración de proyectos","Administración","Planificación, seguimiento de recursos y trabajo colaborativo.","250"},
            {"Comunicación y escritura académica","Humanidades","Lectura crítica, argumentación y elaboración de trabajos académicos.","220"},
            {"Desarrollo sostenible e innovación","Ingeniería","Tecnología, responsabilidad ambiental y proyectos para la comunidad.","295"}
        };
        for(int i=0;i<titulos.length;i++) {
            String[] t=titulos[i];String codigo="DEMO-ITSPA-"+String.format("%02d",i+1)+"-";
            long libro;
            var existente=db.primero("SELECT libro_id FROM ejemplares WHERE codigo_barras=?",codigo+"01");
            if(existente.isPresent())libro=((Number)existente.get().get("libro_id")).longValue();
            else {
                libro=db.insertar("libros",m("titulo",t[0],"descripcion",t[2]+" Registro de demostración de este proyecto; verificar la ficha bibliográfica al incorporar material real.","edicion","Demostración académica","anio_publicacion",2026,"num_paginas",Integer.parseInt(t[3]),"estatus",true,"idioma_id",idioma));
                long categoria=buscarOCrear("categorias","nombre",t[1],m("nombre",t[1],"descripcion","Clasificación académica del catálogo","estatus",true));
                db.ejecutar("INSERT INTO libros_categorias(libro_id,categoria_id) VALUES (?,?) ON CONFLICT DO NOTHING",libro,categoria);
            }
            for(int c=1;c<=2;c++)if(db.numero("SELECT count(*) FROM ejemplares WHERE codigo_barras=?",codigo+String.format("%02d",c))==0)
                db.insertar("ejemplares",m("codigo_barras",codigo+String.format("%02d",c),"fecha_adquisicion",LocalDate.of(2026,10,20),"estado","DISPONIBLE","estado_conservacion","BUENO","estatus",true,"libro_id",libro,"ubicacion_id",ubicacion,"observaciones","Ejemplar de demostración del proyecto actual"));
        }
    }
    private long buscarOCrear(String tabla,String campo,String valor,Map<String,Object> datos){return db.primero("SELECT id FROM "+tabla+" WHERE "+campo+"=? ORDER BY id LIMIT 1",valor).map(r->((Number)r.get("id")).longValue()).orElseGet(()->db.insertar(tabla,datos));}
    private static Map<String,Object> m(Object... pares){Map<String,Object> r=new LinkedHashMap<>();for(int i=0;i<pares.length;i+=2)r.put(pares[i].toString(),pares[i+1]);return r;}
}

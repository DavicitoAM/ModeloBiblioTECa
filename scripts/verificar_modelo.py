#!/usr/bin/env python3
"""Verificacion ESTATICA Java/JPA frente al DDL V1+V2, exclusivamente con stdlib.

No compila Java, no arranca Hibernate/Flyway y no establece conexiones.
El analizador reconoce el subconjunto DDL utilizado por estas migraciones:
CREATE TABLE/INDEX y ALTER TABLE ADD COLUMN/CONSTRAINT. Construcciones ajenas
se rechazan expresamente; comas y punto y coma en cadenas o parentesis no
se interpretan como separadores. No es un parser general de Java/PostgreSQL.
"""
from __future__ import annotations

import argparse
from dataclasses import dataclass, field
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import re
import sys

V1_SHA256 = "1A6E65BA794EC18B1B6BC1B400EA615CDE7AC889C65419CD90815DBBC602CAEB"
EXPECTED = {"entidades": 70, "enums": 9, "tablas": 77, "puentes": 7, "subclases_joined": 4}
INTERNAL = "mx.edu.itspa.biblioteca."


class ParseError(ValueError):
    pass


def tokens(text: str, *, java: bool = False) -> list[str]:
    """Elimina comentarios fuera de cadenas, conservando literales completos."""
    result = []
    i = 0
    while i < len(text):
        c = text[i]
        if c.isspace():
            i += 1
        elif text.startswith("/*", i):
            depth, j = 1, i + 2
            while j < len(text) and depth:
                if text.startswith("/*", j):
                    depth += 1
                    j += 2
                elif text.startswith("*/", j):
                    depth -= 1
                    j += 2
                else:
                    j += 1
            if depth:
                raise ParseError("Comentario sin cierre")
            i = j
        elif text.startswith("//" if java else "--", i):
            j = text.find("\n", i)
            i = len(text) if j < 0 else j + 1
        elif c in ("'", '"'):
            start, quote = i, c
            i += 1
            while i < len(text):
                if java and text[i] == "\\":
                    i += 2
                elif text[i] == quote:
                    if not java and i + 1 < len(text) and text[i + 1] == quote:
                        i += 2
                    else:
                        i += 1
                        break
                else:
                    i += 1
            else:
                raise ParseError("Literal sin cierre")
            result.append(text[start:i])
        elif c.isalpha() or c in "_$":
            j = i + 1
            while j < len(text) and (text[j].isalnum() or text[j] in "_$"):
                j += 1
            result.append(text[i:j])
            i = j
        elif c.isdigit():
            j = i + 1
            while j < len(text) and (text[j].isdigit() or text[j] == "."):
                j += 1
            result.append(text[i:j])
            i = j
        else:
            result.append(c)
            i += 1
    return result


def matching(ts: list[str], start: int) -> int:
    pairs = {"(": ")", "[": "]", "{": "}"}
    if ts[start] not in pairs:
        raise ParseError("Se esperaba apertura de grupo")
    stack = [pairs[ts[start]]]
    for i in range(start + 1, len(ts)):
        t = ts[i]
        if t in pairs:
            stack.append(pairs[t])
        elif t in pairs.values():
            if not stack or t != stack.pop():
                raise ParseError("Grupos desbalanceados")
            if not stack:
                return i
    raise ParseError("Grupo sin cierre")


def split_top(ts: list[str], separator: str = ",") -> list[list[str]]:
    result, current = [], []
    pairs, stack = {"(": ")", "[": "]", "{": "}"}, []
    for t in ts:
        if t == separator and not stack:
            result.append(current)
            current = []
            continue
        current.append(t)
        if t in pairs:
            stack.append(pairs[t])
        elif t in pairs.values():
            if not stack or stack.pop() != t:
                raise ParseError("Grupos desbalanceados al separar")
    if stack:
        raise ParseError("Grupo sin cierre al separar")
    if current:
        result.append(current)
    return result


def ident(t: str) -> str:
    if t.startswith('"'):
        return t[1:-1].replace('""', '"')
    return t.lower()


def group(ts: list[str], i: int) -> tuple[list[str], int]:
    j = matching(ts, i)
    return ts[i + 1:j], j + 1


def col_list(ts: list[str], i: int) -> tuple[tuple[str, ...], int]:
    inside, end = group(ts, i)
    columns = []
    for part in split_top(inside):
        if len(part) != 1:
            raise ParseError("Columna/expresion SQL compuesta no soportada: " + " ".join(part))
        columns.append(ident(part[0]))
    return tuple(columns), end


@dataclass
class SqlColumn:
    kind: str
    length: int | None = None
    precision: int | None = None
    scale: int | None = None
    nullable: bool = True
    identity: bool = False


@dataclass
class SqlTable:
    columns: dict[str, SqlColumn] = field(default_factory=dict)
    primary: tuple[str, ...] = ()
    unique: set[tuple[str, ...]] = field(default_factory=set)
    checks: list[list[str]] = field(default_factory=list)


@dataclass
class ForeignKey:
    table: str
    columns: tuple[str, ...]
    target: str
    target_columns: tuple[str, ...]
    name: str | None = None

    def signature(self) -> tuple:
        return self.table, self.columns, self.target, self.target_columns


class Schema:
    def __init__(self):
        self.tables: dict[str, SqlTable] = {}
        self.fks: list[ForeignKey] = []
        self.indexes: dict[str, tuple[str, tuple[str, ...]]] = {}
        self.statements = 0

    def constraint(self, table: str, ts: list[str]) -> None:
        name = None
        if ts[0].upper() == "CONSTRAINT":
            name, ts = ident(ts[1]), ts[2:]
        up = [t.upper() for t in ts]
        target = self.tables[table]
        if up[:2] == ["PRIMARY", "KEY"]:
            target.primary, end = col_list(ts, 2)
            if end != len(ts):
                raise ParseError("Sufijo PRIMARY KEY no soportado")
        elif up[:1] == ["UNIQUE"]:
            columns, end = col_list(ts, 1)
            if end != len(ts):
                raise ParseError("Sufijo UNIQUE no soportado")
            target.unique.add(columns)
        elif up[:2] == ["FOREIGN", "KEY"]:
            columns, i = col_list(ts, 2)
            if ts[i].upper() != "REFERENCES":
                raise ParseError("FOREIGN KEY sin REFERENCES")
            dest = ident(ts[i + 1])
            dest_cols, end = col_list(ts, i + 2)
            if end != len(ts):
                raise ParseError("Accion/sufijo FK no soportado: " + " ".join(ts[end:]))
            self.fks.append(ForeignKey(table, columns, dest, dest_cols, name))
        elif up[:1] == ["CHECK"]:
            inside, end = group(ts, 1)
            if end != len(ts):
                raise ParseError("Sufijo CHECK no soportado")
            target.checks.append(inside)
        else:
            raise ParseError("Restriccion SQL no soportada: " + " ".join(ts))

    def column(self, table: str, ts: list[str]) -> None:
        if len(ts) < 2:
            raise ParseError("Definicion de columna incompleta")
        name, kind, i = ident(ts[0]), ts[1].upper(), 2
        if name in self.tables[table].columns:
            raise ParseError("Columna SQL duplicada: " + table + "." + name)
        sizes = []
        if i < len(ts) and ts[i] == "(":
            body, i = group(ts, i)
            sizes = [int("".join(p)) for p in split_top(body)]
        if kind == "CHARACTER" and i < len(ts) and ts[i].upper() == "VARYING":
            kind, i = "VARCHAR", i + 1
        if kind in ("TIMESTAMP", "TIME") and i < len(ts) and ts[i].upper() in ("WITH", "WITHOUT"):
            zone = ts[i:i + 3]
            if len(zone) != 3 or [t.upper() for t in zone[1:]] != ["TIME", "ZONE"]:
                raise ParseError("Tipo temporal SQL no reconocido")
            if zone[0].upper() == "WITH":
                kind += "_TZ"
            i += 3
        identity = kind in ("BIGSERIAL", "SERIAL")
        kind = {"BIGSERIAL": "BIGINT", "INT8": "BIGINT", "SERIAL": "INTEGER",
                "INT": "INTEGER", "INT4": "INTEGER", "BOOL": "BOOLEAN",
                "DECIMAL": "NUMERIC"}.get(kind, kind)
        if kind not in {"BIGINT", "INTEGER", "BOOLEAN", "VARCHAR", "TEXT", "DATE",
                        "TIMESTAMP", "TIME", "NUMERIC", "SMALLINT", "REAL", "DOUBLE"}:
            raise ParseError("Tipo SQL no soportado: " + kind)
        col = SqlColumn(kind, identity=identity)
        if kind == "VARCHAR":
            if len(sizes) != 1:
                raise ParseError("VARCHAR sin longitud explicita")
            col.length = sizes[0]
        if kind == "NUMERIC":
            if len(sizes) != 2:
                raise ParseError("NUMERIC sin precision/escala explicitas")
            col.precision, col.scale = sizes
        tail, j = [], i
        while j < len(ts):
            if ts[j] in ("(", "[", "{"):
                j = matching(ts, j) + 1
            else:
                tail.append(ts[j].upper())
                j += 1
        col.nullable = not any(tail[j:j + 2] in (["NOT", "NULL"], ["PRIMARY", "KEY"])
                               for j in range(len(tail)))
        self.tables[table].columns[name] = col
        if "PRIMARY" in tail:
            self.tables[table].primary = (name,)
        if "UNIQUE" in tail:
            self.tables[table].unique.add((name,))
        # Restricciones inline, sin confundir palabras dentro de CHECK/DEFAULT.
        j = i
        while j < len(ts):
            word = ts[j].upper()
            if word == "CHECK":
                inside, j = group(ts, j + 1)
                self.tables[table].checks.append(inside)
            elif word == "REFERENCES":
                dest = ident(ts[j + 1])
                dest_cols, j = col_list(ts, j + 2)
                self.fks.append(ForeignKey(table, (name,), dest, dest_cols))
            elif ts[j] in ("(", "[", "{"):
                j = matching(ts, j) + 1
            else:
                j += 1

    def item(self, table: str, ts: list[str]) -> None:
        if ts[0].upper() in ("CONSTRAINT", "PRIMARY", "UNIQUE", "FOREIGN", "CHECK"):
            self.constraint(table, ts)
        else:
            self.column(table, ts)

    def parse(self, text: str, source: str) -> None:
        for n, ts in enumerate(split_top(tokens(text), ";"), 1):
            if not ts:
                continue
            self.statements += 1
            up = [t.upper() for t in ts]
            try:
                if up[:2] == ["CREATE", "TABLE"]:
                    table = ident(ts[2])
                    if table in self.tables:
                        raise ParseError("Tabla duplicada: " + table)
                    body, end = group(ts, 3)
                    if end != len(ts):
                        raise ParseError("Sufijo CREATE TABLE no soportado")
                    self.tables[table] = SqlTable()
                    for part in split_top(body):
                        self.item(table, part)
                elif up[:2] == ["ALTER", "TABLE"]:
                    table = ident(ts[2])
                    if table not in self.tables:
                        raise ParseError("ALTER antes de CREATE: " + table)
                    for action in split_top(ts[3:]):
                        if action[0].upper() != "ADD":
                            raise ParseError("ALTER solo admite ADD en estas migraciones")
                        action = action[1:]
                        if action[0].upper() == "COLUMN":
                            action = action[1:]
                        self.item(table, action)
                elif up[:2] in (["CREATE", "INDEX"], ["CREATE", "UNIQUE"]):
                    unique = up[1] == "UNIQUE"
                    k = 3 if unique else 2
                    if unique and up[2] != "INDEX":
                        raise ParseError("CREATE UNIQUE no es INDEX")
                    name = ident(ts[k])
                    if ts[k + 1].upper() != "ON":
                        raise ParseError("Indice sin ON")
                    table = ident(ts[k + 2])
                    columns, end = col_list(ts, k + 3)
                    if table not in self.tables:
                        raise ParseError("Indice de tabla inexistente: " + table)
                    if name in self.indexes:
                        raise ParseError("Indice duplicado: " + name)
                    self.indexes[name] = (table, columns)
                    # Un indice parcial no acredita unicidad global.
                    if unique and end == len(ts):
                        self.tables[table].unique.add(columns)
                    elif end < len(ts) and ts[end].upper() != "WHERE":
                        raise ParseError("Sufijo de indice no soportado")
                else:
                    raise ParseError("Sentencia SQL no soportada: " + " ".join(ts[:6]))
            except (IndexError, ValueError) as exc:
                raise ParseError(f"{source}, sentencia {n}: {exc}") from exc
        for table in self.tables.values():
            for name in table.primary:
                if name not in table.columns:
                    raise ParseError("PRIMARY KEY de columna inexistente")
                table.columns[name].nullable = False


@dataclass
class Annotation:
    name: str
    args: dict[str, list[str]]


def annotation(ts: list[str], i: int) -> tuple[Annotation, int]:
    if ts[i] != "@":
        raise ParseError("Se esperaba anotacion Java")
    i += 1
    name = ts[i]
    i += 1
    while i + 1 < len(ts) and ts[i] == ".":
        name, i = ts[i + 1], i + 2
    args = {}
    if i < len(ts) and ts[i] == "(":
        body, i = group(ts, i)
        for part in split_top(body):
            if len(part) >= 2 and part[1] == "=":
                key, value = part[0], part[2:]
            else:
                key, value = "value", part
            if key in args:
                raise ParseError("Argumento Java duplicado: " + key)
            args[key] = value
    return Annotation(name, args), i


def literal(a: Annotation, name: str, default=None):
    value = a.args.get(name)
    if value is None:
        return default
    if len(value) != 1:
        raise ParseError(f"Literal compuesto no soportado en @{a.name}.{name}")
    v = value[0]
    if v.startswith('"'):
        return json.loads(v)
    if v in ("true", "false"):
        return v == "true"
    if v.isdigit():
        return int(v)
    return v


def nested(a: Annotation, name: str) -> list[Annotation]:
    result, ts, i = [], a.args.get(name, []), 0
    while i < len(ts):
        if ts[i] == "@":
            value, i = annotation(ts, i)
            result.append(value)
        else:
            i += 1
    return result


@dataclass
class JavaField:
    name: str
    type: str
    annotations: dict[str, Annotation]
    static: bool = False


@dataclass
class JavaType:
    name: str
    package: str
    path: Path
    annotations: dict[str, Annotation]
    fields: dict[str, JavaField]
    parent: str | None
    imports: dict[str, str]
    constants: set[str] | None = None

    @property
    def qualified(self):
        return self.package + "." + self.name

    @property
    def table(self):
        a = self.annotations.get("Table")
        return literal(a, "name") if a else None


def parse_java(path: Path) -> JavaType:
    ts = tokens(path.read_text(encoding="utf-8-sig"), java=True)
    package, imports, anns, i = "", {}, {}, 0
    while i < len(ts):
        if ts[i] == "package":
            end = ts.index(";", i)
            package, i = "".join(ts[i + 1:end]), end + 1
        elif ts[i] == "import":
            end = ts.index(";", i)
            imp = "".join(ts[i + 1:end])
            imports[imp.split(".")[-1]] = imp
            i = end + 1
        elif ts[i] == "@":
            a, i = annotation(ts, i)
            anns[a.name] = a
        elif ts[i] in ("class", "enum"):
            break
        else:
            i += 1
    if i >= len(ts):
        raise ParseError("Archivo sin clase/enum: " + str(path))
    kind, name, i = ts[i], ts[i + 1], i + 2
    parent = None
    if i < len(ts) and ts[i] == "extends":
        parent, i = ts[i + 1], i + 2
    while i < len(ts) and ts[i] != "{":
        i += 1
    body, _ = group(ts, i)
    if kind == "enum":
        before_methods = split_top(body, ";")[0]
        constants = {part[0] for part in split_top(before_methods) if part}
        return JavaType(name, package, path, anns, {}, parent, imports, constants)
    fields, pending, i = {}, {}, 0
    modifiers = {"public", "private", "protected", "static", "final", "transient", "volatile"}
    while i < len(body):
        if body[i] == "@":
            a, i = annotation(body, i)
            pending[a.name] = a
            continue
        if body[i] == ";":
            i += 1
            pending = {}
            continue
        start, angles = i, 0
        while i < len(body):
            t = body[i]
            if t == "<":
                angles += 1
            elif t == ">":
                angles -= 1
            if not angles and t in ("=", ";", "(", "{"):
                break
            i += 1
        if i >= len(body):
            if body[start:]:
                raise ParseError("Miembro Java no reconocido en " + name)
            break
        header, delimiter = body[start:i], body[i]
        if delimiter in ("(", "{"):  # Metodo, constructor o bloque inicializador.
            i = matching(body, i) + 1
            while i < len(body) and body[i] not in ("{", ";"):
                i += 1
            if i < len(body) and body[i] == "{":
                i = matching(body, i) + 1
            elif i < len(body):
                i += 1
            pending = {}
            continue
        raw = [t for t in header if t not in modifiers]
        if len(raw) < 2:
            raise ParseError("Campo sin tipo/nombre en " + name)
        fname, ftype = raw[-1], "".join(raw[:-1])
        if fname in fields:
            raise ParseError("Campo Java duplicado: " + name + "." + fname)
        fields[fname] = JavaField(fname, ftype, pending, "static" in header)
        pending = {}
        while i < len(body) and body[i] != ";":
            if body[i] in ("(", "[", "{"):
                i = matching(body, i) + 1
            else:
                i += 1
        i += 1
    return JavaType(name, package, path, anns, fields, parent, imports)


def parser_self_check() -> None:
    probe = Schema()
    probe.parse("""-- ;, ()
        CREATE TABLE a (id BIGSERIAL PRIMARY KEY, texto VARCHAR(20) DEFAULT 'x,y);--',
          cantidad NUMERIC(12,2), CONSTRAINT c CHECK (cantidad IN (1,2)));
        CREATE TABLE b (id BIGINT PRIMARY KEY, a_id BIGINT);
        ALTER TABLE b ADD COLUMN nota VARCHAR(9), ADD CONSTRAINT f FOREIGN KEY(a_id) REFERENCES a(id);
        CREATE UNIQUE INDEX ix ON b(a_id) WHERE nota IS NULL;""", "autocomprobacion")
    if len(probe.tables) != 2 or len(probe.fks) != 1 or probe.tables["a"].columns["cantidad"].scale != 2:
        raise ParseError("Fallo de autocomprobacion SQL")
    a, end = annotation(tokens('@JoinTable(name="a,b", joinColumns={@JoinColumn(name="x", foreignKey=@ForeignKey(name="f"))})', java=True), 0)
    if literal(a, "name") != "a,b" or len(nested(a, "joinColumns")) != 1 or not end:
        raise ParseError("Fallo de autocomprobacion de anotaciones")


class Audit:
    def __init__(self, root: Path):
        self.root = root
        self.errors: list[str] = []
        self.warnings: list[str] = []
        self.stats: dict[str, int | str] = {}
        self.schema = Schema()
        self.types: dict[str, JavaType] = {}
        self.entities: dict[str, JavaType] = {}
        self.mapped_tables: set[str] = set()
        self.expected_columns: dict[str, set[str]] = {}
        self.covered_fk: set[tuple] = set()

    def error(self, message):
        self.errors.append(message)

    def resolve(self, owner: JavaType, name: str) -> JavaType | None:
        name = re.sub(r"^(?:List|Set|Collection)<(.+)>$", r"\1", name)
        full = owner.imports.get(name, owner.package + "." + name)
        return self.types.get(full)

    def require_fk(self, table, column, target, target_column="id", explicit_name=None):
        signature = table, (column,), target, (target_column,)
        found = [fk for fk in self.schema.fks if fk.signature() == signature]
        if not found:
            self.error(f"FK ausente: {table}.{column} -> {target}.{target_column}")
        else:
            self.covered_fk.add(signature)
            if explicit_name and not any(fk.name == explicit_name for fk in found):
                self.error(f"Nombre FK distinto: {table}.{column}, Java={explicit_name}")

    def column(self, table, column, kind, nullable, length=None, precision=None, scale=None, unique=False):
        self.expected_columns.setdefault(table, set()).add(column)
        sql_table = self.schema.tables.get(table)
        sql = sql_table.columns.get(column) if sql_table else None
        if sql is None:
            self.error(f"Columna SQL ausente: {table}.{column}")
            return
        if sql.kind != kind:
            self.error(f"Tipo distinto {table}.{column}: Java={kind}, SQL={sql.kind}")
        if sql.nullable != nullable:
            self.error(f"Nulabilidad distinta {table}.{column}: Java={nullable}, SQL={sql.nullable}")
        if length is not None and sql.length != length:
            self.error(f"Longitud distinta {table}.{column}: Java={length}, SQL={sql.length}")
        if precision is not None and (sql.precision, sql.scale) != (precision, scale):
            self.error(f"Precision/escala distinta {table}.{column}: Java={precision},{scale}, SQL={sql.precision},{sql.scale}")
        if unique and (column,) not in sql_table.unique and sql_table.primary != (column,):
            self.error(f"Unicidad SQL ausente: {table}.{column}")

    def scalar(self, owner, f, a):
        column = literal(a, "name")
        if not column:
            self.error(f"Alias @Column ausente: {owner.name}.{f.name}")
            return
        nullable = literal(a, "nullable", True) and "Id" not in f.annotations
        enum = self.resolve(owner, f.type)
        if enum and enum.constants is not None:
            ea = f.annotations.get("Enumerated")
            if not ea or ea.args.get("value") != ["EnumType", ".", "STRING"]:
                self.error(f"Enum sin STRING: {owner.name}.{f.name}")
            length = literal(a, "length", 255)
            if any(len(v) > length for v in enum.constants):
                self.error(f"Enum excede longitud: {owner.name}.{f.name}")
            self.column(owner.table, column, "VARCHAR", nullable, length, unique=literal(a, "unique", False))
            # Coteja los CHECK IN que existan; no afirma validar expresiones generales.
            for check in self.schema.tables[owner.table].checks:
                up = [t.upper() for t in check]
                if len(check) > 2 and ident(check[0]) == column and up[1] == "IN" and check[2] == "(":
                    inside, _ = group(check, 2)
                    actual = {p[0][1:-1].replace("''", "'") for p in split_top(inside) if len(p) == 1 and p[0].startswith("'")}
                    if actual != enum.constants:
                        self.error(f"CHECK enum distinto: {owner.table}.{column}")
            return
        kinds = {"Long": "BIGINT", "long": "BIGINT", "Integer": "INTEGER", "int": "INTEGER",
                 "Boolean": "BOOLEAN", "boolean": "BOOLEAN", "String": "VARCHAR", "LocalDate": "DATE",
                 "LocalDateTime": "TIMESTAMP", "LocalTime": "TIME", "BigDecimal": "NUMERIC"}
        kind = kinds.get(f.type)
        if not kind:
            self.error(f"Tipo Java no reconocido: {owner.name}.{f.name}={f.type}")
            return
        precision = literal(a, "precision", 0) if kind == "NUMERIC" else None
        scale = literal(a, "scale", 0) if kind == "NUMERIC" else None
        self.column(owner.table, column, kind, nullable,
                    literal(a, "length", 255) if kind == "VARCHAR" else None,
                    precision, scale, literal(a, "unique", False))

    def relation(self, owner, f, a):
        self.stats["relaciones"] = int(self.stats.get("relaciones", 0)) + 1
        if a.args.get("fetch") != ["FetchType", ".", "LAZY"]:
            self.error(f"Relacion sin LAZY explicito: {owner.name}.{f.name}")
        target = self.resolve(owner, f.type)
        if target is None or target.qualified not in self.entities:
            self.error(f"Destino JPA desconocido: {owner.name}.{f.name} -> {f.type}")
            return
        mapped_by = literal(a, "mappedBy")
        if mapped_by:
            self.stats["mapped_by"] = int(self.stats.get("mapped_by", 0)) + 1
            other = target.fields.get(mapped_by)
            if other is None:
                self.error(f"mappedBy inexistente: {owner.name}.{f.name} -> {target.name}.{mapped_by}")
                return
            back = self.resolve(target, other.type)
            if back is None or back.qualified != owner.qualified:
                self.error(f"mappedBy apunta a tipo distinto: {owner.name}.{f.name}")
            allowed = {"OneToMany": "ManyToOne", "OneToOne": "OneToOne", "ManyToMany": "ManyToMany"}
            expected = allowed.get(a.name)
            if not expected or expected not in other.annotations:
                self.error(f"mappedBy con anotacion incompatible: {owner.name}.{f.name}")
            elif "mappedBy" in other.annotations[expected].args:
                self.error(f"Ambos extremos inversos: {owner.name}.{f.name}")
            return
        if a.name == "ManyToMany":
            jt = f.annotations.get("JoinTable")
            if not jt or not literal(jt, "name"):
                self.error(f"ManyToMany sin JoinTable explicita: {owner.name}.{f.name}")
                return
            table = literal(jt, "name")
            self.mapped_tables.add(table)
            for arg, dest in (("joinColumns", owner), ("inverseJoinColumns", target)):
                joins = nested(jt, arg)
                if len(joins) != 1 or joins[0].name != "JoinColumn":
                    self.error(f"JoinTable compuesta/no explicita: {owner.name}.{f.name}.{arg}")
                    continue
                join = joins[0]
                column = literal(join, "name")
                referenced = literal(join, "referencedColumnName", "id")
                # Toda fila puente enlaza dos identidades; su PK compuesta implica NOT NULL.
                self.column(table, column, "BIGINT", False)
                self.require_fk(table, column, dest.table, referenced)
            return
        join = f.annotations.get("JoinColumn")
        if not join or not literal(join, "name"):
            self.error(f"Relacion sin JoinColumn explicita: {owner.name}.{f.name}")
            return
        column = literal(join, "name")
        nullable = literal(join, "nullable", literal(a, "optional", True))
        unique = literal(join, "unique", False) or a.name == "OneToOne"
        self.column(owner.table, column, "BIGINT", nullable, unique=unique)
        fks = nested(join, "foreignKey")
        name = literal(fks[0], "name") if fks else None
        self.require_fk(owner.table, column, target.table, literal(join, "referencedColumnName", "id"), name)

    def run(self):
        parser_self_check()
        migrations = self.root / "backend/src/main/resources/db/migration"
        files = sorted(migrations.glob("V*.sql"), key=lambda p: int(re.match(r"V(\d+)__", p.name).group(1)))
        if len(files) != 2 or not files[0].name.startswith("V1__") or not files[1].name.startswith("V2__"):
            self.error("Se requieren exactamente V1 y V2; migraciones inesperadas")
        v1 = migrations / "V1__piloto_libros_ejemplares.sql"
        actual_hash = hashlib.sha256(v1.read_bytes()).hexdigest().upper()
        self.stats["sha256_v1"] = actual_hash
        if actual_hash != V1_SHA256:
            self.error("V1 fue modificada: SHA-256 distinto del original")
        for path in files:
            self.schema.parse(path.read_text(encoding="utf-8-sig"), path.name)
        source_root = self.root / "backend/src/main/java"
        model = source_root / "mx/edu/itspa/biblioteca/modelo"
        for path in sorted(model.rglob("*.java")):
            typ = parse_java(path)
            if typ.qualified in self.types:
                self.error("Tipo Java duplicado: " + typ.qualified)
            self.types[typ.qualified] = typ
            code = tokens(path.read_text(encoding="utf-8-sig"), java=True)
            joined = " ".join(code)
            if re.search(r"@\s*Data\b|FetchType\s*\.\s*EAGER|CascadeType\s*\.\s*(?:REMOVE|ALL)\b", joined):
                self.error("Anotacion prohibida: " + str(path.relative_to(self.root)))
            if re.search(r"\b(?:TODO|FIXME|UnsupportedOperationException)\b", joined):
                self.error("Placeholder de codigo: " + str(path.relative_to(self.root)))
        for path in sorted(source_root.rglob("*.java")):
            code = tokens(path.read_text(encoding="utf-8-sig"), java=True)
            for i, t in enumerate(code):
                if t != "import":
                    continue
                end = code.index(";", i)
                imp = "".join(code[i + 1:end]).removeprefix("static")
                if imp.startswith(INTERNAL) and not imp.endswith(".*"):
                    target = source_root.joinpath(*imp.split(".")).with_suffix(".java")
                    if not target.exists():
                        self.error(f"Import interno inexistente/antiguo: {path.name}: {imp}")
        self.entities = {k: t for k, t in self.types.items() if "Entity" in t.annotations}
        enums = [t for t in self.types.values() if t.constants is not None]
        inherited = []
        for typ in self.entities.values():
            if not typ.table:
                self.error("Entidad sin @Table explicita: " + typ.name)
                continue
            if typ.table in self.mapped_tables:
                self.error("Tabla mapeada por mas de una entidad: " + typ.table)
            self.mapped_tables.add(typ.table)
            self.expected_columns.setdefault(typ.table, set())
            for required in ("Getter", "Setter", "NoArgsConstructor"):
                if required not in typ.annotations:
                    self.error(f"Lombok @{required} ausente: {typ.name}")
            id_fields = [f for f in typ.fields.values() if "Id" in f.annotations]
            if typ.parent:
                parent = self.resolve(typ, typ.parent)
                inheritance = parent.annotations.get("Inheritance") if parent else None
                pk = typ.annotations.get("PrimaryKeyJoinColumn")
                if not inheritance or inheritance.args.get("strategy") != ["InheritanceType", ".", "JOINED"]:
                    self.error("Herencia distinta de JOINED: " + typ.name)
                if not pk or literal(pk, "name") != "id":
                    self.error("PK heredada sin alias id: " + typ.name)
                if id_fields:
                    self.error("PK duplicada en subclase JOINED: " + typ.name)
                inherited.append(typ.name)
                self.column(typ.table, "id", "BIGINT", False)
                if parent:
                    self.require_fk(typ.table, "id", parent.table)
            elif len(id_fields) != 1 or id_fields[0].type != "Long":
                self.error("PK debe ser un Long @Id: " + typ.name)
            else:
                generated = id_fields[0].annotations.get("GeneratedValue")
                if not generated or generated.args.get("strategy") != ["GenerationType", ".", "IDENTITY"]:
                    self.error("PK sin IDENTITY: " + typ.name)
                sql_id = self.schema.tables.get(typ.table, SqlTable()).columns.get("id")
                if not sql_id or not sql_id.identity:
                    self.error("PK SQL sin BIGSERIAL: " + typ.name)
            for f in typ.fields.values():
                if f.static or "Transient" in f.annotations:
                    continue
                relations = [f.annotations[n] for n in ("ManyToOne", "OneToOne", "OneToMany", "ManyToMany") if n in f.annotations]
                if relations:
                    if len(relations) != 1:
                        self.error("Multiples anotaciones de relacion: " + typ.name + "." + f.name)
                    self.relation(typ, f, relations[0])
                elif "Column" in f.annotations:
                    self.scalar(typ, f, f.annotations["Column"])
                else:
                    self.error(f"Campo sin mapeo explicito: {typ.name}.{f.name}")
        for table, cols in self.expected_columns.items():
            sql = self.schema.tables.get(table)
            if sql and set(sql.columns) != cols:
                self.error(f"Columnas sin correspondencia en {table}: soloSQL={sorted(set(sql.columns)-cols)}, soloJava={sorted(cols-set(sql.columns))}")
        all_sql = set(self.schema.tables)
        if all_sql != self.mapped_tables:
            self.error(f"Tablas sin correspondencia: soloSQL={sorted(all_sql-self.mapped_tables)}, soloJava={sorted(self.mapped_tables-all_sql)}")
        for fk in self.schema.fks:
            source = self.schema.tables.get(fk.table)
            target = self.schema.tables.get(fk.target)
            if not source or not target or any(c not in source.columns for c in fk.columns) or any(c not in target.columns for c in fk.target_columns):
                self.error(f"FK SQL con tabla/columna inexistente: {fk}")
            elif fk.target_columns != target.primary and fk.target_columns not in target.unique:
                self.error(f"FK referencia columnas no unicas: {fk}")
            if fk.signature() not in self.covered_fk:
                self.error(f"FK SQL sin relacion JPA equivalente: {fk.table}.{','.join(fk.columns)} -> {fk.target}")
        for name, (table, columns) in self.schema.indexes.items():
            if any(c not in self.schema.tables[table].columns for c in columns):
                self.error("Indice de columna inexistente: " + name)
        self.stats.update(entidades=len(self.entities), enums=len(enums), tablas=len(all_sql),
                          puentes=len(self.mapped_tables)-len(self.entities), subclases_joined=len(inherited),
                          columnas=sum(len(t.columns) for t in self.schema.tables.values()),
                          fk_sql=len(self.schema.fks), indices=len(self.schema.indexes),
                          sentencias_sql=self.schema.statements)
        for label, expected in EXPECTED.items():
            if self.stats[label] != expected:
                self.error(f"Conteo {label}: esperado={expected}, encontrado={self.stats[label]}")

    def report(self):
        now = datetime.now(timezone.utc).isoformat(timespec="seconds")
        lines = ["VERIFICACION ESTATICA REPRODUCIBLE - MODELO ITSPA", f"Fecha UTC: {now}",
                 "Alcance: lectura de fuentes Java/JPA y DDL V1+V2 con Python stdlib.",
                 "No compila Java ni ejecuta Maven, Hibernate, Flyway, PostgreSQL o pruebas funcionales.",
                 "No instala dependencias ni utiliza red.",
                 "Nulos: @Column/@JoinColumn, obligatoriedad JPA, PK y claves de tablas puente.",
                 "CHECK: solo cotejo de vocabularios enum IN; no evaluacion general de expresiones SQL.",
                 ""]
        lines.extend(f"{k}: {v}" for k, v in sorted(self.stats.items()))
        lines += ["", f"Resultado estatico: {'SIN DISCREPANCIAS DETECTADAS' if not self.errors else 'CON DISCREPANCIAS'}",
                  f"Errores: {len(self.errors)}", f"Advertencias: {len(self.warnings)}"]
        lines.extend("ERROR: " + e for e in self.errors)
        lines.extend("AVISO: " + e for e in self.warnings)
        lines += ["", "Este resultado no sustituye compilacion Java ni validacion real del esquema por Hibernate.",
                  "Tampoco acredita migracion ejecutada, integridad operativa ni cumplimiento funcional de otros modulos."]
        return "\n".join(lines) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--output", type=Path, help="Guardar tambien el informe UTF-8 en esta ruta")
    args = parser.parse_args()
    audit = Audit(args.root.resolve())
    try:
        audit.run()
    except (OSError, ParseError, IndexError, KeyError, TypeError, ValueError) as exc:
        audit.error("Analisis interrumpido; no se certifica el resto: " + str(exc))
    report = audit.report()
    print(report, end="")
    if args.output:
        args.output.write_text(report, encoding="utf-8")
    return 1 if audit.errors else 0


if __name__ == "__main__":
    sys.exit(main())


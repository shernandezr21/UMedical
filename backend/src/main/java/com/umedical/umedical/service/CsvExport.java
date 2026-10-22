package com.umedical.umedical.service;

/** Resultado de una exportacion: el archivo y cuantas filas de datos trae (sin contar el encabezado). */
public record CsvExport(byte[] content, int rows) {
}

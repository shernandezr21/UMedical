package com.umedical.umedical.util;

import java.nio.charset.StandardCharsets;

/**
 * Escritor de CSV (RFC 4180) para exportar reportes.
 * - Empieza con BOM UTF-8 para que Excel muestre bien los acentos.
 * - Lineas terminadas en CRLF.
 * - Protege contra la inyeccion de formulas: una celda de texto que empieza con = + - @ se antepone con un
 *   apostrofe para que Excel / Sheets / LibreOffice no la ejecuten como formula.
 */
public final class CsvWriter {

    private static final String FORMULA_PREFIXES = "=+-@\t\r";

    private final StringBuilder out = new StringBuilder("\uFEFF");

    public CsvWriter row(Object... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(escape(cells[i]));
        }
        out.append("\r\n");
        return this;
    }

    public byte[] toBytes() {
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    static String escape(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();

        if (!text.isEmpty() && FORMULA_PREFIXES.indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }

        boolean needsQuotes = text.indexOf(',') >= 0 || text.indexOf('"') >= 0
                || text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0;
        if (needsQuotes) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}

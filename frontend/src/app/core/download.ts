/** Guarda un archivo (Blob) en el equipo del usuario, como una descarga normal del navegador. */
export function saveBlob(blob: Blob, filename: string): void {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}

/**
 * Cuando una descarga falla, el cuerpo del error tambien llega como Blob (porque se pidio un archivo).
 * Esto lo lee y devuelve el mensaje que mando la API, o uno por defecto.
 */
export async function blobErrorMessage(error: any, fallback: string): Promise<string> {
  try {
    if (error?.error instanceof Blob) {
      const body = JSON.parse(await error.error.text());
      return body?.message ?? fallback;
    }
  } catch {
    // no era JSON: se usa el mensaje por defecto
  }
  return error?.error?.message ?? fallback;
}

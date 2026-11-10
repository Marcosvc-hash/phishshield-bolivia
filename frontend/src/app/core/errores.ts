import { HttpErrorResponse } from '@angular/common/http';

/** Convierte un error HTTP del backend (ErrorResponse) en un texto para el usuario. */
export function mensajeDeError(err: unknown, porDefecto = 'Ocurrió un error inesperado.'): string {
  if (!(err instanceof HttpErrorResponse)) {
    return porDefecto;
  }
  if (err.status === 0) {
    return 'No se pudo conectar con el servidor. Verifique que el backend esté ejecutándose en el puerto 8080.';
  }
  if (err.error?.errores?.length) {
    return err.error.errores.map((e: { mensaje: string }) => e.mensaje).join('. ');
  }
  return err.error?.mensaje ?? porDefecto;
}

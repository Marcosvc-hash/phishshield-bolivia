import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { API_URL } from '../api';
import { AuthService } from './auth.service';

/** Estas rutas no llevan token: son justamente las que sirven para obtenerlo. */
const RUTAS_SIN_TOKEN = ['/auth/login', '/auth/registro', '/auth/refresh'];

/**
 * Se ejecuta en cada peticion HTTP de la aplicacion:
 *  1. Si va a nuestro backend, le agrega "Authorization: Bearer <token>".
 *  2. Si el backend responde 401 (el token vencio), pide uno nuevo con el
 *     token de refresco y repite la peticion UNA vez.
 *  3. Si el refresco tambien falla, borra la sesion y manda al login.
 */
export const authInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!peticion.url.startsWith(API_URL) || RUTAS_SIN_TOKEN.some((r) => peticion.url.includes(r))) {
    return siguiente(peticion);
  }

  const token = auth.tokenAcceso();

  return siguiente(conToken(peticion, token)).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401 || !token) {
        return throwError(() => error);
      }
      return auth.refrescar().pipe(
        catchError(() => {
          auth.limpiar();
          router.navigate(['/login'], { queryParams: { volver: router.url } });
          return throwError(() => error);
        }),
        switchMap((nuevo) => siguiente(conToken(peticion, nuevo.tokenAcceso))));
    }));
};

function conToken<T>(peticion: HttpRequest<T>, token: string | null): HttpRequest<T> {
  return token
    ? peticion.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : peticion;
}

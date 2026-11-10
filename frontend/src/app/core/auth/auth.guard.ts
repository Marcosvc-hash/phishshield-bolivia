import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Rol } from './auth.model';
import { AuthService } from './auth.service';

/** Deja pasar solo si hay sesion; si no, manda al login recordando a donde iba. */
export const authGuard: CanActivateFn = (_ruta, estado) => {
  const auth = inject(AuthService);
  return auth.autenticado()
    ? true
    : inject(Router).createUrlTree(['/login'], { queryParams: { volver: estado.url } });
};

/**
 * Deja pasar solo a ciertos roles. Es una comodidad de la interfaz: la
 * proteccion real esta en el backend, que responde 403 a los demas.
 */
export function rolGuard(...roles: Rol[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    return auth.tieneRol(...roles) ? true : inject(Router).createUrlTree(['/']);
  };
}

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
 * Lo contrario de authGuard: deja pasar solo a quien NO tiene sesion. Lo usa
 * la pagina publica; quien ya entro va al analizador dentro del panel.
 */
export const invitadoGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.autenticado() ? inject(Router).createUrlTree([AuthService.RUTA_ANALIZADOR]) : true;
};

/**
 * Deja pasar solo a ciertos roles. Es una comodidad de la interfaz: la
 * proteccion real esta en el backend, que responde 403 a los demas.
 * A quien no tiene el rol lo manda a su pagina de inicio dentro del panel.
 */
export function rolGuard(...roles: Rol[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    return auth.tieneRol(...roles) ? true : inject(Router).createUrlTree([auth.inicioSegunRol()]);
  };
}

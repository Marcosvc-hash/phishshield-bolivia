import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { authGuard, rolGuard } from './auth.guard';

describe('guards de autenticacion', () => {
  const ruta = {} as ActivatedRouteSnapshot;
  const estado = { url: '/admin/panel' } as RouterStateSnapshot;

  function sesionGuardada(rol: string): void {
    localStorage.setItem('phishshield.tokenAcceso', 'token');
    localStorage.setItem('phishshield.usuario',
      JSON.stringify({ id: 'u1', nombreCompleto: 'Prueba', email: 'p@x.bo', rol }));
  }

  function ejecutar(guard: typeof authGuard): boolean | UrlTree {
    return TestBed.runInInjectionContext(() => guard(ruta, estado)) as boolean | UrlTree;
  }

  beforeEach(() => {
    localStorage.clear();
  });

  function configurar(): void {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
  }

  it('authGuard sin sesion manda al login recordando la pagina', () => {
    configurar();
    const resultado = ejecutar(authGuard);
    expect(resultado.toString()).toBe('/login?volver=%2Fadmin%2Fpanel');
  });

  it('authGuard con sesion deja pasar', () => {
    sesionGuardada('USUARIO');
    configurar();
    expect(ejecutar(authGuard)).toBe(true);
  });

  it('rolGuard rechaza un rol que no esta en la lista', () => {
    sesionGuardada('USUARIO');
    configurar();
    const resultado = ejecutar(rolGuard('ADMIN', 'ADMIN_EMPRESA'));
    expect(resultado.toString()).toBe('/');
  });

  it('rolGuard acepta un rol de la lista', () => {
    sesionGuardada('ADMIN_EMPRESA');
    configurar();
    expect(ejecutar(rolGuard('ADMIN', 'ADMIN_EMPRESA'))).toBe(true);
  });
});

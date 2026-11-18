import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from './auth.service';

describe('AuthService.inicioSegunRol', () => {

  function servicioCon(rol: string | null): AuthService {
    localStorage.clear();
    if (rol) {
      localStorage.setItem('phishshield.tokenAcceso', 'token');
      localStorage.setItem('phishshield.usuario',
        JSON.stringify({ id: 'u1', nombreCompleto: 'Prueba', email: 'p@x.bo', rol }));
    }
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    return TestBed.inject(AuthService);
  }

  afterEach(() => localStorage.clear());

  it.each([
    ['ADMIN', '/admin/panel'],
    ['ADMIN_EMPRESA', '/admin/panel'],
    ['USUARIO', '/admin/analizar'],
  ])('%s llega a %s despues del login', (rol, destino) => {
    // Nunca "/admin" a secas: esa ruta redirige con esta misma funcion y seria un bucle
    expect(servicioCon(rol).inicioSegunRol()).toBe(destino);
  });
});

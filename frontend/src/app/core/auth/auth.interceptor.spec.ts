import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  const API = 'http://localhost:8080/api';
  let http: HttpClient;
  let backend: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        // El interceptor navega a /login cuando el refresco falla
        provideRouter([{ path: 'login', children: [] }]),
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    backend.verify();
    localStorage.clear();
  });

  it('agrega el token a las peticiones al backend', () => {
    localStorage.setItem('phishshield.tokenAcceso', 'token-viejo');

    http.get(`${API}/empresas`).subscribe();

    const peticion = backend.expectOne(`${API}/empresas`);
    expect(peticion.request.headers.get('Authorization')).toBe('Bearer token-viejo');
    peticion.flush([]);
  });

  it('no manda token en el login', () => {
    localStorage.setItem('phishshield.tokenAcceso', 'token-viejo');

    http.post(`${API}/auth/login`, {}).subscribe();

    const peticion = backend.expectOne(`${API}/auth/login`);
    expect(peticion.request.headers.has('Authorization')).toBe(false);
    peticion.flush({});
  });

  it('ante un 401 refresca el token y repite la peticion una vez', () => {
    localStorage.setItem('phishshield.tokenAcceso', 'token-viejo');
    localStorage.setItem('phishshield.tokenRefresco', 'refresco-1');
    let respuesta: unknown;

    http.get(`${API}/empresas`).subscribe((r) => (respuesta = r));

    backend.expectOne(`${API}/empresas`).flush(null, { status: 401, statusText: 'Unauthorized' });

    const refresco = backend.expectOne(`${API}/auth/refresh`);
    expect(refresco.request.body).toEqual({ tokenRefresco: 'refresco-1' });
    refresco.flush({
      tokenAcceso: 'token-nuevo', tipo: 'Bearer', accesoExpiraEn: '', tokenRefresco: 'refresco-2',
      refrescoExpiraEn: '', usuario: { id: 'u1', nombreCompleto: 'Admin', email: 'a@x.bo', rol: 'ADMIN' },
    });

    const repetida = backend.expectOne(`${API}/empresas`);
    expect(repetida.request.headers.get('Authorization')).toBe('Bearer token-nuevo');
    repetida.flush(['ok']);

    expect(respuesta).toEqual(['ok']);
    expect(localStorage.getItem('phishshield.tokenRefresco')).toBe('refresco-2');
  });

  it('si el refresco falla, borra la sesion', () => {
    localStorage.setItem('phishshield.tokenAcceso', 'token-viejo');
    localStorage.setItem('phishshield.tokenRefresco', 'refresco-revocado');
    let fallo = false;

    http.get(`${API}/empresas`).subscribe({ error: () => (fallo = true) });

    backend.expectOne(`${API}/empresas`).flush(null, { status: 401, statusText: 'Unauthorized' });
    backend.expectOne(`${API}/auth/refresh`).flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(fallo).toBe(true);
    expect(localStorage.getItem('phishshield.tokenAcceso')).toBeNull();
  });
});

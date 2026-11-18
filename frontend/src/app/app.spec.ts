import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { App } from './app';
import { routes } from './app.routes';

describe('App', () => {
  const PENDIENTES = 'http://localhost:8080/api/admin/alertas/pendientes';

  /** Simula una sesion guardada en el navegador (lo que hace AuthService al hacer login). */
  function sesionGuardada(rol: string): void {
    localStorage.setItem('phishshield.tokenAcceso', 'token-de-prueba');
    localStorage.setItem('phishshield.usuario',
      JSON.stringify({ id: 'u1', nombreCompleto: 'Prueba Uno', email: 'p@x.bo', rol }));
  }

  /** Configura el TestBed con las rutas reales. Se llama DESPUES de guardar la sesion. */
  async function configurar(): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [App],
      // HttpClient de prueba: las peticiones no salen a la red
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter(routes)],
    })
      .compileComponents();
  }

  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create the app', async () => {
    await configurar();
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('sin sesion, la ruta raiz muestra la marca, el analizador y el boton de iniciar sesion', async () => {
    await configurar();
    const harness = await RouterTestingHarness.create('/');
    const pagina = harness.routeNativeElement as HTMLElement;
    expect(pagina.querySelector('header')?.textContent).toContain('PhishShield');
    expect(pagina.querySelector('h1')?.textContent).toContain('Verifique un enlace');
    expect(pagina.querySelector('app-analizador')).not.toBeNull();
    expect(pagina.textContent).toContain('Iniciar sesión');
  });

  it('sin sesion, /admin redirige al login', async () => {
    await configurar();
    const harness = await RouterTestingHarness.create('/admin');
    const pagina = harness.routeNativeElement as HTMLElement;
    expect(pagina.querySelector('h1')?.textContent).toContain('Iniciar sesión');
  });

  it('sin sesion, /admin/analizar tambien pide iniciar sesion', async () => {
    await configurar();
    await RouterTestingHarness.create('/admin/analizar');
    expect(TestBed.inject(Router).url).toBe('/login?volver=%2Fadmin%2Fanalizar');
  });

  it('con sesion, la ruta raiz redirige al analizador dentro del panel', async () => {
    sesionGuardada('USUARIO');
    await configurar();
    const harness = await RouterTestingHarness.create('/');
    const pagina = harness.routeNativeElement as HTMLElement;

    expect(TestBed.inject(Router).url).toBe('/admin/analizar');
    // El mismo componente del analizador, pero dentro del marco del panel (menu lateral)
    expect(pagina.querySelector('app-admin-layout aside')).not.toBeNull();
    expect(pagina.querySelector('app-admin-layout app-analizador')).not.toBeNull();
    expect(pagina.querySelector('h1')?.textContent).toContain('Analizar URL');
    // Ya no se ve la barra superior publica
    expect(pagina.textContent).not.toContain('Verifique un enlace');
  });

  it('con sesion de USUARIO, /admin lleva al analizador (no al panel, que no puede ver)', async () => {
    sesionGuardada('USUARIO');
    await configurar();
    await RouterTestingHarness.create('/admin');
    expect(TestBed.inject(Router).url).toBe('/admin/analizar');
  });

  it('con sesion de ADMIN, /admin sigue llevando al panel', async () => {
    sesionGuardada('ADMIN');
    await configurar();
    await RouterTestingHarness.create('/admin');
    expect(TestBed.inject(Router).url).toBe('/admin/panel');
    // El layout pide las alertas pendientes y el panel sus datos: no interesan aqui
    TestBed.inject(HttpTestingController).match(() => true);
  });

  it('con sesion de ADMIN_EMPRESA, la ruta raiz tambien lleva al analizador del panel', async () => {
    sesionGuardada('ADMIN_EMPRESA');
    await configurar();
    const harness = await RouterTestingHarness.create('/');
    expect(TestBed.inject(Router).url).toBe('/admin/analizar');
    expect((harness.routeNativeElement as HTMLElement).querySelector('app-analizador')).not.toBeNull();
    TestBed.inject(HttpTestingController).expectOne(PENDIENTES).flush({ cantidad: 0 });
  });
});

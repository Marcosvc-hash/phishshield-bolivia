import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { App } from './app';
import { routes } from './app.routes';

describe('App', () => {
  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [App],
      // HttpClient de prueba: las peticiones no salen a la red
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter(routes)],
    })
      .compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('la ruta raiz muestra la marca, el analizador y el boton de iniciar sesion', async () => {
    const harness = await RouterTestingHarness.create('/');
    const pagina = harness.routeNativeElement as HTMLElement;
    expect(pagina.querySelector('header')?.textContent).toContain('PhishShield');
    expect(pagina.querySelector('h1')?.textContent).toContain('Verifique un enlace');
    expect(pagina.querySelector('app-analizador')).not.toBeNull();
    expect(pagina.textContent).toContain('Iniciar sesión');
  });

  it('sin sesion, /admin redirige al login', async () => {
    const harness = await RouterTestingHarness.create('/admin');
    const pagina = harness.routeNativeElement as HTMLElement;
    expect(pagina.querySelector('h1')?.textContent).toContain('Iniciar sesión');
  });
});

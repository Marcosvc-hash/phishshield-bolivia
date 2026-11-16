import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AdminLayout } from './admin-layout';

describe('AdminLayout', () => {
  const PENDIENTES = 'http://localhost:8080/api/admin/alertas/pendientes';

  let fixture: ComponentFixture<AdminLayout>;
  let http: HttpTestingController;

  async function montarComo(rol: string) {
    localStorage.clear();
    localStorage.setItem('phishshield.tokenAcceso', 't');
    localStorage.setItem('phishshield.usuario',
      JSON.stringify({ id: 'yo', nombreCompleto: 'Yo', email: 'yo@x.bo', rol }));

    await TestBed.configureTestingModule({
      imports: [AdminLayout],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(AdminLayout);
    fixture.detectChanges();
  }

  const opcion = (texto: string) =>
    ([...fixture.nativeElement.querySelectorAll('nav a')] as HTMLElement[]).find((a) => a.textContent?.includes(texto));

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('muestra el número de alertas pendientes junto a "Alertas"', async () => {
    await montarComo('ADMIN_EMPRESA');
    http.expectOne(PENDIENTES).flush({ cantidad: 4 });
    await fixture.whenStable();

    expect(opcion('Alertas')?.querySelector('.insignia')?.textContent?.trim()).toBe('4');
  });

  it('sin pendientes no muestra el número', async () => {
    await montarComo('ADMIN');
    http.expectOne(PENDIENTES).flush({ cantidad: 0 });
    await fixture.whenStable();

    expect(opcion('Alertas')?.querySelector('.insignia')).toBeNull();
  });

  it('el admin de empresa no ve las opciones de plataforma', async () => {
    await montarComo('ADMIN_EMPRESA');
    http.expectOne(PENDIENTES).flush({ cantidad: 0 });
    await fixture.whenStable();

    expect(opcion('Registro de accesos')).toBeUndefined();
    expect(opcion('Empresas')).toBeUndefined();
    expect(opcion('Historial de análisis')).toBeDefined();
    // El grupo "Plataforma" queda vacío y no se muestra
    const grupos = [...fixture.nativeElement.querySelectorAll('.grupo')].map((g: HTMLElement) => g.textContent?.trim());
    expect(grupos).toEqual(['Monitoreo', 'Gestión']);
  });

  it.each(['ADMIN', 'ADMIN_EMPRESA'])('para %s, "Analizar URL" es la primera opción de Monitoreo', async (rol) => {
    await montarComo(rol);
    http.expectOne(PENDIENTES).flush({ cantidad: 0 });
    await fixture.whenStable();

    const primera = fixture.nativeElement.querySelector('nav a') as HTMLAnchorElement;
    expect(primera.textContent?.trim()).toBe('Analizar URL');
    expect(primera.getAttribute('href')).toBe('/admin/analizar');
    // Con el mismo icono de escudo que usa el boton "Analizar" del analizador
    expect(primera.querySelector('app-icono')).not.toBeNull();
  });

  it('el USUARIO solo ve "Analizar URL" y no pide las alertas pendientes', async () => {
    await montarComo('USUARIO');
    // http.verify() del afterEach falla si se hubiera pedido /pendientes
    await fixture.whenStable();

    const opciones = ([...fixture.nativeElement.querySelectorAll('nav a')] as HTMLElement[]).map((a) => a.textContent?.trim());
    expect(opciones).toEqual(['Analizar URL']);
    const grupos = [...fixture.nativeElement.querySelectorAll('.grupo')].map((g: HTMLElement) => g.textContent?.trim());
    expect(grupos).toEqual(['Monitoreo']);
  });

  it('ya no hay un enlace aparte "Ir al analizador" (quedó en el menú)', async () => {
    await montarComo('ADMIN');
    http.expectOne(PENDIENTES).flush({ cantidad: 0 });
    await fixture.whenStable();

    expect(fixture.nativeElement.textContent).not.toContain('Ir al analizador');
  });

  it('el menú del usuario muestra nombre y rol y se cierra con Escape', async () => {
    await montarComo('ADMIN');
    http.expectOne(PENDIENTES).flush({ cantidad: 0 });
    await fixture.whenStable();

    const boton = fixture.nativeElement.querySelector('.boton-usuario') as HTMLButtonElement;
    boton.click();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(boton.getAttribute('aria-expanded')).toBe('true');
    const menu = fixture.nativeElement.querySelector('#menu-usuario') as HTMLElement;
    expect(menu.textContent).toContain('Administrador');
    expect(menu.textContent).toContain('Cerrar sesión');

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('#menu-usuario')).toBeNull();
  });

  it('el botón de hamburguesa abre el menú lateral (celular)', async () => {
    await montarComo('ADMIN');
    http.expectOne(PENDIENTES).flush({ cantidad: 0 });
    await fixture.whenStable();

    const abrir = fixture.nativeElement.querySelector('button[aria-label="Abrir menú"]') as HTMLButtonElement;
    abrir.click();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(abrir.getAttribute('aria-expanded')).toBe('true');
    expect(fixture.nativeElement.querySelector('.marco')?.classList).toContain('menu-abierto');
  });
});

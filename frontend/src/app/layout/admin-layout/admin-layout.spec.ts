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

  it('muestra el numero de alertas pendientes junto a "Alertas"', async () => {
    await montarComo('ADMIN_EMPRESA');
    http.expectOne(PENDIENTES).flush({ cantidad: 4 });
    await fixture.whenStable();

    expect(opcion('Alertas')?.querySelector('.insignia')?.textContent?.trim()).toBe('4');
  });

  it('sin pendientes no muestra el numero', async () => {
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
    expect(opcion('Historial de analisis')).toBeDefined();
  });
});

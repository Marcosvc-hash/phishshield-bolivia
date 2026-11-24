import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { Reportes, fechaIso } from './reportes';
import { ReporteEmpresa } from './reportes.service';

describe('Reportes por empresa', () => {
  const URL = 'http://localhost:8080/api/admin/reportes';
  const EMPRESAS = 'http://localhost:8080/api/empresas';

  let fixture: ComponentFixture<Reportes>;
  let http: HttpTestingController;

  const reporte = (empresa = 'Importadora Andina'): ReporteEmpresa => ({
    id: 'r1', idEmpresa: 'e1', empresa, desde: '2026-11-01', hasta: '2026-11-24', dias: 24,
    totalAnalisis: 40, totalPhishing: 6, totalAlertas: 9, porcentajePhishing: 15,
    generadoPor: 'admin@phishshield.bo', fechaGeneracion: '2026-11-24T10:30:00-04:00',
  });
  const pagina = (contenido: ReporteEmpresa[]) => ({ contenido, pagina: 0, tamano: 20, totalElementos: contenido.length, totalPaginas: 1 });

  async function montarComo(rol: string) {
    localStorage.clear();
    localStorage.setItem('phishshield.tokenAcceso', 't');
    localStorage.setItem('phishshield.usuario', JSON.stringify({ id: 'yo', nombreCompleto: 'Yo', email: 'yo@x.bo', rol }));
    await TestBed.configureTestingModule({
      imports: [Reportes],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Reportes);
    fixture.detectChanges();
  }

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('el ADMIN elige la empresa y genera el reporte de los últimos 30 días por defecto', async () => {
    await montarComo('ADMIN');
    http.expectOne(EMPRESAS).flush([{ id: 'e1', nombre: 'Importadora Andina', activa: true }]);
    http.expectOne((r) => r.url === URL && r.method === 'GET').flush(pagina([]));
    await fixture.whenStable();
    const c = fixture.componentInstance;
    expect(c.hasta).toBe(fechaIso(new Date()));

    c.generar();
    expect(http.match((r) => r.method === 'POST').length).toBe(0); // sin empresa no manda nada

    c.idEmpresa = 'e1';
    c.generar();
    const post = http.expectOne((r) => r.url === URL && r.method === 'POST');
    expect(post.request.body).toEqual({ desde: c.desde, hasta: c.hasta, idEmpresa: 'e1' });
    post.flush(reporte());
    http.expectOne((r) => r.url === URL && r.method === 'GET').flush(pagina([reporte()]));
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.ultimo')?.textContent).toContain('40 análisis');
    expect(fixture.nativeElement.querySelectorAll('tbody tr').length).toBe(1);
  });

  it('el administrador de empresa no elige empresa ni pide la lista de empresas', async () => {
    await montarComo('ADMIN_EMPRESA');
    http.expectOne((r) => r.url === URL && r.method === 'GET').flush(pagina([reporte()]));
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('#reporte-empresa')).toBeNull();
    fixture.componentInstance.generar();
    const post = http.expectOne((r) => r.url === URL && r.method === 'POST');
    expect(post.request.body.idEmpresa).toBeUndefined();
    post.flush(reporte());
    http.expectOne((r) => r.url === URL && r.method === 'GET').flush(pagina([reporte()]));
  });

  it('descarga el PDF como archivo con el token', async () => {
    await montarComo('ADMIN_EMPRESA');
    http.expectOne((r) => r.url === URL && r.method === 'GET').flush(pagina([reporte()]));
    await fixture.whenStable();
    const guardar = vi.spyOn(Reportes, 'guardarArchivo').mockImplementation(() => undefined);

    (fixture.nativeElement.querySelector('button.descargar') as HTMLButtonElement).click();
    const peticion = http.expectOne(`${URL}/r1/pdf`);
    expect(peticion.request.responseType).toBe('blob');
    peticion.flush(new Blob(['%PDF']));

    expect(guardar).toHaveBeenCalledWith(expect.any(Blob), 'reporte-importadora-andina-2026-11-01-al-2026-11-24.pdf');
  });

  it('muestra el mensaje del backend si el período no es válido', async () => {
    await montarComo('ADMIN_EMPRESA');
    http.expectOne((r) => r.url === URL && r.method === 'GET').flush(pagina([]));
    await fixture.whenStable();

    fixture.componentInstance.generar();
    http.expectOne((r) => r.url === URL && r.method === 'POST')
      .flush({ estado: 400, mensaje: 'El período no puede pasar de 366 días' }, { status: 400, statusText: 'Bad Request' });
    expect(fixture.componentInstance.generando()).toBe(false);
    expect(fixture.componentInstance.ultimo()).toBeNull();
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Alertas } from './alertas';
import { Alerta, AlertasService } from './alertas.service';

describe('Alertas', () => {
  const URL = 'http://localhost:8080/api/admin/alertas';

  let fixture: ComponentFixture<Alertas>;
  let http: HttpTestingController;

  const alerta = (estado: Alerta['estado'], notas: string | null = null): Alerta => ({
    id: 'al1', idAnalisis: 'an1', severidad: 'alta', estado, url: 'http://bnb-bo-seguro.com/login',
    dominio: 'bnb-bo-seguro.com', nivelRiesgo: 85, empresa: 'Andina', usuario: null,
    asignado: estado === 'pendiente' ? null : 'maria@andina.bo', notas,
    fechaNotificacion: '2026-11-09T10:00:00-04:00', fechaCierre: null,
  });

  const pagina = (contenido: Alerta[]) => ({ contenido, pagina: 0, tamano: 20, totalElementos: contenido.length, totalPaginas: 1 });

  async function abrirPrimera(a: Alerta) {
    http.expectOne((r) => r.url === URL).flush(pagina([a]));
    await fixture.whenStable();
    (fixture.nativeElement.querySelector('tbody tr') as HTMLElement).click();
    fixture.detectChanges();
    await fixture.whenStable();
  }

  const botones = () => [...fixture.nativeElement.querySelectorAll('.botones button')] as HTMLButtonElement[];

  beforeEach(async () => {
    localStorage.clear();
    localStorage.setItem('phishshield.tokenAcceso', 't');
    localStorage.setItem('phishshield.usuario',
      JSON.stringify({ id: 'yo', nombreCompleto: 'Maria', email: 'maria@andina.bo', rol: 'ADMIN_EMPRESA' }));

    await TestBed.configureTestingModule({
      imports: [Alertas],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Alertas);
    fixture.detectChanges();
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('por defecto pide las pendientes', () => {
    const peticion = http.expectOne((r) => r.url === URL);
    expect(peticion.request.params.get('estado')).toBe('pendiente');
    expect(peticion.request.params.has('severidad')).toBe(false);
    peticion.flush(pagina([]));
  });

  it('una pendiente solo ofrece "Tomar en revision" y manda el PATCH con las notas', async () => {
    await abrirPrimera(alerta('pendiente'));
    expect(botones().map((b) => b.textContent?.trim())).toEqual(['Tomar en revision', 'Guardar notas']);

    fixture.componentInstance.notas = 'La reviso yo';
    botones()[0].click();

    const patch = http.expectOne(`${URL}/al1`);
    expect(patch.request.method).toBe('PATCH');
    expect(patch.request.body).toEqual({ estado: 'en_revision', notas: 'La reviso yo' });
    patch.flush(alerta('en_revision', 'La reviso yo'));
    // Despues del cambio se refresca el contador del menu
    http.expectOne(`${URL}/pendientes`).flush({ cantidad: 0 });
    await fixture.whenStable();

    expect(botones().map((b) => b.textContent?.trim()))
      .toEqual(['Marcar revisada', 'Descartar (falso positivo)', 'Guardar notas']);
    expect(TestBed.inject(AlertasService).pendientes()).toBe(0);
  });

  it('una alerta cerrada se muestra sin botones', async () => {
    await abrirPrimera(alerta('descartada', 'Falso positivo'));
    expect(botones().length).toBe(0);
    expect(fixture.nativeElement.querySelector('.notas-cerradas')?.textContent).toContain('Falso positivo');
  });

  it('si el backend rechaza la transicion muestra su mensaje', async () => {
    await abrirPrimera(alerta('en_revision'));
    botones()[0].click();

    http.expectOne(`${URL}/al1`).flush(
      { mensaje: 'Otra persona cambio esta alerta mientras la editaba.' },
      { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('.aviso.error')?.textContent).toContain('Otra persona');
  });
});

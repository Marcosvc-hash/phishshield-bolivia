import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Historial } from './historial';
import { AnalisisFila, DetalleAnalisis } from './historial.service';
import { Pagina } from '../../../core/pagina';

describe('Historial', () => {
  const URL = 'http://localhost:8080/api/admin/analisis';

  let fixture: ComponentFixture<Historial>;
  let http: HttpTestingController;

  const fila: AnalisisFila = {
    id: 'a1', url: 'http://pagos-ende.xyz/login', dominio: 'pagos-ende.xyz', resultado: 'phishing',
    nivelRiesgo: 82.4, fecha: '2026-11-09T10:00:00-04:00', usuario: null, empresa: null, estadoAlerta: 'pendiente',
  };

  const pagina = (contenido: AnalisisFila[], numero = 0, total = contenido.length): Pagina<AnalisisFila> => ({
    contenido, pagina: numero, tamano: 20, totalElementos: total, totalPaginas: Math.ceil(total / 20),
  });

  const detalle: DetalleAnalisis = {
    ...fila, origen: 'web', tiempoMs: 35, modeloIa: 'phishshield-url-ia 1.0.0', severidadAlerta: 'alta',
    indicadores: [
      { codigo: 'URL_TYPOSQUATTING', descripcion: 'Se parece a una entidad', valor: 'ende.bo', aporte: 45 },
      { codigo: 'IA_MODELO_URL', descripcion: 'Modelo de IA', valor: 'probabilidad 0.93', aporte: 37.4 },
    ],
  };

  beforeEach(async () => {
    localStorage.clear();
    localStorage.setItem('phishshield.tokenAcceso', 't');
    localStorage.setItem('phishshield.usuario',
      JSON.stringify({ id: 'yo', nombreCompleto: 'Admin', email: 'admin@x.bo', rol: 'ADMIN' }));

    await TestBed.configureTestingModule({
      imports: [Historial],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Historial);
    fixture.detectChanges();
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('pide la primera pagina sin filtros vacios', async () => {
    const peticion = http.expectOne((r) => r.url === URL);
    expect(peticion.request.params.keys().sort()).toEqual(['pagina', 'tamano']);
    peticion.flush(pagina([fila]));
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelectorAll('tbody tr').length).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('anonimo');
  });

  it('manda los filtros elegidos y vuelve a la pagina 0', async () => {
    http.expectOne((r) => r.url === URL).flush(pagina([fila], 1, 45));
    await fixture.whenStable();

    fixture.componentInstance.filtro = { resultado: 'phishing', desde: '2026-11-01', hasta: '', q: 'ende' };
    fixture.componentInstance.buscar();

    const peticion = http.expectOne((r) => r.url === URL);
    expect(peticion.request.params.get('resultado')).toBe('phishing');
    expect(peticion.request.params.get('desde')).toBe('2026-11-01');
    expect(peticion.request.params.get('q')).toBe('ende');
    expect(peticion.request.params.has('hasta')).toBe(false);
    expect(peticion.request.params.get('pagina')).toBe('0');
    peticion.flush(pagina([]));
  });

  it('al hacer clic en una fila muestra los indicadores con su aporte, incluida la IA', async () => {
    http.expectOne((r) => r.url === URL).flush(pagina([fila]));
    await fixture.whenStable();

    (fixture.nativeElement.querySelector('tbody tr') as HTMLElement).click();
    http.expectOne(`${URL}/a1`).flush(detalle);
    await fixture.whenStable();

    const filasIndicadores = [...fixture.nativeElement.querySelectorAll('.indicadores tbody tr')] as HTMLElement[];
    expect(filasIndicadores.length).toBe(2);
    expect(filasIndicadores[1].textContent).toContain('IA_MODELO_URL');
    expect(filasIndicadores[1].textContent).toContain('+37.40');
    expect(fixture.nativeElement.querySelector('tfoot')?.textContent).toContain('82.40');
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Analizador } from './analizador';

describe('Analizador', () => {
  const API = 'http://localhost:8080/api/analisis';

  let component: Analizador;
  let fixture: ComponentFixture<Analizador>;
  let http: HttpTestingController;

  /** Simula una sesion guardada en el navegador (lo que hace AuthService al hacer login). */
  function sesionGuardada(rol: string): void {
    localStorage.setItem('phishshield.tokenAcceso', 'token-de-prueba');
    localStorage.setItem('phishshield.usuario',
      JSON.stringify({ id: 'u1', nombreCompleto: 'Prueba', email: 'p@x.bo', rol }));
  }

  function crear(): void {
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Analizador);
    component = fixture.componentInstance;
  }

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [Analizador],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
  });

  afterEach(() => {
    // Falla si quedo alguna peticion sin esperar
    http.verify();
    localStorage.clear();
  });

  it('should create', () => {
    crear();
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('sin sesion no pide el listado general (es solo para ADMIN)', () => {
    crear();
    fixture.detectChanges();

    http.expectNone(`${API}?limite=10`);
    expect(component.historial().length).toBe(0);
  });

  it('con sesion de ADMIN carga el historial al iniciar', () => {
    sesionGuardada('ADMIN');
    crear();
    fixture.detectChanges(); // dispara ngOnInit

    const peticion = http.expectOne(`${API}?limite=10`);
    expect(peticion.request.method).toBe('GET');

    peticion.flush([{
      id: 'a1b2', url: 'https://www.bnb.com.bo/', dominio: 'bnb.com.bo',
      tipo: 'url', origen: 'web', resultado: 'seguro', nivelRiesgo: 0,
      requiereAlerta: false, tiempoMs: 3, fechaAnalisis: '2026-10-11T12:00:00-04:00',
      indicadores: [],
    }]);

    expect(component.historial().length).toBe(1);
  });

  it('muestra el veredicto, el medidor y cada indicador explicado, sin códigos sueltos', async () => {
    crear();
    fixture.detectChanges();

    component.url = 'http://bnb-bo-seguro.com/login';
    component.analizar();
    http.expectOne({ method: 'POST', url: API }).flush({
      id: 'x1', url: 'http://bnb-bo-seguro.com/login', dominio: 'bnb-bo-seguro.com',
      tipo: 'url', origen: 'web', resultado: 'phishing', nivelRiesgo: 100,
      requiereAlerta: true, tiempoMs: 12, fechaAnalisis: '2026-11-10T12:00:00-04:00',
      indicadores: [
        { codigo: 'IA_MODELO_URL', detalle: 'probabilidad 0.91', aporte: 36.4 },
        { codigo: 'URL_LISTA_NEGRA', detalle: 'bnb-bo-seguro.com', aporte: 70 },
        { codigo: 'URL_REGLA_FUTURA', detalle: null, aporte: 5 },
      ],
    });
    await fixture.whenStable();

    const pagina = fixture.nativeElement as HTMLElement;
    expect(pagina.querySelector('.etiqueta')?.textContent).toContain('Phishing detectado');
    expect(pagina.querySelector('[role="meter"]')?.getAttribute('aria-valuenow')).toBe('100');

    const titulos = [...pagina.querySelectorAll('.titulo-indicador')].map((e) => e.textContent?.trim());
    // Ordenados por aporte: lista negra (70), IA (36.4), regla sin texto (5)
    expect(titulos).toEqual(['Dominio reportado como fraude', 'Detectado por inteligencia artificial', 'Otra señal de riesgo']);
    expect(pagina.querySelector('.explicacion')?.textContent).not.toMatch(/URL_|IA_MODELO/);
  });

  it('no llama a la API si la URL no tiene http o https', () => {
    crear();
    fixture.detectChanges();

    component.url = 'ftp://archivos.com';
    component.analizar();

    http.expectNone({ method: 'POST', url: API });
    expect(component.error()).toBe(
      'La URL debe empezar con http:// o https://, por ejemplo https://ejemplo.com');
    expect(component.analizando()).toBe(false);
  });
});

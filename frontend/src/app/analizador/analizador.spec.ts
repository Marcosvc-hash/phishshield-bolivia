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

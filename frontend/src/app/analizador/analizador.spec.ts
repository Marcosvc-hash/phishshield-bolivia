import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Analizador } from './analizador';

describe('Analizador', () => {
  const API = 'http://localhost:8080/api/analisis';

  let component: Analizador;
  let fixture: ComponentFixture<Analizador>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Analizador],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Analizador);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    // Falla si quedo alguna peticion sin esperar
    http.verify();
  });

  it('should create', () => {
    fixture.detectChanges();
    http.expectOne(`${API}?limite=10`).flush([]);
    expect(component).toBeTruthy();
  });

  it('carga el historial al iniciar', () => {
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
    fixture.detectChanges();
    http.expectOne(`${API}?limite=10`).flush([]);

    component.url = 'ftp://archivos.com';
    component.analizar();

    http.expectNone({ method: 'POST', url: API });
    expect(component.error()).toBe(
      'La URL debe empezar con http:// o https://, por ejemplo https://ejemplo.com');
    expect(component.analizando()).toBe(false);
  });
});

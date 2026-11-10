import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Panel } from './panel';
import { PanelDatos } from './panel.model';

describe('Panel', () => {
  const URL = 'http://localhost:8080/api/admin/panel?dias=30';

  let fixture: ComponentFixture<Panel>;
  let http: HttpTestingController;

  const datos: PanelDatos = {
    resumen: { totalAnalisis: 8, totalPhishing: 2, porcentajePhishing: 25, alertasAbiertas: 3, usuariosActivos: 4 },
    porDia: [{ dia: '2026-11-09', total: 8, phishing: 2, sospechosos: 1, seguros: 5 }],
    topEntidades: [
      { entidad: 'Banco Nacional de Bolivia', tipo: 'banco', veces: 3 },
      { entidad: 'Tigo Money', tipo: 'billetera', veces: 1 },
    ],
    ultimosAnalisis: [{
      id: 'a1', contenido: 'http://bnb-bo-seguro.com/login', dominio: 'bnb-bo-seguro.com',
      resultado: 'phishing', nivelRiesgo: 94.5, fecha: '2026-11-09T15:00:00-04:00',
      usuario: null, empresa: null,
    }],
    porEmpresa: [],
  };

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [Panel],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Panel);
  });

  afterEach(() => http.verify());

  it('muestra las cuatro tarjetas con los totales del backend', async () => {
    fixture.detectChanges();
    http.expectOne(URL).flush(datos);
    await fixture.whenStable();

    const cifras = [...fixture.nativeElement.querySelectorAll('.cifra')]
      .map((e: HTMLElement) => e.textContent?.trim());
    expect(cifras).toEqual(['8', '25.0%', '3', '4']);
  });

  it('lista el top de entidades y marca los analisis anonimos', async () => {
    fixture.detectChanges();
    http.expectOne(URL).flush(datos);
    await fixture.whenStable();

    const pagina = fixture.nativeElement as HTMLElement;
    expect(pagina.querySelectorAll('.top li').length).toBe(2);
    expect(pagina.querySelector('.usuario')?.textContent).toContain('anonimo');
  });

  it('muestra el mensaje del backend si responde con error', async () => {
    fixture.detectChanges();
    http.expectOne(URL).flush(
      { mensaje: 'Los datos administrativos son solo para administradores' },
      { status: 403, statusText: 'Forbidden' });
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('.aviso.error')?.textContent)
      .toContain('solo para administradores');
  });
});

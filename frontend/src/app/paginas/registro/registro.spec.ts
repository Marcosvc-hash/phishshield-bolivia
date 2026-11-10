import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Registro } from './registro';

describe('Registro', () => {
  let fixture: ComponentFixture<Registro>;
  let http: HttpTestingController;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [Registro],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Registro);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  const pagina = () => fixture.nativeElement as HTMLElement;

  async function escribir(selector: string, valor: string) {
    const input = pagina().querySelector(selector) as HTMLInputElement;
    input.value = valor;
    input.dispatchEvent(new Event('input'));
    input.dispatchEvent(new Event('blur'));
    fixture.detectChanges();
    await fixture.whenStable();
  }

  it('marca en vivo el requisito de largo y la confirmación distinta', async () => {
    await escribir('#contrasena', 'corta');
    expect(pagina().querySelector('.requisitos li')?.classList).not.toContain('cumple');

    await escribir('#contrasena', 'ClaveSegura1');
    expect(pagina().querySelector('.requisitos li')?.classList).toContain('cumple');

    await escribir('#confirmacion', 'ClaveSegura2');
    expect(pagina().querySelector('#confirmacion-error')?.textContent).toContain('no coinciden');
  });

  it('con datos inválidos no llama al backend', () => {
    fixture.componentInstance.nombres = 'Ana';
    fixture.componentInstance.email = 'ana@x.bo';
    fixture.componentInstance.contrasena = 'ClaveSegura1';
    fixture.componentInstance.confirmacion = 'Otra';
    fixture.componentInstance.registrar();

    http.expectNone('http://localhost:8080/api/auth/registro');
    expect(fixture.componentInstance.error()).toBe('Las contraseñas no coinciden.');
  });
});

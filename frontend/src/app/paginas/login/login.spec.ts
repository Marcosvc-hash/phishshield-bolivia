import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Login } from './login';

describe('Login', () => {
  let fixture: ComponentFixture<Login>;
  let http: HttpTestingController;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Login);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  const pagina = () => fixture.nativeElement as HTMLElement;
  const email = () => pagina().querySelector('#email') as HTMLInputElement;

  async function escribir(input: HTMLInputElement, valor: string) {
    input.value = valor;
    input.dispatchEvent(new Event('input'));
    input.dispatchEvent(new Event('blur'));
    fixture.detectChanges();
    await fixture.whenStable();
  }

  it('no muestra errores al abrir y valida el email al salir del campo', async () => {
    expect(pagina().querySelector('.campo-error')).toBeNull();

    await escribir(email(), 'maria@');
    expect(pagina().querySelector('#email-error')?.textContent).toContain('nombre@empresa.bo');
    expect(email().getAttribute('aria-invalid')).toBe('true');

    await escribir(email(), 'maria@andina.bo');
    expect(pagina().querySelector('#email-error')).toBeNull();
  });

  it('el botón del ojo muestra y oculta la contraseña', async () => {
    const contrasena = pagina().querySelector('#contrasena') as HTMLInputElement;
    const ojo = pagina().querySelector('button[aria-label="Mostrar contraseña"]') as HTMLButtonElement;
    expect(contrasena.type).toBe('password');

    ojo.click();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(contrasena.type).toBe('text');
    expect(ojo.getAttribute('aria-label')).toBe('Ocultar contraseña');
    expect(ojo.getAttribute('aria-pressed')).toBe('true');
  });

  it('mientras entra deshabilita el botón y muestra el mensaje del backend si falla', async () => {
    fixture.componentInstance.email = 'maria@andina.bo';
    fixture.componentInstance.contrasena = 'incorrecta';
    fixture.componentInstance.entrar();
    fixture.detectChanges();
    await fixture.whenStable();

    const boton = pagina().querySelector('button[type="submit"]') as HTMLButtonElement;
    expect(boton.disabled).toBe(true);
    expect(boton.textContent).toContain('Entrando');

    http.expectOne('http://localhost:8080/api/auth/login')
      .flush({ mensaje: 'Email o contraseña incorrectos.' }, { status: 401, statusText: 'Unauthorized' });
    fixture.detectChanges();
    await fixture.whenStable();

    expect(boton.disabled).toBe(false);
    expect(pagina().querySelector('.aviso.error')?.textContent).toContain('incorrectos');
  });
});

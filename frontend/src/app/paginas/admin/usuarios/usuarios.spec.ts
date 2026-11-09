import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Usuarios } from './usuarios';
import { UsuarioAdmin } from './usuarios.service';

describe('Usuarios', () => {
  const URL = 'http://localhost:8080/api/admin/usuarios';

  let fixture: ComponentFixture<Usuarios>;
  let http: HttpTestingController;

  const fila = (id: string, email: string, estado: UsuarioAdmin['estado']): UsuarioAdmin => ({
    id, nombreCompleto: email, email, rol: 'USUARIO', estado, empresa: 'Importadora Andina',
    emailVerificado: true, intentosFallidos: 0, ultimoLogin: null, creadoEn: '2026-11-01T10:00:00-04:00',
  });

  beforeEach(async () => {
    localStorage.clear();
    // Sesion del admin "yo"
    localStorage.setItem('phishshield.tokenAcceso', 't');
    localStorage.setItem('phishshield.usuario',
      JSON.stringify({ id: 'yo', nombreCompleto: 'Admin', email: 'admin@x.bo', rol: 'ADMIN' }));

    await TestBed.configureTestingModule({
      imports: [Usuarios],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Usuarios);
    fixture.detectChanges();
    http.expectOne(URL).flush([fila('yo', 'admin@x.bo', 'activo'), fila('c1', 'carlos@x.bo', 'activo')]);
    await fixture.whenStable();
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  function botonDe(email: string): HTMLButtonElement {
    const filas = [...fixture.nativeElement.querySelectorAll('tbody tr')] as HTMLElement[];
    return filas.find((f) => f.textContent?.includes(email))!.querySelector('button')!;
  }

  it('no deja desactivar la propia cuenta', () => {
    expect(botonDe('admin@x.bo').disabled).toBe(true);
    expect(botonDe('carlos@x.bo').disabled).toBe(false);
  });

  it('desactivar manda PATCH con activo=false y actualiza la fila', async () => {
    botonDe('carlos@x.bo').click();

    const peticion = http.expectOne(`${URL}/c1/estado`);
    expect(peticion.request.method).toBe('PATCH');
    expect(peticion.request.body).toEqual({ activo: false });
    peticion.flush(fila('c1', 'carlos@x.bo', 'inactivo'));
    await fixture.whenStable();

    expect(botonDe('carlos@x.bo').textContent?.trim()).toBe('Activar');
  });
});

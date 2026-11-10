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

  function selectorDeRol(email: string): HTMLSelectElement | null {
    const filas = [...fixture.nativeElement.querySelectorAll('tbody tr')] as HTMLElement[];
    return filas.find((f) => f.textContent?.includes(email))!.querySelector('select.rol');
  }

  it('el propio rol no se puede cambiar; el de otro manda PATCH /rol', async () => {
    expect(selectorDeRol('admin@x.bo')).toBeNull();

    const selector = selectorDeRol('carlos@x.bo')!;
    expect([...selector.options].map((o) => o.value)).toEqual(['USUARIO', 'ADMIN_EMPRESA', 'ADMIN']);
    selector.value = 'ADMIN_EMPRESA';
    selector.dispatchEvent(new Event('change'));

    const peticion = http.expectOne(`${URL}/c1/rol`);
    expect(peticion.request.method).toBe('PATCH');
    expect(peticion.request.body).toEqual({ rol: 'ADMIN_EMPRESA' });
    peticion.flush({ ...fila('c1', 'carlos@x.bo', 'activo'), rol: 'ADMIN_EMPRESA' });
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('.aviso.ok')?.textContent).toContain('Admin. de empresa');
  });

  it('el admin crea un usuario eligiendo una empresa activa', async () => {
    (fixture.nativeElement.querySelector('button.nuevo') as HTMLButtonElement).click();
    http.expectOne('http://localhost:8080/api/empresas').flush([
      { id: 'e1', nombre: 'Andina', nit: null, sector: null, ciudad: null, plan: 'basico', activa: true, creadoEn: '' },
      { id: 'e2', nombre: 'Cerrada', nit: null, sector: null, ciudad: null, plan: 'basico', activa: false, creadoEn: '' },
    ]);
    await fixture.whenStable();
    expect(fixture.componentInstance.empresas().map((e) => e.nombre)).toEqual(['Andina']);

    fixture.componentInstance.nuevo = {
      nombres: 'Pedro', apellidos: '', email: 'pedro@andina.bo', contrasena: 'ClaveInicial1', rol: 'USUARIO', empresa: 'e1',
    };
    fixture.componentInstance.crear();

    const post = http.expectOne(URL);
    expect(post.request.method).toBe('POST');
    expect(post.request.body.empresa).toBe('e1');
    post.flush(fila('p1', 'pedro@andina.bo', 'activo'));
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelectorAll('tbody tr').length).toBe(3);
  });

  it('al crear un ADMIN no se manda empresa', () => {
    fixture.componentInstance.nuevo = {
      nombres: 'Ana', apellidos: '', email: 'ana@x.bo', contrasena: 'ClaveInicial1', rol: 'ADMIN', empresa: 'e1',
    };
    fixture.componentInstance.crear();

    const post = http.expectOne(URL);
    expect(post.request.body.empresa).toBeNull();
    post.flush(fila('a1', 'ana@x.bo', 'activo'));
  });
});

describe('Usuarios como admin de empresa', () => {
  const URL = 'http://localhost:8080/api/admin/usuarios';

  it('no ofrece el rol ADMIN ni pide la lista de empresas', async () => {
    localStorage.clear();
    localStorage.setItem('phishshield.tokenAcceso', 't');
    localStorage.setItem('phishshield.usuario',
      JSON.stringify({ id: 'yo', nombreCompleto: 'Maria', email: 'maria@andina.bo', rol: 'ADMIN_EMPRESA' }));

    await TestBed.configureTestingModule({
      imports: [Usuarios],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(Usuarios);
    fixture.detectChanges();
    http.expectOne(URL).flush([]);
    await fixture.whenStable();

    (fixture.nativeElement.querySelector('button.nuevo') as HTMLButtonElement).click();
    fixture.detectChanges();
    await fixture.whenStable();

    const opciones = [...fixture.nativeElement.querySelectorAll('select[name="rol"] option')]
      .map((o) => (o as HTMLOptionElement).value);
    expect(opciones).toEqual(['USUARIO', 'ADMIN_EMPRESA']);
    expect(fixture.nativeElement.querySelector('select[name="empresa"]')).toBeNull();
    http.verify();   // no hubo GET /api/empresas
    localStorage.clear();
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Auditoria } from './auditoria';
import { RegistroAuditoria } from './auditoria.service';

describe('Auditoria', () => {
  const URL = 'http://localhost:8080/api/admin/auditoria';

  let fixture: ComponentFixture<Auditoria>;
  let http: HttpTestingController;

  const registro: RegistroAuditoria = {
    fecha: '2026-11-09T10:00:00-04:00', accion: 'LOGIN_FALLIDO', usuario: 'nadie@x.bo', entidad: 'usuarios',
    detalle: { email: 'nadie@x.bo', motivo: 'email_no_registrado' }, ip: '190.129.10.25',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Auditoria],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Auditoria);
    fixture.detectChanges();
    http.expectOne(`${URL}/acciones`).flush(['LOGIN_EXITOSO', 'LOGIN_FALLIDO']);
  });

  afterEach(() => http.verify());

  it('muestra IP, acción con nombre legible y el detalle, y llena el selector de acciones', async () => {
    http.expectOne((r) => r.url === URL).flush({
      contenido: [registro], pagina: 0, tamano: 20, totalElementos: 1, totalPaginas: 1,
    });
    await fixture.whenStable();

    const pagina = fixture.nativeElement as HTMLElement;
    expect(pagina.querySelector('td.ip')?.textContent).toContain('190.129.10.25');
    const accion = pagina.querySelector('.badge.atencion') as HTMLElement;
    expect(accion.textContent).toContain('Inicio de sesión fallido');
    expect(accion.getAttribute('title')).toBe('LOGIN_FALLIDO');
    expect(pagina.querySelector('.detalle')?.textContent).toContain('motivo: email_no_registrado');
    const opciones = [...pagina.querySelectorAll('select option')].map((o) => o.textContent?.trim());
    expect(opciones).toEqual(['Todas', 'Inicio de sesión', 'Inicio de sesión fallido']);
  });

  it('el atajo "Inicio de sesión fallido" filtra al instante', () => {
    http.expectOne((r) => r.url === URL).flush({ contenido: [], pagina: 0, tamano: 20, totalElementos: 0, totalPaginas: 0 });
    fixture.detectChanges();

    const chip = ([...fixture.nativeElement.querySelectorAll('.chip-filtro')] as HTMLButtonElement[])
      .find((b) => b.textContent?.trim() === 'Inicio de sesión fallido')!;
    chip.click();

    const peticion = http.expectOne((r) => r.url === URL);
    expect(peticion.request.params.get('accion')).toBe('LOGIN_FALLIDO');
    peticion.flush({ contenido: [], pagina: 0, tamano: 20, totalElementos: 0, totalPaginas: 0 });
  });

  it('manda acción y usuario como filtros', async () => {
    http.expectOne((r) => r.url === URL).flush({ contenido: [], pagina: 0, tamano: 20, totalElementos: 0, totalPaginas: 0 });

    fixture.componentInstance.filtro = { accion: 'LOGIN_FALLIDO', usuario: 'carlos', desde: '', hasta: '2026-11-09' };
    fixture.componentInstance.buscar();

    const peticion = http.expectOne((r) => r.url === URL);
    expect(peticion.request.params.get('accion')).toBe('LOGIN_FALLIDO');
    expect(peticion.request.params.get('usuario')).toBe('carlos');
    expect(peticion.request.params.get('hasta')).toBe('2026-11-09');
    expect(peticion.request.params.has('desde')).toBe(false);
    peticion.flush({ contenido: [], pagina: 0, tamano: 20, totalElementos: 0, totalPaginas: 0 });
  });
});

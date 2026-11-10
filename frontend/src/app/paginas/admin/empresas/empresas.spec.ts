import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Empresas } from './empresas';
import { Empresa } from '../../../empresa.model';

describe('Empresas', () => {
  const URL = 'http://localhost:8080/api/empresas';

  let fixture: ComponentFixture<Empresas>;
  let http: HttpTestingController;

  const andina: Empresa = {
    id: 'e1', nombre: 'Importadora Andina', nit: '1023456789', sector: 'Comercio', ciudad: 'La Paz',
    plan: 'basico', activa: true, creadoEn: '2026-10-01T10:00:00-04:00',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Empresas],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Empresas);
    fixture.detectChanges();
    http.expectOne(URL).flush([andina]);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  const boton = (texto: string) => ([...fixture.nativeElement.querySelectorAll('tbody button')] as HTMLButtonElement[])
    .find((b) => b.textContent?.includes(texto))!;

  it('editar llena el formulario y manda PUT a la misma empresa', async () => {
    boton('Editar').click();
    expect(fixture.componentInstance.formulario.nombre).toBe('Importadora Andina');

    fixture.componentInstance.formulario.plan = 'empresarial';
    fixture.componentInstance.guardar();

    const put = http.expectOne(`${URL}/e1`);
    expect(put.request.method).toBe('PUT');
    expect(put.request.body.plan).toBe('empresarial');
    put.flush({ ...andina, plan: 'empresarial' });
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelectorAll('tbody tr').length).toBe(1);
    expect(fixture.nativeElement.querySelector('.plan')?.textContent).toContain('empresarial');
  });

  it('desactivar manda activo=false y marca la fila como inactiva', async () => {
    boton('Desactivar').click();

    const patch = http.expectOne(`${URL}/e1/estado`);
    expect(patch.request.body).toEqual({ activo: false });
    patch.flush({ ...andina, activa: false });
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('.inactiva')).not.toBeNull();
    expect(boton('Activar')).toBeDefined();
    expect(fixture.nativeElement.querySelector('.aviso.ok')?.textContent).toContain('usuarios');
  });

  it('crear agrega la empresa nueva a la tabla', async () => {
    fixture.componentInstance.formulario = { nombre: 'Clinica San Rafael', nit: '', sector: 'Salud', ciudad: '', plan: 'basico' };
    fixture.componentInstance.guardar();

    const post = http.expectOne(URL);
    expect(post.request.method).toBe('POST');
    post.flush({ ...andina, id: 'e2', nombre: 'Clinica San Rafael', nit: null });
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelectorAll('tbody tr').length).toBe(2);
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Dominios } from './dominios';
import { DominioListado } from './dominios.service';
import { ConfirmacionService } from '../../../core/ui/confirmacion.service';
import { ToastService } from '../../../core/ui/toast.service';

describe('Dominios', () => {
  const URL = 'http://localhost:8080/api/admin/dominios';

  let fixture: ComponentFixture<Dominios>;
  let http: HttpTestingController;

  const fila: DominioListado = {
    id: 'd1', dominio: 'bnb-bo-seguro.com', lista: 'negra', entidad: 'Banco Nacional de Bolivia',
    fuente: 'reporte_usuario', motivo: 'Clon de la banca', activo: true, creadoEn: '2026-11-01T10:00:00-04:00',
  };
  const pagina = (contenido: DominioListado[]) =>
    ({ contenido, pagina: 0, tamano: 20, totalElementos: contenido.length, totalPaginas: 1 });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Dominios],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Dominios);
    fixture.detectChanges();
    http.expectOne('http://localhost:8080/api/admin/entidades')
      .flush([{ nombre: 'Banco Nacional de Bolivia', tipo: 'banco', dominioOficial: 'bnb.com.bo' }]);
  });

  afterEach(() => http.verify());

  it('por defecto lista los activos', () => {
    const peticion = http.expectOne((r) => r.url === URL);
    expect(peticion.request.params.get('activo')).toBe('true');
    peticion.flush(pagina([fila]));
  });

  it('agregar manda POST con los datos y recarga la tabla', async () => {
    http.expectOne((r) => r.url === URL).flush(pagina([]));
    await fixture.whenStable();

    fixture.componentInstance.formulario = {
      dominio: 'BNB-Pagos.xyz', lista: 'negra', motivo: 'Reportado', entidad: 'Banco Nacional de Bolivia',
    };
    fixture.componentInstance.guardar();

    const post = http.expectOne((r) => r.url === URL && r.method === 'POST');
    expect(post.request.body).toEqual({
      dominio: 'BNB-Pagos.xyz', lista: 'negra', motivo: 'Reportado', entidad: 'Banco Nacional de Bolivia',
    });
    post.flush({ ...fila, id: 'd2', dominio: 'bnb-pagos.xyz' });
    http.expectOne((r) => r.url === URL && r.method === 'GET').flush(pagina([fila]));
    await fixture.whenStable();

    const toast = TestBed.inject(ToastService).toasts().at(-1);
    expect(toast?.tipo).toBe('exito');
    expect(toast?.mensaje).toContain('bnb-pagos.xyz');
  });

  it('"Desactivar" pide confirmación y manda activo=false (desactiva, no borra)', async () => {
    http.expectOne((r) => r.url === URL).flush(pagina([fila]));
    await fixture.whenStable();

    const desactivar = fixture.nativeElement
      .querySelector('button[aria-label="Desactivar bnb-bo-seguro.com"]') as HTMLButtonElement;
    expect(desactivar.getAttribute('data-tooltip')).toBe('Desactivar');
    desactivar.click();
    TestBed.inject(ConfirmacionService).responder(true);

    const patch = http.expectOne(`${URL}/d1/estado`);
    expect(patch.request.method).toBe('PATCH');
    expect(patch.request.body).toEqual({ activo: false });
    patch.flush({ ...fila, activo: false });
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('tbody tr')?.classList).toContain('apagado');
  });

  it('muestra el mensaje del backend si hay conflicto entre listas', async () => {
    http.expectOne((r) => r.url === URL).flush(pagina([]));
    fixture.componentInstance.formulario = { dominio: 'visa.com', lista: 'negra', motivo: 'x', entidad: '' };
    fixture.componentInstance.guardar();

    http.expectOne((r) => r.method === 'POST').flush(
      { mensaje: 'visa.com está activo en la lista blanca.' }, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();

    const toast = TestBed.inject(ToastService).toasts().at(-1);
    expect(toast?.tipo).toBe('error');
    expect(toast?.mensaje).toContain('lista blanca');
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Paginador } from './paginador';

describe('Paginador', () => {
  let fixture: ComponentFixture<Paginador>;

  async function montar(pagina: number, totalPaginas: number, totalElementos: number) {
    await TestBed.configureTestingModule({ imports: [Paginador] }).compileComponents();
    fixture = TestBed.createComponent(Paginador);
    fixture.componentRef.setInput('pagina', pagina);
    fixture.componentRef.setInput('totalPaginas', totalPaginas);
    fixture.componentRef.setInput('totalElementos', totalElementos);
    fixture.detectChanges();
    await fixture.whenStable();
  }

  const boton = (etiqueta: string) =>
    fixture.nativeElement.querySelector(`button[aria-label="${etiqueta}"]`) as HTMLButtonElement;
  const texto = () => (fixture.nativeElement.textContent as string).replace(/\s+/g, ' ');

  it('en la primera página no deja ir hacia atrás', async () => {
    await montar(0, 3, 45);
    expect(boton('Página anterior').disabled).toBe(true);
    expect(boton('Página siguiente').disabled).toBe(false);
    expect(texto()).toContain('Mostrando 1–20 de 45');
    expect(texto()).toContain('Página 1 de 3');
  });

  it('en la última página no deja avanzar y emite la página anterior', async () => {
    await montar(2, 3, 45);
    const emitidas: number[] = [];
    fixture.componentInstance.cambiar.subscribe((p) => emitidas.push(p));

    expect(boton('Página siguiente').disabled).toBe(true);
    expect(texto()).toContain('Mostrando 41–45 de 45');
    boton('Página anterior').click();
    expect(emitidas).toEqual([1]);
  });

  it('marca la página actual y resume con "…" las lejanas', async () => {
    await montar(5, 10, 200);
    const emitidas: number[] = [];
    fixture.componentInstance.cambiar.subscribe((p) => emitidas.push(p));

    const numeros = [...fixture.nativeElement.querySelectorAll('button.numero')] as HTMLButtonElement[];
    expect(numeros.map((b) => b.textContent?.trim())).toEqual(['1', '5', '6', '7', '10']);
    expect(fixture.nativeElement.querySelectorAll('.salto').length).toBe(2);
    expect(boton('Página 6').getAttribute('aria-current')).toBe('page');

    boton('Página 10').click();
    expect(emitidas).toEqual([9]);
  });

  it('sin resultados lo dice y deshabilita los dos botones', async () => {
    await montar(0, 0, 0);
    expect(boton('Página anterior').disabled).toBe(true);
    expect(boton('Página siguiente').disabled).toBe(true);
    expect(texto()).toContain('Sin resultados');
  });
});

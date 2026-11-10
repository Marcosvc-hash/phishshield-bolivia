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

  const botones = () => [...fixture.nativeElement.querySelectorAll('button')] as HTMLButtonElement[];

  it('en la primera pagina no deja ir hacia atras', async () => {
    await montar(0, 3, 45);
    const [anterior, siguiente] = botones();
    expect(anterior.disabled).toBe(true);
    expect(siguiente.disabled).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('Pagina 1 de 3 (45 resultados)');
  });

  it('en la ultima pagina no deja avanzar y emite la pagina anterior', async () => {
    await montar(2, 3, 45);
    const emitidas: number[] = [];
    fixture.componentInstance.cambiar.subscribe((p) => emitidas.push(p));

    const [anterior, siguiente] = botones();
    expect(siguiente.disabled).toBe(true);
    anterior.click();
    expect(emitidas).toEqual([1]);
  });

  it('sin resultados lo dice y deshabilita los dos botones', async () => {
    await montar(0, 0, 0);
    expect(botones().every((b) => b.disabled)).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('Sin resultados');
  });
});

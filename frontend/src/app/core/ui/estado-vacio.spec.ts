import { TestBed } from '@angular/core/testing';
import { EstadoVacio } from './estado-vacio';
import { Skeleton } from './skeleton';

describe('EstadoVacio y Skeleton', () => {
  it('el estado vacío muestra título y descripción', async () => {
    await TestBed.configureTestingModule({ imports: [EstadoVacio] }).compileComponents();
    const fixture = TestBed.createComponent(EstadoVacio);
    fixture.componentRef.setInput('titulo', 'Sin alertas');
    fixture.componentRef.setInput('descripcion', 'No hay alertas pendientes.');
    fixture.detectChanges();
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('.titulo')?.textContent).toBe('Sin alertas');
    expect(fixture.nativeElement.querySelector('.descripcion')?.textContent).toContain('pendientes');
  });

  it('el skeleton se anuncia como "cargando" y dibuja tantas filas como se pidan', async () => {
    await TestBed.configureTestingModule({ imports: [Skeleton] }).compileComponents();
    const fixture = TestBed.createComponent(Skeleton);
    fixture.componentRef.setInput('cantidad', 3);
    fixture.detectChanges();
    await fixture.whenStable();

    const raiz = fixture.nativeElement.querySelector('[role="status"]') as HTMLElement;
    expect(raiz.getAttribute('aria-label')).toBe('Cargando…');
    expect(raiz.querySelectorAll('.fila').length).toBe(3);
  });
});

import { TestBed } from '@angular/core/testing';
import { Icono } from './icono';

describe('Icono', () => {
  async function montar(nombre: string, etiqueta: string | null = null) {
    await TestBed.configureTestingModule({ imports: [Icono] }).compileComponents();
    const fixture = TestBed.createComponent(Icono);
    fixture.componentRef.setInput('nombre', nombre);
    fixture.componentRef.setInput('etiqueta', etiqueta);
    fixture.detectChanges();
    await fixture.whenStable();
    return fixture.nativeElement.querySelector('svg') as SVGElement;
  }

  it('dibuja los trazos del icono y por defecto es decorativo', async () => {
    const svg = await montar('escudo-check');
    expect(svg.querySelectorAll('path').length).toBe(2);
    expect(svg.getAttribute('aria-hidden')).toBe('true');
    expect(svg.getAttribute('role')).toBeNull();
  });

  it('con etiqueta lo anuncia como imagen', async () => {
    const svg = await montar('alerta', 'Atención');
    expect(svg.getAttribute('aria-hidden')).toBeNull();
    expect(svg.getAttribute('role')).toBe('img');
    expect(svg.getAttribute('aria-label')).toBe('Atención');
  });
});

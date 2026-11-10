import { Component, input } from '@angular/core';
import { Icono, NombreIcono } from './icono';

/**
 * Lo que se muestra cuando una lista no tiene nada: icono, titulo, una
 * explicacion y, opcionalmente, una accion (lo que se ponga adentro).
 *
 * Uso: <app-estado-vacio icono="buscar" titulo="Sin resultados" descripcion="..." />
 */
@Component({
  selector: 'app-estado-vacio',
  imports: [Icono],
  template: `
    <div class="vacio" [class.compacto]="compacto()">
      <span class="circulo"><app-icono [nombre]="icono()" [tamano]="compacto() ? 20 : 24" /></span>
      <p class="titulo">{{ titulo() }}</p>
      @if (descripcion()) {
        <p class="descripcion">{{ descripcion() }}</p>
      }
      <ng-content />
    </div>
  `,
  styles: `
    .vacio {
      display: flex; flex-direction: column; align-items: center; gap: 8px;
      padding: 48px 24px; text-align: center;
    }
    .vacio.compacto { padding: 24px 16px; }
    .circulo {
      display: grid; place-items: center; width: 48px; height: 48px; margin-bottom: 4px;
      border-radius: 50%; background: var(--superficie-2); color: var(--texto-3);
    }
    .compacto .circulo { width: 40px; height: 40px; }
    .titulo { font-weight: 600; color: var(--texto); }
    .descripcion { max-width: 380px; font-size: var(--t-sm); color: var(--texto-2); }
  `
})
export class EstadoVacio {
  readonly icono = input<NombreIcono>('buscar');
  readonly titulo = input.required<string>();
  readonly descripcion = input<string | null>(null);
  readonly compacto = input(false);
}

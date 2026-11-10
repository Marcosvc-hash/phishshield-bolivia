import { Component, computed, input } from '@angular/core';

/**
 * Esqueleto de carga: bloques grises con brillo mientras llegan los datos.
 * Asi la pagina no "salta" cuando aparece el contenido real.
 *
 * - tipo "tabla": filas con varias columnas.
 * - tipo "tarjetas": la fila de tarjetas KPI del panel.
 * - tipo "texto": unas lineas sueltas.
 */
@Component({
  selector: 'app-skeleton',
  template: `
    <div class="skeleton" role="status" [attr.aria-label]="etiqueta()">
      @switch (tipo()) {
        @case ('tarjetas') {
          <div class="tarjetas">
            @for (i of indices(); track i) {
              <div class="tarjeta"><span class="b corto"></span><span class="b grande"></span><span class="b medio"></span></div>
            }
          </div>
        }
        @case ('tabla') {
          @for (i of indices(); track i) {
            <div class="fila">
              <span class="b"></span><span class="b ancho"></span><span class="b"></span><span class="b corto"></span>
            </div>
          }
        }
        @default {
          @for (i of indices(); track i) {
            <span class="b linea" [style.width.%]="i % 3 === 2 ? 60 : 100"></span>
          }
        }
      }
    </div>
  `,
  styles: `
    .skeleton { display: flex; flex-direction: column; gap: 12px; padding: 16px; }
    .b {
      display: block; height: 12px; border-radius: 6px;
      background: linear-gradient(90deg, #eef2f6 25%, #e2e8f0 37%, #eef2f6 63%);
      background-size: 400% 100%; animation: brillo 1.4s ease infinite;
    }
    .fila { display: grid; grid-template-columns: 1fr 3fr 1fr 0.6fr; gap: 24px; padding: 8px 0; }
    .tarjetas { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; }
    .tarjeta {
      display: flex; flex-direction: column; gap: 12px; padding: 20px;
      border: 1px solid var(--borde); border-radius: var(--radio); background: var(--superficie);
    }
    .grande { height: 28px; width: 50%; }
    .corto { width: 40%; }
    .medio { width: 70%; }
    .linea { height: 14px; }
    @keyframes brillo { from { background-position: 100% 50%; } to { background-position: 0 50%; } }
  `
})
export class Skeleton {
  readonly tipo = input<'tabla' | 'tarjetas' | 'texto'>('tabla');
  readonly cantidad = input(5);
  readonly etiqueta = input('Cargando…');

  protected indices = computed(() => Array.from({ length: this.cantidad() }, (_, i) => i));
}

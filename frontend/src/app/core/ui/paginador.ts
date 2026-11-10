import { Component, computed, input, output } from '@angular/core';

/**
 * Botones "Anterior / Siguiente" y el texto "Pagina 2 de 5 (93 resultados)".
 * No guarda estado: la pagina actual la tiene el componente padre, que
 * vuelve a pedir datos al backend cuando recibe el evento cambiar.
 */
@Component({
  selector: 'app-paginador',
  template: `
    <div class="paginador">
      <button class="btn" [disabled]="!hayAnterior()" (click)="cambiar.emit(pagina() - 1)">Anterior</button>
      <span class="texto">
        @if (totalElementos() === 0) {
          Sin resultados
        } @else {
          Pagina {{ pagina() + 1 }} de {{ totalPaginas() }} ({{ totalElementos() }} resultados)
        }
      </span>
      <button class="btn" [disabled]="!haySiguiente()" (click)="cambiar.emit(pagina() + 1)">Siguiente</button>
    </div>
  `,
  styles: `
    .paginador { display: flex; align-items: center; gap: 0.75rem; padding: 0.75rem 0.25rem 0.25rem; }
    .texto { font-size: 0.85rem; color: #52514e; }
  `
})
export class Paginador {
  readonly pagina = input.required<number>();
  readonly totalPaginas = input.required<number>();
  readonly totalElementos = input.required<number>();
  readonly cambiar = output<number>();

  protected hayAnterior = computed(() => this.pagina() > 0);
  protected haySiguiente = computed(() => this.pagina() + 1 < this.totalPaginas());
}

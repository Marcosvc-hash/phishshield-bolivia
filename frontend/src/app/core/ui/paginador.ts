import { Component, computed, input, output } from '@angular/core';
import { Icono } from './icono';

/** Un boton de pagina (numero base 0) o un "..." entre numeros lejanos. */
type Hueco = { tipo: 'pagina'; numero: number } | { tipo: 'salto'; clave: string };

/**
 * Pie de las tablas paginadas: "Mostrando 21–40 de 93" y los botones
 * anterior / numeros / siguiente. No guarda estado: la pagina actual la
 * tiene el componente padre, que vuelve a pedir datos al backend cuando
 * recibe el evento cambiar.
 */
@Component({
  selector: 'app-paginador',
  imports: [Icono],
  template: `
    <nav class="paginador" aria-label="Paginación">
      <p class="resumen" aria-live="polite">
        @if (totalElementos() === 0) {
          Sin resultados
        } @else {
          Mostrando <strong>{{ desde() }}–{{ hasta() }}</strong> de <strong>{{ totalElementos() }}</strong>
          · Página {{ pagina() + 1 }} de {{ totalPaginas() }}
        }
      </p>
      <div class="controles">
        <button type="button" class="btn btn-secundario btn-sm" [disabled]="!hayAnterior()"
                aria-label="Página anterior" (click)="cambiar.emit(pagina() - 1)">
          <app-icono nombre="chevron-izq" [tamano]="16" /><span class="texto">Anterior</span>
        </button>
        @for (h of huecos(); track h.tipo === 'pagina' ? h.numero : h.clave) {
          @if (h.tipo === 'pagina') {
            <button type="button" class="numero" [class.actual]="h.numero === pagina()"
                    [attr.aria-current]="h.numero === pagina() ? 'page' : null"
                    [attr.aria-label]="'Página ' + (h.numero + 1)" (click)="ir(h.numero)">{{ h.numero + 1 }}</button>
          } @else {
            <span class="salto" aria-hidden="true">…</span>
          }
        }
        <button type="button" class="btn btn-secundario btn-sm" [disabled]="!haySiguiente()"
                aria-label="Página siguiente" (click)="cambiar.emit(pagina() + 1)">
          <span class="texto">Siguiente</span><app-icono nombre="chevron-der" [tamano]="16" />
        </button>
      </div>
    </nav>
  `,
  styles: `
    .paginador {
      display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 12px;
      padding: 12px 16px; border-top: 1px solid var(--borde);
    }
    .resumen { font-size: var(--t-sm); color: var(--texto-2); }
    .resumen strong { color: var(--texto); font-weight: 600; font-variant-numeric: tabular-nums; }
    .controles { display: flex; align-items: center; gap: 4px; }
    .numero {
      min-width: 32px; height: 32px; padding: 0 6px; border: 1px solid transparent; border-radius: var(--radio-sm);
      background: transparent; color: var(--texto-2); font-size: var(--t-sm); font-weight: 500; cursor: pointer;
      font-variant-numeric: tabular-nums;
    }
    .numero:hover { background: var(--superficie-2); color: var(--texto); }
    .numero.actual { background: var(--primario); color: #fff; font-weight: 600; }
    .salto { padding: 0 4px; color: var(--texto-3); }
    @media (max-width: 640px) {
      .paginador { justify-content: center; text-align: center; }
      .texto, .numero:not(.actual), .salto { display: none; }
    }
  `
})
export class Paginador {
  readonly pagina = input.required<number>();
  readonly totalPaginas = input.required<number>();
  readonly totalElementos = input.required<number>();
  /** Filas por pagina, para el texto "Mostrando 21–40". */
  readonly tamano = input(20);
  readonly cambiar = output<number>();

  protected hayAnterior = computed(() => this.pagina() > 0);
  protected haySiguiente = computed(() => this.pagina() + 1 < this.totalPaginas());
  protected desde = computed(() => this.pagina() * this.tamano() + 1);
  protected hasta = computed(() => Math.min((this.pagina() + 1) * this.tamano(), this.totalElementos()));

  /** Primera, ultima y las vecinas de la actual; lo demas se resume con "...". */
  protected huecos = computed<Hueco[]>(() => {
    const total = this.totalPaginas();
    const actual = this.pagina();
    if (total <= 1) {
      return [];
    }
    const visibles = [...new Set([0, actual - 1, actual, actual + 1, total - 1])]
      .filter((n) => n >= 0 && n < total)
      .sort((a, b) => a - b);
    const resultado: Hueco[] = [];
    visibles.forEach((numero, i) => {
      if (i > 0 && numero - visibles[i - 1] > 1) {
        resultado.push({ tipo: 'salto', clave: `s${numero}` });
      }
      resultado.push({ tipo: 'pagina', numero });
    });
    return resultado;
  });

  protected ir(numero: number): void {
    if (numero !== this.pagina()) {
      this.cambiar.emit(numero);
    }
  }
}

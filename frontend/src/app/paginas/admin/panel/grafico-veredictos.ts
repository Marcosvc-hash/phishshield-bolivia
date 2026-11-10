import { Component, ElementRef, OnDestroy, computed, effect, input, viewChild } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { ArcElement, Chart, DoughnutController, Tooltip } from 'chart.js';
import { COLOR_VEREDICTO, ESTILO_GRAFICO, TOOLTIP_BASE } from './grafico-analisis';

Chart.register(DoughnutController, ArcElement, Tooltip);

export interface TotalesVeredicto {
  phishing: number;
  sospechoso: number;
  seguro: number;
}

/**
 * Dona con la proporcion de cada veredicto en el periodo. El total va en el
 * centro y la leyenda (con cantidad y porcentaje) es HTML, asi el valor
 * nunca depende solo del color.
 */
@Component({
  selector: 'app-grafico-veredictos',
  imports: [DecimalPipe],
  template: `
    <div class="contenedor">
      <div class="lienzo">
        <canvas #lienzo role="img" [attr.aria-label]="descripcion()"></canvas>
        <div class="centro" aria-hidden="true">
          <strong>{{ total() | number }}</strong>
          <span>análisis</span>
        </div>
      </div>
      <ul class="leyenda">
        @for (f of filas(); track f.clave) {
          <li>
            <span class="muestra" [style.background]="f.color"></span>
            <span class="nombre">{{ f.nombre }}</span>
            <span class="valor">{{ f.valor | number }}</span>
            <span class="porcentaje">{{ f.porcentaje | number: '1.0-0' }}%</span>
          </li>
        }
      </ul>
    </div>
  `,
  styles: `
    .contenedor { display: flex; flex-direction: column; align-items: center; gap: 20px; }
    .lienzo { position: relative; width: 200px; height: 200px; }
    .centro {
      position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center;
      pointer-events: none; line-height: 1.1;
    }
    .centro strong { font-size: 1.75rem; font-weight: 700; letter-spacing: -0.02em; font-variant-numeric: tabular-nums; }
    .centro span { font-size: var(--t-xs); color: var(--texto-3); }
    .leyenda { width: 100%; margin: 0; padding: 0; list-style: none; display: flex; flex-direction: column; gap: 10px; }
    li { display: grid; grid-template-columns: 12px 1fr auto 44px; align-items: center; gap: 10px; font-size: var(--t-sm); }
    .muestra { width: 12px; height: 12px; border-radius: 3px; }
    .valor { font-weight: 600; font-variant-numeric: tabular-nums; }
    .porcentaje { text-align: right; color: var(--texto-3); font-variant-numeric: tabular-nums; }
  `
})
export class GraficoVeredictos implements OnDestroy {

  totales = input.required<TotalesVeredicto>();

  private lienzo = viewChild.required<ElementRef<HTMLCanvasElement>>('lienzo');
  private grafico: Chart<'doughnut'> | null = null;

  protected total = computed(() => {
    const t = this.totales();
    return t.phishing + t.sospechoso + t.seguro;
  });

  protected filas = computed(() => {
    const t = this.totales();
    const total = Math.max(1, this.total());
    return [
      { clave: 'phishing', nombre: 'Phishing', valor: t.phishing, color: COLOR_VEREDICTO.phishing },
      { clave: 'sospechoso', nombre: 'Sospechoso', valor: t.sospechoso, color: COLOR_VEREDICTO.sospechoso },
      { clave: 'seguro', nombre: 'Seguro', valor: t.seguro, color: COLOR_VEREDICTO.seguro },
    ].map((f) => ({ ...f, porcentaje: (f.valor / total) * 100 }));
  });

  protected descripcion = computed(() =>
    'Distribución de veredictos: ' + this.filas().map((f) => `${f.nombre} ${f.valor}`).join(', '));

  constructor() {
    effect(() => this.dibujar(this.totales()));
  }

  ngOnDestroy(): void {
    this.grafico?.destroy();
  }

  private dibujar(t: TotalesVeredicto): void {
    const contexto = this.lienzo().nativeElement.getContext('2d');
    if (!contexto) {
      return;   // jsdom no tiene canvas
    }
    this.grafico?.destroy();
    const vacio = t.phishing + t.sospechoso + t.seguro === 0;

    this.grafico = new Chart(contexto, {
      type: 'doughnut',
      data: {
        labels: ['Phishing', 'Sospechoso', 'Seguro'],
        datasets: [{
          // Sin datos se dibuja un anillo gris para que la forma no desaparezca
          data: vacio ? [1] : [t.phishing, t.sospechoso, t.seguro],
          backgroundColor: vacio
            ? [ESTILO_GRAFICO.rejilla]
            : [COLOR_VEREDICTO.phishing, COLOR_VEREDICTO.sospechoso, COLOR_VEREDICTO.seguro],
          borderColor: ESTILO_GRAFICO.superficie,
          borderWidth: 2,
          borderRadius: 4,
          hoverOffset: 4,
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        animation: false,
        cutout: '72%',
        plugins: {
          legend: { display: false },
          tooltip: { ...TOOLTIP_BASE, enabled: !vacio },
        },
      },
    });
  }
}

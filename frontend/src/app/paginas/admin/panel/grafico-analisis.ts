import { Component, ElementRef, OnDestroy, effect, input, viewChild } from '@angular/core';
import {
  BarController, BarElement, CategoryScale, Chart, Legend, LinearScale, Tooltip
} from 'chart.js';
import { AnalisisDia } from './panel.model';

// chart.js se arma por piezas: solo se registra lo que usa este grafico
Chart.register(BarController, BarElement, CategoryScale, LinearScale, Tooltip, Legend);

/**
 * Colores de estado (no de serie): rojo = phishing, ambar = sospechoso,
 * verde = seguro. Se validaron para daltonismo; como el ambar tiene poco
 * contraste sobre blanco, el grafico siempre va con leyenda, tooltip y
 * la tabla de datos debajo.
 */
export const COLOR_VEREDICTO = {
  phishing: '#d03b3b',
  sospechoso: '#fab219',
  seguro: '#0ca30c',
} as const;

const TEXTO_SECUNDARIO = '#52514e';
const SUPERFICIE = '#ffffff';

/** Barras apiladas por dia: cuantos analisis hubo y con que veredicto. */
@Component({
  selector: 'app-grafico-analisis',
  template: `<div class="lienzo"><canvas #lienzo aria-label="Analisis por dia segun veredicto" role="img"></canvas></div>`,
  styles: `.lienzo { position: relative; height: 280px; }`
})
export class GraficoAnalisis implements OnDestroy {

  dias = input.required<AnalisisDia[]>();

  private lienzo = viewChild.required<ElementRef<HTMLCanvasElement>>('lienzo');
  private grafico: Chart<'bar'> | null = null;

  constructor() {
    // Se vuelve a dibujar cada vez que cambian los datos
    effect(() => this.dibujar(this.dias()));
  }

  ngOnDestroy(): void {
    this.grafico?.destroy();
  }

  private dibujar(dias: AnalisisDia[]): void {
    const contexto = this.lienzo().nativeElement.getContext('2d');
    if (!contexto) {
      return;   // entorno sin canvas (por ejemplo, las pruebas con jsdom)
    }
    this.grafico?.destroy();

    const serie = (etiqueta: string, color: string, valores: number[]) => ({
      label: etiqueta,
      data: valores,
      backgroundColor: color,
      // 2px del color de fondo entre segmentos apilados: separa los colores
      borderColor: SUPERFICIE,
      borderWidth: { top: 2 },
      borderSkipped: false as const,
      maxBarThickness: 18,
    });

    this.grafico = new Chart(contexto, {
      type: 'bar',
      data: {
        labels: dias.map((d) => fechaCorta(d.dia)),
        datasets: [
          // El primero queda abajo, pegado a la base: phishing es lo mas importante
          serie('Phishing', COLOR_VEREDICTO.phishing, dias.map((d) => d.phishing)),
          serie('Sospechoso', COLOR_VEREDICTO.sospechoso, dias.map((d) => d.sospechosos)),
          serie('Seguro', COLOR_VEREDICTO.seguro, dias.map((d) => d.seguros)),
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        animation: false,
        interaction: { mode: 'index', intersect: false },   // el tooltip aparece con solo pasar por la columna
        scales: {
          x: {
            stacked: true,
            grid: { display: false },
            ticks: { color: TEXTO_SECUNDARIO, maxRotation: 0, autoSkip: true, maxTicksLimit: 10 },
          },
          y: {
            stacked: true,
            beginAtZero: true,
            ticks: { color: TEXTO_SECUNDARIO, precision: 0 },
            grid: { color: '#eef0f3' },
            border: { display: false },
          },
        },
        plugins: {
          legend: {
            position: 'top',
            align: 'start',
            labels: { color: '#1f2933', boxWidth: 12, boxHeight: 12 },
          },
          tooltip: {
            callbacks: {
              title: (items) => dias[items[0].dataIndex]?.dia ?? '',
              footer: (items) => `Total: ${dias[items[0].dataIndex]?.total ?? 0}`,
            },
          },
        },
      },
    });
  }
}

/** "2026-11-09" -> "09/11" */
export function fechaCorta(dia: string): string {
  const [, mes, d] = dia.split('-');
  return `${d}/${mes}`;
}

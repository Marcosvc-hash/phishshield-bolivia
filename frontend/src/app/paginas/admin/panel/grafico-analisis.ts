import { Component, ElementRef, OnDestroy, effect, input, viewChild } from '@angular/core';
import {
  BarController, BarElement, CategoryScale, Chart, Legend, LinearScale, Tooltip
} from 'chart.js';
import { AnalisisDia } from './panel.model';

// chart.js se arma por piezas: solo se registra lo que usa este grafico
Chart.register(BarController, BarElement, CategoryScale, LinearScale, Tooltip, Legend);

/**
 * Colores de estado (no de serie): rojo = phishing, ambar = sospechoso,
 * verde = seguro. Son los --*-solido de styles.css (canvas no lee variables
 * CSS). Se validaron para daltonismo; como el ambar tiene poco contraste
 * sobre blanco, los graficos siempre van con leyenda, tooltip y la tabla de
 * datos debajo.
 */
export const COLOR_VEREDICTO = {
  phishing: '#dc2626',
  sospechoso: '#e8a317',
  seguro: '#0f8a4a',
} as const;

/** Tinta, rejilla y tipografia del sistema de diseno, para todos los graficos. */
export const ESTILO_GRAFICO = {
  texto: '#0f172a',
  textoSecundario: '#475569',
  rejilla: '#e2e8f0',
  superficie: '#ffffff',
  fuente: 'Inter, system-ui, -apple-system, "Segoe UI", sans-serif',
} as const;

Chart.defaults.font.family = ESTILO_GRAFICO.fuente;
Chart.defaults.color = ESTILO_GRAFICO.textoSecundario;

/** Tooltip oscuro, igual en todos los graficos del panel. */
export const TOOLTIP_BASE = {
  backgroundColor: ESTILO_GRAFICO.texto,
  padding: 10,
  cornerRadius: 8,
  boxPadding: 4,
  titleFont: { weight: 600 as const },
  usePointStyle: true,
};

/** Barras apiladas por dia: cuantos analisis hubo y con que veredicto. */
@Component({
  selector: 'app-grafico-analisis',
  template: `<div class="lienzo"><canvas #lienzo aria-label="Análisis por día según veredicto" role="img"></canvas></div>`,
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
      borderColor: ESTILO_GRAFICO.superficie,
      borderWidth: { top: 2 },
      borderSkipped: false as const,
      borderRadius: 3,
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
            border: { color: ESTILO_GRAFICO.rejilla },
            ticks: { maxRotation: 0, autoSkip: true, maxTicksLimit: 10, font: { size: 11 } },
          },
          y: {
            stacked: true,
            beginAtZero: true,
            ticks: { precision: 0, font: { size: 11 } },
            grid: { color: ESTILO_GRAFICO.rejilla },
            border: { display: false },
          },
        },
        plugins: {
          legend: {
            position: 'top',
            align: 'end',
            labels: { color: ESTILO_GRAFICO.texto, boxWidth: 10, boxHeight: 10, useBorderRadius: true, borderRadius: 3, padding: 16 },
          },
          tooltip: {
            ...TOOLTIP_BASE,
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

import { Component, computed, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';

type Veredicto = 'seguro' | 'sospechoso' | 'phishing';

/** Umbrales del motor (AnalizarUrlUseCase): sospechoso desde 40, phishing desde 70. */
export const UMBRAL_SOSPECHOSO = 40;
export const UMBRAL_PHISHING = 70;

const CX = 120;
const CY = 124;
const RADIO = 96;

/** Punto del arco para un valor de 0 (izquierda) a 100 (derecha). */
function punto(valor: number, radio = RADIO): { x: number; y: number } {
  const angulo = Math.PI * (1 - valor / 100);
  return { x: CX + radio * Math.cos(angulo), y: CY - radio * Math.sin(angulo) };
}

/** Trazo SVG del arco entre dos valores (siempre menos de media vuelta). */
function arco(desde: number, hasta: number): string {
  const a = punto(desde);
  const b = punto(hasta);
  return `M ${a.x.toFixed(2)} ${a.y.toFixed(2)} A ${RADIO} ${RADIO} 0 0 1 ${b.x.toFixed(2)} ${b.y.toFixed(2)}`;
}

/**
 * Medidor semicircular del nivel de riesgo (0 a 100). Las tres zonas y las
 * marcas 40 y 70 son las del motor. El numero grande y el texto accesible
 * dicen el valor: el color solo lo acompana.
 */
@Component({
  selector: 'app-medidor-riesgo',
  template: `
    <div class="medidor" role="meter" aria-valuemin="0" aria-valuemax="100"
         [attr.aria-valuenow]="valorSeguro()" [attr.aria-valuetext]="textoAccesible()" [attr.aria-label]="'Nivel de riesgo'">
      <svg viewBox="0 0 240 150" aria-hidden="true" focusable="false">
        <path [attr.d]="zonas.seguro" class="zona seguro" />
        <path [attr.d]="zonas.sospechoso" class="zona sospechoso" />
        <path [attr.d]="zonas.phishing" class="zona phishing" />
        @if (valorSeguro() > 0.5) {
          <path [attr.d]="arcoValor()" class="valor" [class]="veredicto()" />
        }
        @for (m of marcas; track m.valor) {
          <line [attr.x1]="m.adentro.x" [attr.y1]="m.adentro.y" [attr.x2]="m.afuera.x" [attr.y2]="m.afuera.y" class="marca" />
          <text [attr.x]="m.texto.x" [attr.y]="m.texto.y" class="rotulo">{{ m.valor }}</text>
        }
        <text x="24" y="146" class="rotulo">0</text>
        <text x="216" y="146" class="rotulo">100</text>
        <circle [attr.cx]="aguja().x" [attr.cy]="aguja().y" r="9" class="aguja" [class]="veredicto()" />
      </svg>
      <div class="centro">
        <span class="cifra" [class]="veredicto()">{{ valorSeguro() | number: '1.0-0' }}</span>
        <span class="de">de 100</span>
      </div>
    </div>
  `,
  imports: [DecimalPipe],
  styles: `
    :host { display: block; }
    .medidor { position: relative; width: 100%; max-width: 260px; margin: 0 auto; }
    svg { display: block; width: 100%; height: auto; overflow: visible; }
    .zona { fill: none; stroke-width: 18; }
    .zona.seguro { stroke: var(--exito-borde); }
    .zona.sospechoso { stroke: var(--alerta-borde); }
    .zona.phishing { stroke: var(--peligro-borde); }
    .valor { fill: none; stroke-width: 18; }
    .valor.seguro { stroke: var(--exito-solido); }
    .valor.sospechoso { stroke: var(--alerta-solido); }
    .valor.phishing { stroke: var(--peligro-solido); }
    .marca { stroke: var(--texto); stroke-width: 2; }
    .rotulo { font-size: 11px; font-weight: 600; fill: var(--texto-2); text-anchor: middle; }
    .aguja { fill: #fff; stroke-width: 4; filter: drop-shadow(0 1px 2px rgba(15, 23, 42, 0.3)); }
    .aguja.seguro { stroke: var(--exito-solido); }
    .aguja.sospechoso { stroke: var(--alerta-solido); }
    .aguja.phishing { stroke: var(--peligro-solido); }
    .centro {
      position: absolute; left: 0; right: 0; bottom: 6%;
      display: flex; flex-direction: column; align-items: center; line-height: 1;
    }
    .cifra { font-size: 2.75rem; font-weight: 700; letter-spacing: -0.03em; font-variant-numeric: tabular-nums; }
    .cifra.seguro { color: var(--exito); }
    .cifra.sospechoso { color: var(--alerta); }
    .cifra.phishing { color: var(--peligro); }
    .de { margin-top: 4px; font-size: var(--t-xs); color: var(--texto-3); font-weight: 500; }
  `
})
export class MedidorRiesgo {
  readonly valor = input.required<number>();
  readonly veredicto = input.required<Veredicto>();

  /** El backend limita el puntaje a 100; aca tambien, por si acaso. */
  protected valorSeguro = computed(() => Math.min(100, Math.max(0, Number(this.valor()) || 0)));
  protected arcoValor = computed(() => arco(0, this.valorSeguro()));
  protected aguja = computed(() => punto(this.valorSeguro()));
  protected textoAccesible = computed(() => {
    const nombre = { seguro: 'seguro', sospechoso: 'sospechoso', phishing: 'phishing' }[this.veredicto()];
    return `${Math.round(this.valorSeguro())} de 100, ${nombre}`;
  });

  // Un pequeno hueco entre zonas para que se distingan los limites
  protected readonly zonas = {
    seguro: arco(0, UMBRAL_SOSPECHOSO - 0.6),
    sospechoso: arco(UMBRAL_SOSPECHOSO + 0.6, UMBRAL_PHISHING - 0.6),
    phishing: arco(UMBRAL_PHISHING + 0.6, 100),
  };

  protected readonly marcas = [UMBRAL_SOSPECHOSO, UMBRAL_PHISHING].map((valor) => ({
    valor,
    adentro: punto(valor, RADIO - 14),
    afuera: punto(valor, RADIO + 14),
    texto: (() => {
      const p = punto(valor, RADIO + 26);
      return { x: p.x, y: p.y + 4 };
    })(),
  }));
}

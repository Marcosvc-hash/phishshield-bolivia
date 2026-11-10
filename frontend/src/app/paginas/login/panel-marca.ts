import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Icono } from '../../core/ui/icono';
import { Marca } from '../../core/ui/marca';

/**
 * Mitad izquierda de las pantallas de login y registro: la marca y el
 * mensaje del producto. En celular se reduce a una franja con el logotipo.
 */
@Component({
  selector: 'app-panel-marca',
  imports: [RouterLink, Icono, Marca],
  template: `
    <aside class="panel">
      <a routerLink="/" class="enlace-marca" aria-label="PhishShield Bolivia, volver al inicio"><app-marca [claro]="true" /></a>

      <div class="mensaje">
        <h2>Proteja a su organización del phishing</h2>
        <p>Analice enlaces sospechosos, reciba alertas y siga cada intento de suplantación desde un solo panel.</p>
        <ul>
          <li><app-icono nombre="reglas" [tamano]="18" /> Reglas de detección e inteligencia artificial</li>
          <li><app-icono nombre="campana" [tamano]="18" /> Alertas para su equipo ante cada caso de phishing</li>
          <li><app-icono nombre="candado" [tamano]="18" /> Los datos de cada empresa, separados</li>
        </ul>
      </div>

      <p class="pie">Detección de phishing para el contexto boliviano</p>
    </aside>
  `,
  styles: `
    :host { display: contents; }
    .panel {
      position: relative; overflow: hidden; display: flex; flex-direction: column; justify-content: space-between;
      gap: 32px; padding: 40px 48px; color: #dbe4f5;
      background:
        radial-gradient(circle at 85% 15%, rgba(59, 130, 246, 0.35), transparent 45%),
        radial-gradient(circle at 10% 90%, rgba(67, 56, 202, 0.35), transparent 45%),
        linear-gradient(160deg, var(--marino) 0%, #13284f 100%);
    }
    .panel::after {
      content: ""; position: absolute; inset: 0; pointer-events: none; opacity: 0.08;
      background-image: linear-gradient(#fff 1px, transparent 1px), linear-gradient(90deg, #fff 1px, transparent 1px);
      background-size: 32px 32px;
    }
    .enlace-marca { position: relative; z-index: 1; width: fit-content; border-radius: var(--radio-sm); text-decoration: none; }
    .enlace-marca:focus-visible { outline-color: #fff; }
    .mensaje { position: relative; z-index: 1; max-width: 440px; }
    h2 { color: #fff; font-size: 2rem; font-weight: 700; letter-spacing: -0.03em; line-height: 1.15; }
    p { margin-top: 12px; font-size: var(--t-md); line-height: 1.6; }
    ul { display: flex; flex-direction: column; gap: 14px; margin: 32px 0 0; padding: 0; list-style: none; }
    li { display: flex; align-items: center; gap: 12px; color: #fff; font-weight: 500; }
    li app-icono {
      display: grid; place-items: center; width: 34px; height: 34px; border-radius: 8px;
      background: rgba(255, 255, 255, 0.1); color: #93c5fd;
    }
    .pie { position: relative; z-index: 1; font-size: var(--t-sm); color: #9fb0cf; }
    @media (max-width: 900px) {
      .panel { padding: 16px 20px; }
      .mensaje, .pie { display: none; }
    }
  `
})
export class PanelMarca {
}

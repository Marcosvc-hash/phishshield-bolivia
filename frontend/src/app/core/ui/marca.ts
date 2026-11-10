import { Component, input } from '@angular/core';
import { Icono } from './icono';

/**
 * Logotipo de PhishShield: escudo en un cuadro azul y el nombre.
 * "claro" es para fondos oscuros (menu lateral, panel de login).
 */
@Component({
  selector: 'app-marca',
  imports: [Icono],
  template: `
    <span class="marca" [class.claro]="claro()">
      <span class="sello"><app-icono nombre="escudo-check" [tamano]="compacta() ? 18 : 20" /></span>
      <span class="nombre">PhishShield<span class="pais">Bolivia</span></span>
    </span>
  `,
  styles: `
    .marca { display: inline-flex; align-items: center; gap: 10px; color: var(--texto); }
    .sello {
      display: grid; place-items: center; width: 34px; height: 34px; border-radius: 9px;
      background: linear-gradient(135deg, #2563eb, #1e3a8a); color: #fff;
      box-shadow: 0 2px 6px rgba(29, 78, 216, 0.35);
    }
    .nombre { display: flex; flex-direction: column; font-weight: 700; font-size: 1.05rem; letter-spacing: -0.02em; line-height: 1.1; }
    .pais { font-size: 0.68rem; font-weight: 600; letter-spacing: 0.14em; text-transform: uppercase; color: var(--texto-3); }
    .claro { color: #fff; }
    .claro .pais { color: #93c5fd; }
  `
})
export class Marca {
  readonly claro = input(false);
  readonly compacta = input(false);
}

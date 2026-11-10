import { Component, ElementRef, effect, inject, viewChild } from '@angular/core';
import { ConfirmacionService } from './confirmacion.service';
import { Icono } from './icono';

/**
 * Modal de confirmacion. Va una sola vez, en App, y muestra lo que pida
 * ConfirmacionService.
 *
 * Accesibilidad: role="alertdialog", el foco entra al boton Cancelar (lo mas
 * seguro ante una accion delicada), Tab no sale del modal, Escape cancela y,
 * al cerrar, el foco vuelve al boton que lo abrio.
 */
@Component({
  selector: 'app-modal-confirmacion',
  imports: [Icono],
  host: { '(document:keydown.escape)': 'cerrarConEscape()' },
  template: `
    @if (servicio.solicitud(); as s) {
      <div class="fondo" (click)="servicio.responder(false)"></div>
      <div class="modal" role="alertdialog" aria-modal="true"
           aria-labelledby="modal-titulo" aria-describedby="modal-mensaje" (keydown.tab)="atraparFoco($event)">
        <div class="icono" [class.peligro]="s.peligro">
          <app-icono [nombre]="s.peligro ? 'alerta' : 'info'" [tamano]="22" />
        </div>
        <div class="texto">
          <h2 id="modal-titulo">{{ s.titulo }}</h2>
          <p id="modal-mensaje">{{ s.mensaje }}</p>
        </div>
        <div class="botones">
          <button #cancelar type="button" class="btn btn-secundario" (click)="servicio.responder(false)">Cancelar</button>
          <button #confirmar type="button" class="btn" [class.btn-peligro]="s.peligro" [class.btn-primario]="!s.peligro"
                  (click)="servicio.responder(true)">{{ s.confirmar }}</button>
        </div>
      </div>
    }
  `,
  styles: `
    .fondo { position: fixed; inset: 0; z-index: 80; background: rgba(15, 23, 42, 0.5); animation: aparecer 150ms; }
    .modal {
      position: fixed; z-index: 81; left: 50%; top: 50%; transform: translate(-50%, -50%);
      width: min(460px, calc(100vw - 32px)); display: grid; grid-template-columns: auto 1fr; gap: 16px;
      padding: 24px; background: var(--superficie); border-radius: var(--radio-lg); box-shadow: var(--sombra-3);
      animation: aparecer 150ms;
    }
    .icono {
      display: grid; place-items: center; width: 44px; height: 44px; border-radius: 50%;
      background: var(--primario-suave); color: var(--primario);
    }
    .icono.peligro { background: var(--peligro-suave); color: var(--peligro); }
    h2 { font-size: var(--t-md); font-weight: 600; margin-top: 2px; }
    p { margin-top: 6px; color: var(--texto-2); font-size: var(--t-sm); }
    .botones { grid-column: 1 / -1; display: flex; justify-content: flex-end; gap: 8px; margin-top: 8px; }
    @keyframes aparecer { from { opacity: 0; } }
    @media (max-width: 480px) {
      .modal { grid-template-columns: 1fr; }
      .botones { flex-direction: column-reverse; }
      .botones .btn { width: 100%; }
    }
  `
})
export class ModalConfirmacion {

  protected servicio = inject(ConfirmacionService);

  private cancelar = viewChild<ElementRef<HTMLButtonElement>>('cancelar');
  private confirmar = viewChild<ElementRef<HTMLButtonElement>>('confirmar');
  private focoAnterior: HTMLElement | null = null;

  constructor() {
    effect(() => {
      const abierto = this.servicio.solicitud() !== null;
      const boton = this.cancelar();
      if (abierto && boton) {
        if (!this.focoAnterior) {
          this.focoAnterior = document.activeElement as HTMLElement | null;
        }
        boton.nativeElement.focus();
      } else if (!abierto && this.focoAnterior) {
        this.focoAnterior.focus?.();
        this.focoAnterior = null;
      }
    });
  }

  protected cerrarConEscape(): void {
    if (this.servicio.solicitud()) {
      this.servicio.responder(false);
    }
  }

  /** Solo hay dos botones: Tab y Shift+Tab saltan de uno al otro. */
  protected atraparFoco(evento: Event): void {
    const tecla = evento as KeyboardEvent;
    const primero = this.cancelar()?.nativeElement;
    const ultimo = this.confirmar()?.nativeElement;
    if (!primero || !ultimo) {
      return;
    }
    if (tecla.shiftKey && document.activeElement === primero) {
      ultimo.focus();
      tecla.preventDefault();
    } else if (!tecla.shiftKey && document.activeElement === ultimo) {
      primero.focus();
      tecla.preventDefault();
    }
  }
}

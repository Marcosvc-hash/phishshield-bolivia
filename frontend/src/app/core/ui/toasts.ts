import { Component, inject } from '@angular/core';
import { Icono, NombreIcono } from './icono';
import { TipoToast, ToastService } from './toast.service';

/**
 * Contenedor de los toasts. Va una sola vez, en App. Los de exito se anuncian
 * con aria-live="polite"; los de error con role="alert", que interrumpe.
 */
@Component({
  selector: 'app-toasts',
  imports: [Icono],
  template: `
    <div class="pila" aria-live="polite">
      @for (t of servicio.toasts(); track t.id) {
        <div class="toast" [class]="t.tipo" [attr.role]="t.tipo === 'error' ? 'alert' : 'status'">
          <app-icono [nombre]="icono(t.tipo)" class="icono" />
          <p>{{ t.mensaje }}</p>
          <button type="button" class="btn btn-icono" aria-label="Cerrar aviso" (click)="servicio.cerrar(t.id)">
            <app-icono nombre="x" [tamano]="16" />
          </button>
        </div>
      }
    </div>
  `,
  styles: `
    .pila {
      position: fixed; right: 24px; bottom: 24px; z-index: 90;
      display: flex; flex-direction: column; gap: 8px; width: min(400px, calc(100vw - 32px));
    }
    .toast {
      display: flex; align-items: flex-start; gap: 12px; padding: 12px 8px 12px 16px;
      background: var(--superficie); border: 1px solid var(--borde); border-left: 4px solid var(--primario);
      border-radius: var(--radio); box-shadow: var(--sombra-3); animation: entrar 200ms ease-out;
    }
    .toast p { flex: 1; padding-top: 6px; font-size: var(--t-sm); color: var(--texto); }
    .icono { margin-top: 6px; color: var(--primario); }
    .exito { border-left-color: var(--exito-solido); }
    .exito .icono { color: var(--exito); }
    .error { border-left-color: var(--peligro-solido); }
    .error .icono { color: var(--peligro); }
    @keyframes entrar { from { opacity: 0; transform: translateY(8px); } }
    @media (max-width: 640px) {
      .pila { left: 16px; right: 16px; bottom: auto; top: 16px; width: auto; }
    }
  `
})
export class Toasts {
  protected servicio = inject(ToastService);

  protected icono(tipo: TipoToast): NombreIcono {
    return tipo === 'exito' ? 'check-circulo' : tipo === 'error' ? 'x-circulo' : 'info';
  }
}

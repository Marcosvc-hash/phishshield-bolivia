import { Injectable, signal } from '@angular/core';

export type TipoToast = 'exito' | 'error' | 'info';

export interface Toast {
  id: number;
  tipo: TipoToast;
  mensaje: string;
}

/**
 * Avisos flotantes que desaparecen solos ("Cambios guardados", "No se pudo...").
 * Los muestra <app-toasts>, que esta una sola vez en App. Cualquier pagina
 * los dispara con inject(ToastService).exito('...').
 */
@Injectable({ providedIn: 'root' })
export class ToastService {

  /** Cuanto dura cada tipo en pantalla: un error se deja leer mas tiempo. */
  static readonly DURACION_MS: Record<TipoToast, number> = { exito: 5000, info: 5000, error: 9000 };

  private readonly lista = signal<Toast[]>([]);
  readonly toasts = this.lista.asReadonly();
  private siguienteId = 1;

  exito(mensaje: string): void {
    this.mostrar('exito', mensaje);
  }

  error(mensaje: string): void {
    this.mostrar('error', mensaje);
  }

  info(mensaje: string): void {
    this.mostrar('info', mensaje);
  }

  cerrar(id: number): void {
    this.lista.update((actual) => actual.filter((t) => t.id !== id));
  }

  private mostrar(tipo: TipoToast, mensaje: string): void {
    const id = this.siguienteId++;
    // Como maximo 4 a la vez: el mas viejo se va
    this.lista.update((actual) => [...actual.slice(-3), { id, tipo, mensaje }]);
    setTimeout(() => this.cerrar(id), ToastService.DURACION_MS[tipo]);
  }
}

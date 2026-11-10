import { Injectable, signal } from '@angular/core';
import { Observable, Subject } from 'rxjs';

export interface OpcionesConfirmacion {
  titulo: string;
  mensaje: string;
  /** Texto del boton que confirma, por ejemplo "Desactivar". */
  confirmar: string;
  /** true pinta el boton de rojo: la accion quita acceso o cierra algo. */
  peligro?: boolean;
}

interface Solicitud extends OpcionesConfirmacion {
  respuesta: Subject<boolean>;
}

/**
 * Pide confirmacion antes de una accion delicada (desactivar, descartar,
 * cambiar un rol). El modal lo dibuja <app-modal-confirmacion>, que esta
 * una sola vez en App.
 *
 * Uso: this.confirmacion.pedir({...}).subscribe((ok) => { if (ok) ... })
 * Equivale en C# a un "await dialogo.ShowAsync()" que devuelve true/false.
 */
@Injectable({ providedIn: 'root' })
export class ConfirmacionService {

  private readonly actual = signal<Solicitud | null>(null);
  readonly solicitud = this.actual.asReadonly();

  pedir(opciones: OpcionesConfirmacion): Observable<boolean> {
    // Si habia otra abierta, se toma como cancelada
    this.responder(false);
    const respuesta = new Subject<boolean>();
    this.actual.set({ ...opciones, respuesta });
    return respuesta.asObservable();
  }

  /** La llama el modal (o una prueba) con la decision del usuario. */
  responder(confirmado: boolean): void {
    const solicitud = this.actual();
    if (!solicitud) {
      return;
    }
    this.actual.set(null);
    solicitud.respuesta.next(confirmado);
    solicitud.respuesta.complete();
  }
}

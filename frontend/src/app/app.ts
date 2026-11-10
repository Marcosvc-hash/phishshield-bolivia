import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ModalConfirmacion } from './core/ui/modal-confirmacion';
import { Toasts } from './core/ui/toasts';

/**
 * Raiz de la aplicacion: muestra la pagina que corresponde a la ruta y,
 * una sola vez para todas, los toasts y el modal de confirmacion.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Toasts, ModalConfirmacion],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
}

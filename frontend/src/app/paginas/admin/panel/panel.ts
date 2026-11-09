import { Component, inject } from '@angular/core';
import { AuthService } from '../../../core/auth/auth.service';

/** Panel de administracion (las tarjetas y graficos llegan en la siguiente fase). */
@Component({
  selector: 'app-panel',
  template: `
    <h1>Panel</h1>
    <p class="aviso">Bienvenido, {{ auth.usuario()?.nombreCompleto }}. Las estadisticas se agregan en la siguiente fase.</p>
  `
})
export class Panel {
  protected auth = inject(AuthService);
}

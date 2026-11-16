import { Component } from '@angular/core';
import { Analizador } from '../../../analizador/analizador';

/**
 * El analizador de URL dentro del panel. No tiene logica propia: solo pone
 * el MISMO componente <app-analizador> que usa la pagina publica dentro del
 * marco del panel (menu lateral y barra superior los pone AdminLayout).
 */
@Component({
  selector: 'app-analizar',
  imports: [Analizador],
  template: `
    <div class="pagina-cabecera">
      <p class="pagina-descripcion">
        Pegue un enlace para saber si es seguro, sospechoso o phishing. El análisis queda registrado a su nombre.
      </p>
    </div>
    <app-analizador />
  `,
  // Ancho de lectura comodo: en pantallas grandes el resultado no se estira de borde a borde
  styles: `:host { display: block; max-width: 960px; }`
})
export class Analizar {
}

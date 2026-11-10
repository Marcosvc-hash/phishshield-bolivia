import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/** Raiz de la aplicacion: solo muestra la pagina que corresponde a la ruta. */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
}

import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Analizador } from '../../analizador/analizador';
import { AuthService } from '../../core/auth/auth.service';

/** Pagina publica: el analizador de URL, con o sin sesion. */
@Component({
  selector: 'app-inicio',
  imports: [Analizador, RouterLink],
  templateUrl: './inicio.html',
  styleUrl: './inicio.css'
})
export class Inicio {
  protected auth = inject(AuthService);
}

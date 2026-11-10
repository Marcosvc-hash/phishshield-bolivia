import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { mensajeDeError } from '../../core/errores';
import { Icono } from '../../core/ui/icono';
import { PanelMarca } from './panel-marca';

/** Formato basico de email: algo@algo.algo, sin espacios. */
export const FORMATO_EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink, Icono, PanelMarca],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {

  private auth = inject(AuthService);
  private router = inject(Router);
  private ruta = inject(ActivatedRoute);

  email = '';
  contrasena = '';
  enviando = signal(false);
  error = signal<string | null>(null);
  mostrarContrasena = signal(false);

  /**
   * Un campo muestra su error cuando la persona ya paso por el (blur) o
   * intento enviar. Asi no aparece todo en rojo apenas se abre la pagina.
   */
  protected tocado = { email: false, contrasena: false };
  protected intentoEnviar = false;

  protected errorEmail(): string | null {
    if (!this.tocado.email && !this.intentoEnviar) {
      return null;
    }
    if (!this.email.trim()) {
      return 'Escriba su correo electrónico.';
    }
    return FORMATO_EMAIL.test(this.email.trim()) ? null : 'Revise el correo: debe tener la forma nombre@empresa.bo';
  }

  protected errorContrasena(): string | null {
    return (this.tocado.contrasena || this.intentoEnviar) && !this.contrasena ? 'Escriba su contraseña.' : null;
  }

  entrar(): void {
    this.intentoEnviar = true;
    if (!this.email.trim() || !this.contrasena) {
      this.error.set('Escriba su email y su contraseña.');
      return;
    }
    if (this.errorEmail()) {
      this.error.set(null);
      return;
    }
    this.enviando.set(true);
    this.error.set(null);

    this.auth.login(this.email, this.contrasena).subscribe({
      next: () => {
        // Si el guard lo mando aca desde una pagina protegida, vuelve a ella
        const volver = this.ruta.snapshot.queryParamMap.get('volver');
        this.router.navigateByUrl(volver || this.auth.inicioSegunRol());
      },
      error: (err) => {
        this.error.set(mensajeDeError(err, 'No se pudo iniciar sesión.'));
        this.enviando.set(false);
      }
    });
  }
}

import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { mensajeDeError } from '../../core/errores';

@Component({
  selector: 'app-registro',
  imports: [FormsModule, RouterLink],
  templateUrl: './registro.html',
  styleUrl: '../login/login.css'
})
export class Registro {

  private auth = inject(AuthService);
  private router = inject(Router);

  nombres = '';
  apellidos = '';
  email = '';
  contrasena = '';
  confirmacion = '';
  enviando = signal(false);
  error = signal<string | null>(null);

  /** Crea la cuenta y, si sale bien, inicia sesion de una vez. */
  registrar(): void {
    if (!this.nombres.trim() || !this.email.trim()) {
      this.error.set('Los nombres y el email son obligatorios.');
      return;
    }
    if (this.contrasena.length < 8) {
      this.error.set('La contrasena debe tener al menos 8 caracteres.');
      return;
    }
    if (this.contrasena !== this.confirmacion) {
      this.error.set('Las contrasenas no coinciden.');
      return;
    }
    this.enviando.set(true);
    this.error.set(null);

    this.auth.registrar({
      nombres: this.nombres,
      apellidos: this.apellidos || undefined,
      email: this.email,
      contrasena: this.contrasena
    }).pipe(
      switchMap(() => this.auth.login(this.email, this.contrasena))
    ).subscribe({
      next: () => this.router.navigateByUrl('/'),
      error: (err) => {
        this.error.set(mensajeDeError(err, 'No se pudo crear la cuenta.'));
        this.enviando.set(false);
      }
    });
  }
}

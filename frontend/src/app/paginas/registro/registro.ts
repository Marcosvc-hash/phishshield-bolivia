import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { mensajeDeError } from '../../core/errores';
import { Icono } from '../../core/ui/icono';
import { FORMATO_EMAIL } from '../login/login';
import { PanelMarca } from '../login/panel-marca';

type Campo = 'nombres' | 'email' | 'contrasena' | 'confirmacion';

@Component({
  selector: 'app-registro',
  imports: [FormsModule, RouterLink, Icono, PanelMarca],
  templateUrl: './registro.html',
  styleUrl: '../login/login.css'
})
export class Registro {

  static readonly MINIMO_CONTRASENA = 8;

  private auth = inject(AuthService);
  private router = inject(Router);

  nombres = '';
  apellidos = '';
  email = '';
  contrasena = '';
  confirmacion = '';
  enviando = signal(false);
  error = signal<string | null>(null);
  mostrarContrasena = signal(false);

  /** Igual que en el login: el error de un campo aparece despues de pasar por el. */
  protected tocado: Record<Campo, boolean> = { nombres: false, email: false, contrasena: false, confirmacion: false };
  protected intentoEnviar = false;
  protected readonly minimo = Registro.MINIMO_CONTRASENA;

  /** Mensaje de error de cada campo, o null si esta bien (o todavia no se toco). */
  protected errorDe(campo: Campo): string | null {
    if (!this.tocado[campo] && !this.intentoEnviar) {
      return null;
    }
    switch (campo) {
      case 'nombres':
        return this.nombres.trim() ? null : 'Escriba sus nombres.';
      case 'email':
        if (!this.email.trim()) {
          return 'Escriba su correo electrónico.';
        }
        return FORMATO_EMAIL.test(this.email.trim()) ? null : 'Revise el correo: debe tener la forma nombre@empresa.bo';
      case 'contrasena':
        return this.contrasena.length >= this.minimo ? null : `Use al menos ${this.minimo} caracteres.`;
      case 'confirmacion':
        return this.confirmacion && this.confirmacion === this.contrasena ? null : 'Las contraseñas no coinciden.';
    }
  }

  protected largoSuficiente(): boolean {
    return this.contrasena.length >= this.minimo;
  }

  /** Crea la cuenta y, si sale bien, inicia sesion de una vez. */
  registrar(): void {
    this.intentoEnviar = true;
    if (!this.nombres.trim() || !this.email.trim()) {
      this.error.set('Los nombres y el email son obligatorios.');
      return;
    }
    if (this.contrasena.length < 8) {
      this.error.set('La contraseña debe tener al menos 8 caracteres.');
      return;
    }
    if (this.contrasena !== this.confirmacion) {
      this.error.set('Las contraseñas no coinciden.');
      return;
    }
    if (this.errorDe('email')) {
      this.error.set(null);
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

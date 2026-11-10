import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { mensajeDeError } from '../../core/errores';

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink],
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

  entrar(): void {
    if (!this.email.trim() || !this.contrasena) {
      this.error.set('Escriba su email y su contrasena.');
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
        this.error.set(mensajeDeError(err, 'No se pudo iniciar sesion.'));
        this.enviando.set(false);
      }
    });
  }
}

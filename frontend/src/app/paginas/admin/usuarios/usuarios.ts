import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { AuthService } from '../../../core/auth/auth.service';
import { Rol } from '../../../core/auth/auth.model';
import { mensajeDeError } from '../../../core/errores';
import { UsuarioAdmin, UsuariosService } from './usuarios.service';

/**
 * Gestion simple de usuarios: listar y activar/desactivar.
 * El ADMIN ve a todos; el ADMIN_EMPRESA, a los de su empresa (lo filtra el backend).
 */
@Component({
  selector: 'app-usuarios',
  imports: [DatePipe],
  templateUrl: './usuarios.html',
  styleUrl: './usuarios.css'
})
export class Usuarios implements OnInit {

  protected auth = inject(AuthService);
  private servicio = inject(UsuariosService);

  usuarios = signal<UsuarioAdmin[]>([]);
  cargando = signal(true);
  error = signal<string | null>(null);
  /** id del usuario cuyo cambio se esta guardando (para deshabilitar su boton). */
  guardando = signal<string | null>(null);

  ngOnInit(): void {
    this.servicio.listar().subscribe({
      next: (lista) => {
        this.usuarios.set(lista);
        this.cargando.set(false);
      },
      error: (err) => {
        this.error.set(mensajeDeError(err, 'No se pudieron cargar los usuarios.'));
        this.cargando.set(false);
      }
    });
  }

  /** Nadie puede desactivarse a si mismo (el backend tambien lo impide). */
  esUnoMismo(u: UsuarioAdmin): boolean {
    return u.id === this.auth.usuario()?.id;
  }

  alternarEstado(u: UsuarioAdmin): void {
    const activar = u.estado !== 'activo';
    this.guardando.set(u.id);
    this.error.set(null);

    this.servicio.cambiarEstado(u.id, activar).subscribe({
      next: (actualizado) => {
        // Se reemplaza solo la fila que cambio
        this.usuarios.update((lista) => lista.map((x) => (x.id === actualizado.id ? actualizado : x)));
        this.guardando.set(null);
      },
      error: (err) => {
        this.error.set(mensajeDeError(err, 'No se pudo cambiar el estado.'));
        this.guardando.set(null);
      }
    });
  }

  nombreRol(rol: Rol): string {
    switch (rol) {
      case 'ADMIN': return 'Administrador';
      case 'ADMIN_EMPRESA': return 'Admin. de empresa';
      default: return 'Usuario';
    }
  }
}

import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/auth/auth.service';
import { Rol } from '../../../core/auth/auth.model';
import { mensajeDeError } from '../../../core/errores';
import { Empresa } from '../../../empresa.model';
import { EmpresaService } from '../../../empresa.service';
import { NuevoUsuario, UsuarioAdmin, UsuariosService } from './usuarios.service';

const NUEVO_VACIO: NuevoUsuario = { nombres: '', apellidos: '', email: '', contrasena: '', rol: 'USUARIO', empresa: null };

/**
 * Gestion de usuarios: listar, crear, activar/desactivar y cambiar el rol.
 * El ADMIN ve a todos; el ADMIN_EMPRESA, a los de su empresa (lo filtra el backend).
 * Los selectores solo ofrecen lo que cada rol puede hacer; aun asi, la regla
 * real esta en el backend (PoliticaGestionUsuarios).
 */
@Component({
  selector: 'app-usuarios',
  imports: [DatePipe, FormsModule],
  templateUrl: './usuarios.html',
  styleUrl: './usuarios.css'
})
export class Usuarios implements OnInit {

  protected auth = inject(AuthService);
  private servicio = inject(UsuariosService);
  private empresaService = inject(EmpresaService);

  usuarios = signal<UsuarioAdmin[]>([]);
  cargando = signal(true);
  error = signal<string | null>(null);
  aviso = signal<string | null>(null);
  /** id del usuario cuyo cambio se esta guardando (para deshabilitar su boton). */
  guardando = signal<string | null>(null);

  mostrarFormulario = signal(false);
  nuevo: NuevoUsuario = { ...NUEVO_VACIO };
  creando = signal(false);
  /** Solo empresas activas: a una inactiva no se le pueden crear usuarios. */
  empresas = signal<Empresa[]>([]);

  /** Un admin de empresa nunca asigna el rol ADMIN. */
  protected rolesAsignables = computed<Rol[]>(() =>
    this.auth.tieneRol('ADMIN') ? ['USUARIO', 'ADMIN_EMPRESA', 'ADMIN'] : ['USUARIO', 'ADMIN_EMPRESA']);

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

  abrirFormulario(): void {
    this.mostrarFormulario.set(true);
    this.nuevo = { ...NUEVO_VACIO };
    // La lista de empresas (/api/empresas) es solo para el ADMIN; el admin
    // de empresa no elige: el usuario va a su propia empresa.
    if (this.auth.tieneRol('ADMIN') && this.empresas().length === 0) {
      this.empresaService.listar().subscribe({
        next: (lista) => this.empresas.set(lista.filter((e) => e.activa)),
        error: (err) => this.error.set(mensajeDeError(err, 'No se pudieron cargar las empresas.'))
      });
    }
  }

  crear(): void {
    // Un administrador de la plataforma no pertenece a una empresa
    const datos: NuevoUsuario = { ...this.nuevo, empresa: this.nuevo.rol === 'ADMIN' ? null : this.nuevo.empresa || null };
    this.creando.set(true);
    this.error.set(null);
    this.servicio.crear(datos).subscribe({
      next: (creado) => {
        this.usuarios.update((lista) =>
          [...lista, creado].sort((a, b) => a.nombreCompleto.localeCompare(b.nombreCompleto)));
        this.aviso.set(`Se creo la cuenta de ${creado.email}. Comparta la contrasena inicial por un canal seguro.`);
        this.mostrarFormulario.set(false);
        this.creando.set(false);
      },
      error: (err) => {
        this.error.set(mensajeDeError(err, 'No se pudo crear el usuario.'));
        this.creando.set(false);
      }
    });
  }

  /** Nadie puede desactivarse ni cambiarse el rol a si mismo (el backend tambien lo impide). */
  esUnoMismo(u: UsuarioAdmin): boolean {
    return u.id === this.auth.usuario()?.id;
  }

  alternarEstado(u: UsuarioAdmin): void {
    const activar = u.estado !== 'activo';
    this.guardando.set(u.id);
    this.error.set(null);

    this.servicio.cambiarEstado(u.id, activar).subscribe({
      next: (actualizado) => this.reemplazar(actualizado),
      error: (err) => {
        this.error.set(mensajeDeError(err, 'No se pudo cambiar el estado.'));
        this.guardando.set(null);
      }
    });
  }

  cambiarRol(u: UsuarioAdmin, rol: Rol): void {
    if (rol === u.rol) {
      return;
    }
    this.guardando.set(u.id);
    this.error.set(null);
    this.servicio.cambiarRol(u.id, rol).subscribe({
      next: (actualizado) => {
        this.reemplazar(actualizado);
        this.aviso.set(`${actualizado.email} ahora es ${this.nombreRol(actualizado.rol)}.`);
      },
      error: (err) => {
        this.error.set(mensajeDeError(err, 'No se pudo cambiar el rol.'));
        this.guardando.set(null);
        // Se vuelve a dibujar la fila para que el selector muestre el rol real
        this.usuarios.update((lista) => [...lista]);
      }
    });
  }

  /** Se reemplaza solo la fila que cambio. */
  private reemplazar(actualizado: UsuarioAdmin): void {
    this.usuarios.update((lista) => lista.map((x) => (x.id === actualizado.id ? actualizado : x)));
    this.guardando.set(null);
  }

  nombreRol(rol: Rol): string {
    switch (rol) {
      case 'ADMIN': return 'Administrador';
      case 'ADMIN_EMPRESA': return 'Admin. de empresa';
      default: return 'Usuario';
    }
  }
}

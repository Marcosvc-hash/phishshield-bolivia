import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { filter } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { Rol } from '../../../core/auth/auth.model';
import { mensajeDeError } from '../../../core/errores';
import { paginarLocal } from '../../../core/pagina';
import { ConfirmacionService } from '../../../core/ui/confirmacion.service';
import { EstadoVacio } from '../../../core/ui/estado-vacio';
import { nombreEstadoUsuario } from '../../../core/ui/etiquetas';
import { Icono } from '../../../core/ui/icono';
import { Paginador } from '../../../core/ui/paginador';
import { Skeleton } from '../../../core/ui/skeleton';
import { ToastService } from '../../../core/ui/toast.service';
import { Empresa } from '../../../empresa.model';
import { EmpresaService } from '../../../empresa.service';
import { NuevoUsuario, UsuarioAdmin, UsuariosService } from './usuarios.service';

const NUEVO_VACIO: NuevoUsuario = { nombres: '', apellidos: '', email: '', contrasena: '', rol: 'USUARIO', empresa: null };

/**
 * Gestion de usuarios: listar, crear, activar/desactivar y cambiar el rol.
 * El ADMIN ve a todos; el ADMIN_EMPRESA, a los de su empresa (lo filtra el backend).
 * Los selectores solo ofrecen lo que cada rol puede hacer; aun asi, la regla
 * real esta en el backend (PoliticaGestionUsuarios).
 *
 * El backend devuelve la lista completa: la busqueda, los chips y la
 * paginacion se resuelven en el navegador.
 */
@Component({
  selector: 'app-usuarios',
  imports: [DatePipe, FormsModule, Icono, Paginador, EstadoVacio, Skeleton],
  templateUrl: './usuarios.html',
  styleUrl: './usuarios.css'
})
export class Usuarios implements OnInit {

  protected auth = inject(AuthService);
  private servicio = inject(UsuariosService);
  private empresaService = inject(EmpresaService);
  private toast = inject(ToastService);
  private confirmacion = inject(ConfirmacionService);

  usuarios = signal<UsuarioAdmin[]>([]);
  cargando = signal(true);
  error = signal<string | null>(null);
  /** id del usuario cuyo cambio se esta guardando (para deshabilitar su boton). */
  guardando = signal<string | null>(null);

  mostrarFormulario = signal(false);
  nuevo: NuevoUsuario = { ...NUEVO_VACIO };
  creando = signal(false);
  mostrarContrasena = signal(false);
  /** Solo empresas activas: a una inactiva no se le pueden crear usuarios. */
  empresas = signal<Empresa[]>([]);

  // Filtros y paginacion en el navegador
  busqueda = signal('');
  filtroRol = signal<Rol | ''>('');
  filtroEstado = signal('');
  numeroPagina = signal(0);

  /** Un admin de empresa nunca asigna el rol ADMIN. */
  protected rolesAsignables = computed<Rol[]>(() =>
    this.auth.tieneRol('ADMIN') ? ['USUARIO', 'ADMIN_EMPRESA', 'ADMIN'] : ['USUARIO', 'ADMIN_EMPRESA']);

  protected filtrados = computed(() => {
    const texto = this.busqueda().trim().toLowerCase();
    return this.usuarios().filter((u) =>
      (!texto || u.nombreCompleto.toLowerCase().includes(texto) || u.email.toLowerCase().includes(texto)) &&
      (!this.filtroRol() || u.rol === this.filtroRol()) &&
      (!this.filtroEstado() || u.estado === this.filtroEstado()));
  });

  protected pagina = computed(() => paginarLocal(this.filtrados(), this.numeroPagina()));

  protected readonly estados = [
    { valor: '', nombre: 'Todos' },
    { valor: 'activo', nombre: 'Activos' },
    { valor: 'inactivo', nombre: 'Inactivos' },
    { valor: 'bloqueado', nombre: 'Bloqueados' },
  ];

  protected readonly nombreEstado = nombreEstadoUsuario;

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

  buscarTexto(texto: string): void {
    this.busqueda.set(texto);
    this.numeroPagina.set(0);
  }

  elegirRol(rol: Rol | ''): void {
    this.filtroRol.set(rol);
    this.numeroPagina.set(0);
  }

  elegirEstado(estado: string): void {
    this.filtroEstado.set(estado);
    this.numeroPagina.set(0);
  }

  abrirFormulario(): void {
    this.mostrarFormulario.set(true);
    this.nuevo = { ...NUEVO_VACIO };
    setTimeout(() => document.getElementById('nuevo-nombres')?.focus());
    // La lista de empresas (/api/empresas) es solo para el ADMIN; el admin
    // de empresa no elige: el usuario va a su propia empresa.
    if (this.auth.tieneRol('ADMIN') && this.empresas().length === 0) {
      this.empresaService.listar().subscribe({
        next: (lista) => this.empresas.set(lista.filter((e) => e.activa)),
        error: (err) => this.toast.error(mensajeDeError(err, 'No se pudieron cargar las empresas.'))
      });
    }
  }

  crear(): void {
    // Un administrador de la plataforma no pertenece a una empresa
    const datos: NuevoUsuario = { ...this.nuevo, empresa: this.nuevo.rol === 'ADMIN' ? null : this.nuevo.empresa || null };
    this.creando.set(true);
    this.servicio.crear(datos).subscribe({
      next: (creado) => {
        this.usuarios.update((lista) =>
          [...lista, creado].sort((a, b) => a.nombreCompleto.localeCompare(b.nombreCompleto)));
        this.toast.exito(`Se creó la cuenta de ${creado.email}. Comparta la contraseña inicial por un canal seguro.`);
        this.mostrarFormulario.set(false);
        this.creando.set(false);
      },
      error: (err) => {
        this.toast.error(mensajeDeError(err, 'No se pudo crear el usuario.'));
        this.creando.set(false);
      }
    });
  }

  /** Nadie puede desactivarse ni cambiarse el rol a si mismo (el backend tambien lo impide). */
  esUnoMismo(u: UsuarioAdmin): boolean {
    return u.id === this.auth.usuario()?.id;
  }

  /** Desactivar quita el acceso: se confirma antes. Activar va directo. */
  alternarEstado(u: UsuarioAdmin): void {
    if (u.estado !== 'activo') {
      this.cambiarEstado(u);
      return;
    }
    this.confirmacion.pedir({
      titulo: `¿Desactivar a ${u.nombreCompleto}?`,
      mensaje: `${u.email} ya no podrá iniciar sesión. Puede volver a activarlo cuando quiera.`,
      confirmar: 'Desactivar',
      peligro: true,
    }).pipe(filter(Boolean)).subscribe(() => this.cambiarEstado(u));
  }

  /** El cambio de rol se confirma; si se cancela, el selector vuelve al rol actual. */
  pedirCambioRol(u: UsuarioAdmin, selector: HTMLSelectElement): void {
    const rol = selector.value as Rol;
    if (rol === u.rol) {
      return;
    }
    this.confirmacion.pedir({
      titulo: '¿Cambiar el rol?',
      mensaje: `${u.email} pasará de ${this.nombreRol(u.rol)} a ${this.nombreRol(rol)}. Los permisos cambian en su próxima acción.`,
      confirmar: 'Cambiar rol',
      peligro: rol === 'ADMIN' || u.rol === 'ADMIN',
    }).subscribe((ok) => {
      if (ok) {
        this.cambiarRol(u, rol, selector);
      } else {
        selector.value = u.rol;
      }
    });
  }

  cambiarRol(u: UsuarioAdmin, rol: Rol, selector?: HTMLSelectElement): void {
    if (rol === u.rol) {
      return;
    }
    this.guardando.set(u.id);
    this.servicio.cambiarRol(u.id, rol).subscribe({
      next: (actualizado) => {
        this.reemplazar(actualizado);
        this.toast.exito(`${actualizado.email} ahora es ${this.nombreRol(actualizado.rol)}.`);
      },
      error: (err) => {
        this.toast.error(mensajeDeError(err, 'No se pudo cambiar el rol.'));
        this.guardando.set(null);
        // El selector vuelve a mostrar el rol real
        if (selector) {
          selector.value = u.rol;
        }
      }
    });
  }

  private cambiarEstado(u: UsuarioAdmin): void {
    const activar = u.estado !== 'activo';
    this.guardando.set(u.id);
    this.servicio.cambiarEstado(u.id, activar).subscribe({
      next: (actualizado) => {
        this.reemplazar(actualizado);
        this.toast.exito(activar ? `${actualizado.email} se activó.` : `${actualizado.email} se desactivó.`);
      },
      error: (err) => {
        this.toast.error(mensajeDeError(err, 'No se pudo cambiar el estado.'));
        this.guardando.set(null);
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

  protected iniciales(nombre: string): string {
    const partes = nombre.trim().split(/\s+/).filter(Boolean);
    return ((partes[0]?.[0] ?? '') + (partes[1]?.[0] ?? '')).toUpperCase() || '?';
  }
}

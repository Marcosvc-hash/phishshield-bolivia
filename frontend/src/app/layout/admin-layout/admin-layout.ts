import { Component, ElementRef, OnInit, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { Rol } from '../../core/auth/auth.model';
import { AuthService } from '../../core/auth/auth.service';
import { Icono, NombreIcono } from '../../core/ui/icono';
import { Marca } from '../../core/ui/marca';
import { AlertasService } from '../../paginas/admin/alertas/alertas.service';

interface OpcionMenu {
  etiqueta: string;
  ruta: string;
  icono: NombreIcono;
  roles: Rol[];
  /** Muestra junto a la etiqueta el numero de alertas pendientes. */
  conPendientes?: boolean;
}

interface GrupoMenu {
  titulo: string;
  opciones: OpcionMenu[];
}

/**
 * Marco de las paginas de administracion: menu lateral (que en celular se
 * abre y cierra con el boton de hamburguesa), barra superior con el titulo
 * de la pagina y el menu del usuario, y la pagina elegida en el <router-outlet>.
 */
@Component({
  selector: 'app-admin-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Icono, Marca],
  templateUrl: './admin-layout.html',
  styleUrl: './admin-layout.css',
  host: {
    '(document:keydown.escape)': 'cerrarTodo()',
    '(document:click)': 'clicFuera($event)',
  }
})
export class AdminLayout implements OnInit {

  protected auth = inject(AuthService);
  protected alertas = inject(AlertasService);
  private router = inject(Router);
  private elemento = inject(ElementRef<HTMLElement>);

  private static readonly MENU: GrupoMenu[] = [
    {
      titulo: 'Monitoreo',
      opciones: [
        { etiqueta: 'Panel', ruta: '/admin/panel', icono: 'panel', roles: ['ADMIN', 'ADMIN_EMPRESA'] },
        { etiqueta: 'Historial de análisis', ruta: '/admin/historial', icono: 'historial', roles: ['ADMIN', 'ADMIN_EMPRESA'] },
        { etiqueta: 'Alertas', ruta: '/admin/alertas', icono: 'campana', roles: ['ADMIN', 'ADMIN_EMPRESA'], conPendientes: true },
      ]
    },
    {
      titulo: 'Gestión',
      opciones: [
        { etiqueta: 'Usuarios', ruta: '/admin/usuarios', icono: 'usuarios', roles: ['ADMIN', 'ADMIN_EMPRESA'] },
        { etiqueta: 'Empresas', ruta: '/admin/empresas', icono: 'empresa', roles: ['ADMIN'] },
      ]
    },
    {
      titulo: 'Plataforma',
      opciones: [
        { etiqueta: 'Listas de dominios', ruta: '/admin/dominios', icono: 'lista', roles: ['ADMIN'] },
        { etiqueta: 'Registro de accesos', ruta: '/admin/auditoria', icono: 'registro', roles: ['ADMIN'] },
      ]
    },
  ];

  /** Cada rol ve solo sus opciones (y no ve grupos vacios); se recalcula si cambia el usuario. */
  protected grupos = computed(() => {
    this.auth.usuario();
    return AdminLayout.MENU
      .map((g) => ({ ...g, opciones: g.opciones.filter((o) => this.auth.tieneRol(...o.roles)) }))
      .filter((g) => g.opciones.length > 0);
  });

  /** En celular el menu lateral esta oculto hasta que se abre. */
  protected menuAbierto = signal(false);
  protected menuUsuario = signal(false);

  /** Titulo de la pagina actual, tomado del "title" de su ruta. */
  protected titulo = toSignal(
    this.router.events.pipe(filter((e) => e instanceof NavigationEnd), map(() => this.tituloActual())),
    { initialValue: this.tituloActual() });

  protected iniciales = computed(() => {
    const partes = (this.auth.usuario()?.nombreCompleto ?? '').trim().split(/\s+/).filter(Boolean);
    return ((partes[0]?.[0] ?? '') + (partes[1]?.[0] ?? '')).toUpperCase() || '?';
  });

  constructor() {
    // Al navegar se cierran el menu lateral (celular) y el del usuario
    this.router.events.pipe(filter((e) => e instanceof NavigationEnd)).subscribe(() => this.cerrarTodo());
  }

  ngOnInit(): void {
    if (this.auth.tieneRol('ADMIN', 'ADMIN_EMPRESA')) {
      this.alertas.refrescarPendientes();
    }
  }

  protected nombreRol(rol: Rol): string {
    switch (rol) {
      case 'ADMIN': return 'Administrador';
      case 'ADMIN_EMPRESA': return 'Administrador de empresa';
      default: return 'Usuario';
    }
  }

  protected cerrarTodo(): void {
    this.menuAbierto.set(false);
    this.menuUsuario.set(false);
  }

  /** Un clic fuera del menu del usuario lo cierra. */
  protected clicFuera(evento: Event): void {
    const zona = (this.elemento.nativeElement as HTMLElement).querySelector('.usuario');
    if (this.menuUsuario() && zona && !zona.contains(evento.target as Node)) {
      this.menuUsuario.set(false);
    }
  }

  private tituloActual(): string {
    let ruta = this.router.routerState.snapshot.root;
    while (ruta.firstChild) {
      ruta = ruta.firstChild;
    }
    return ruta.title ?? 'Administración';
  }
}

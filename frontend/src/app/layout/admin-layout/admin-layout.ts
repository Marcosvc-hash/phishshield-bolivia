import { Component, OnInit, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Rol } from '../../core/auth/auth.model';
import { AuthService } from '../../core/auth/auth.service';
import { AlertasService } from '../../paginas/admin/alertas/alertas.service';

interface OpcionMenu {
  etiqueta: string;
  ruta: string | null;   // null = todavia no construida
  roles: Rol[];
  /** Muestra junto a la etiqueta el numero de alertas pendientes. */
  conPendientes?: boolean;
}

/**
 * Marco de las paginas de administracion: menu lateral a la izquierda y
 * la pagina elegida a la derecha (en el <router-outlet>).
 */
@Component({
  selector: 'app-admin-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './admin-layout.html',
  styleUrl: './admin-layout.css'
})
export class AdminLayout implements OnInit {

  protected auth = inject(AuthService);
  protected alertas = inject(AlertasService);

  private static readonly MENU: OpcionMenu[] = [
    { etiqueta: 'Panel', ruta: '/admin/panel', roles: ['ADMIN', 'ADMIN_EMPRESA'] },
    { etiqueta: 'Usuarios', ruta: '/admin/usuarios', roles: ['ADMIN', 'ADMIN_EMPRESA'] },
    { etiqueta: 'Empresas', ruta: '/admin/empresas', roles: ['ADMIN'] },
    { etiqueta: 'Historial de analisis', ruta: '/admin/historial', roles: ['ADMIN', 'ADMIN_EMPRESA'] },
    { etiqueta: 'Alertas', ruta: '/admin/alertas', roles: ['ADMIN', 'ADMIN_EMPRESA'], conPendientes: true },
    { etiqueta: 'Listas de dominios', ruta: null, roles: ['ADMIN'] },
    { etiqueta: 'Registro de accesos', ruta: '/admin/auditoria', roles: ['ADMIN'] },
  ];

  /** Cada rol ve solo sus opciones; se recalcula si cambia el usuario. */
  protected opciones = computed(() => {
    this.auth.usuario();
    return AdminLayout.MENU.filter((o) => this.auth.tieneRol(...o.roles));
  });

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
}

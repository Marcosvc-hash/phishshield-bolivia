import { Routes } from '@angular/router';
import { authGuard, rolGuard } from './core/auth/auth.guard';
import { Inicio } from './paginas/inicio/inicio';
import { Login } from './paginas/login/login';
import { Registro } from './paginas/registro/registro';
import { AdminLayout } from './layout/admin-layout/admin-layout';

export const routes: Routes = [
  { path: '', component: Inicio },
  { path: 'login', component: Login, title: 'Iniciar sesión' },
  { path: 'registro', component: Registro, title: 'Crear cuenta' },
  {
    // Todo lo de /admin exige sesion; cada pagina, ademas, un rol
    path: 'admin',
    component: AdminLayout,
    canActivate: [authGuard],
    // Carga diferida: el codigo de estas paginas (y chart.js) solo se
    // descarga cuando alguien entra; quien solo usa el analizador no lo baja
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'panel' },
      {
        path: 'panel',
        title: 'Panel',
        canActivate: [rolGuard('ADMIN', 'ADMIN_EMPRESA')],
        loadComponent: () => import('./paginas/admin/panel/panel').then((m) => m.Panel)
      },
      {
        path: 'usuarios',
        title: 'Usuarios',
        canActivate: [rolGuard('ADMIN', 'ADMIN_EMPRESA')],
        loadComponent: () => import('./paginas/admin/usuarios/usuarios').then((m) => m.Usuarios)
      },
      {
        path: 'empresas',
        title: 'Empresas',
        canActivate: [rolGuard('ADMIN')],
        loadComponent: () => import('./paginas/admin/empresas/empresas').then((m) => m.Empresas)
      },
      {
        path: 'historial',
        title: 'Historial de análisis',
        canActivate: [rolGuard('ADMIN', 'ADMIN_EMPRESA')],
        loadComponent: () => import('./paginas/admin/historial/historial').then((m) => m.Historial)
      },
      {
        path: 'alertas',
        title: 'Alertas',
        canActivate: [rolGuard('ADMIN', 'ADMIN_EMPRESA')],
        loadComponent: () => import('./paginas/admin/alertas/alertas').then((m) => m.Alertas)
      },
      {
        path: 'dominios',
        title: 'Listas de dominios',
        canActivate: [rolGuard('ADMIN')],
        loadComponent: () => import('./paginas/admin/dominios/dominios').then((m) => m.Dominios)
      },
      {
        path: 'auditoria',
        title: 'Registro de accesos',
        canActivate: [rolGuard('ADMIN')],
        loadComponent: () => import('./paginas/admin/auditoria/auditoria').then((m) => m.Auditoria)
      },
    ]
  },
  { path: '**', redirectTo: '' }
];

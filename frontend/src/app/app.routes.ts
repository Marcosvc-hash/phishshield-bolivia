import { Routes } from '@angular/router';
import { authGuard, rolGuard } from './core/auth/auth.guard';
import { Inicio } from './paginas/inicio/inicio';
import { Login } from './paginas/login/login';
import { Registro } from './paginas/registro/registro';
import { AdminLayout } from './layout/admin-layout/admin-layout';
import { Panel } from './paginas/admin/panel/panel';
import { Empresas } from './paginas/admin/empresas/empresas';

export const routes: Routes = [
  { path: '', component: Inicio },
  { path: 'login', component: Login },
  { path: 'registro', component: Registro },
  {
    // Todo lo de /admin exige sesion; cada pagina, ademas, un rol
    path: 'admin',
    component: AdminLayout,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'panel' },
      { path: 'panel', component: Panel, canActivate: [rolGuard('ADMIN', 'ADMIN_EMPRESA')] },
      { path: 'empresas', component: Empresas, canActivate: [rolGuard('ADMIN')] },
    ]
  },
  { path: '**', redirectTo: '' }
];

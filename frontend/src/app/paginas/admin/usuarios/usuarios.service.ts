import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../../../core/api';
import { Rol } from '../../../core/auth/auth.model';

/** Fila de GET /api/admin/usuarios (UsuarioAdminResponse en el backend). */
export interface UsuarioAdmin {
  id: string;
  nombreCompleto: string;
  email: string;
  rol: Rol;
  estado: 'activo' | 'inactivo' | 'bloqueado' | 'pendiente';
  empresa: string | null;
  emailVerificado: boolean;
  intentosFallidos: number;
  ultimoLogin: string | null;
  creadoEn: string;
}

/** Cuerpo de POST /api/admin/usuarios (CrearUsuarioRequest). */
export interface NuevoUsuario {
  nombres: string;
  apellidos: string;
  email: string;
  contrasena: string;
  rol: Rol;
  /** id de la empresa; null = sin empresa (o la propia, para un admin de empresa) */
  empresa: string | null;
}

@Injectable({ providedIn: 'root' })
export class UsuariosService {

  private http = inject(HttpClient);
  private readonly url = `${API_URL}/admin/usuarios`;

  listar(): Observable<UsuarioAdmin[]> {
    return this.http.get<UsuarioAdmin[]>(this.url);
  }

  crear(datos: NuevoUsuario): Observable<UsuarioAdmin> {
    return this.http.post<UsuarioAdmin>(this.url, datos);
  }

  cambiarEstado(id: string, activo: boolean): Observable<UsuarioAdmin> {
    return this.http.patch<UsuarioAdmin>(`${this.url}/${id}/estado`, { activo });
  }

  cambiarRol(id: string, rol: Rol): Observable<UsuarioAdmin> {
    return this.http.patch<UsuarioAdmin>(`${this.url}/${id}/rol`, { rol });
  }
}

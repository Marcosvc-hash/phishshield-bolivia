import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, finalize, map, shareReplay, tap, throwError } from 'rxjs';
import { API_URL } from '../api';
import { RegistroRequest, Rol, TokenResponse, UsuarioSesion } from './auth.model';

const CLAVE_ACCESO = 'phishshield.tokenAcceso';
const CLAVE_REFRESCO = 'phishshield.tokenRefresco';
const CLAVE_USUARIO = 'phishshield.usuario';

/**
 * Guarda la sesion en localStorage para que sobreviva a un F5.
 *
 * El rol guardado aca solo sirve para mostrar u ocultar menus: si alguien lo
 * edita a mano en el navegador, el backend igual le responde 403, porque
 * vuelve a leer el rol de la base en cada peticion.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {

  private http = inject(HttpClient);
  private router = inject(Router);
  private readonly url = `${API_URL}/auth`;

  private readonly usuarioActual = signal<UsuarioSesion | null>(leerUsuarioGuardado());
  readonly usuario = this.usuarioActual.asReadonly();
  readonly autenticado = computed(() => this.usuarioActual() !== null);

  /** Si varias peticiones reciben 401 a la vez, todas esperan el MISMO refresco. */
  private refrescoEnCurso: Observable<TokenResponse> | null = null;

  login(email: string, contrasena: string): Observable<UsuarioSesion> {
    return this.http.post<TokenResponse>(`${this.url}/login`, { email, contrasena }).pipe(
      tap((respuesta) => this.guardar(respuesta)),
      map((respuesta) => respuesta.usuario));
  }

  registrar(datos: RegistroRequest): Observable<UsuarioSesion> {
    return this.http.post<UsuarioSesion>(`${this.url}/registro`, datos);
  }

  /**
   * Cambia el token de refresco por un token de acceso nuevo. El backend
   * rota el de refresco, por eso no deben salir dos refrescos con el mismo.
   */
  refrescar(): Observable<TokenResponse> {
    const tokenRefresco = localStorage.getItem(CLAVE_REFRESCO);
    if (!tokenRefresco) {
      return throwError(() => new Error('No hay token de refresco'));
    }
    if (!this.refrescoEnCurso) {
      this.refrescoEnCurso = this.http
        .post<TokenResponse>(`${this.url}/refresh`, { tokenRefresco })
        .pipe(
          tap((respuesta) => this.guardar(respuesta)),
          finalize(() => (this.refrescoEnCurso = null)),
          shareReplay(1));
    }
    return this.refrescoEnCurso;
  }

  /** Revoca la sesion en el backend y, pase lo que pase, la borra del navegador. */
  logout(): void {
    const terminar = () => {
      this.limpiar();
      this.router.navigate(['/login']);
    };
    if (!this.tokenAcceso()) {
      terminar();
      return;
    }
    this.http
      .post<void>(`${this.url}/logout`, { tokenRefresco: localStorage.getItem(CLAVE_REFRESCO) })
      .subscribe({ complete: terminar, error: terminar });
  }

  tokenAcceso(): string | null {
    return localStorage.getItem(CLAVE_ACCESO);
  }

  tieneRol(...roles: Rol[]): boolean {
    const usuario = this.usuarioActual();
    return usuario !== null && roles.includes(usuario.rol);
  }

  /** Pagina a la que va cada rol despues del login. */
  inicioSegunRol(): string {
    return this.tieneRol('ADMIN', 'ADMIN_EMPRESA') ? '/admin' : '/';
  }

  limpiar(): void {
    localStorage.removeItem(CLAVE_ACCESO);
    localStorage.removeItem(CLAVE_REFRESCO);
    localStorage.removeItem(CLAVE_USUARIO);
    this.usuarioActual.set(null);
  }

  private guardar(respuesta: TokenResponse): void {
    localStorage.setItem(CLAVE_ACCESO, respuesta.tokenAcceso);
    localStorage.setItem(CLAVE_REFRESCO, respuesta.tokenRefresco);
    localStorage.setItem(CLAVE_USUARIO, JSON.stringify(respuesta.usuario));
    this.usuarioActual.set(respuesta.usuario);
  }
}

function leerUsuarioGuardado(): UsuarioSesion | null {
  try {
    const texto = localStorage.getItem(CLAVE_USUARIO);
    return texto && localStorage.getItem(CLAVE_ACCESO) ? (JSON.parse(texto) as UsuarioSesion) : null;
  } catch {
    return null;
  }
}

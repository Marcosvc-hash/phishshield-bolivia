export type Rol = 'ADMIN' | 'ADMIN_EMPRESA' | 'USUARIO';

/** Lo que el backend devuelve como "usuario" en el login y en /auth/yo. */
export interface UsuarioSesion {
  id: string;
  nombreCompleto: string;
  email: string;
  rol: Rol;
}

export interface TokenResponse {
  tokenAcceso: string;
  tipo: string;
  accesoExpiraEn: string;
  tokenRefresco: string;
  refrescoExpiraEn: string;
  usuario: UsuarioSesion;
}

export interface RegistroRequest {
  nombres: string;
  apellidos?: string;
  email: string;
  contrasena: string;
}

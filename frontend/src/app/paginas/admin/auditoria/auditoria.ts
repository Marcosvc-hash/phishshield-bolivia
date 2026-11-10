import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { mensajeDeError } from '../../../core/errores';
import { Pagina } from '../../../core/pagina';
import { EstadoVacio } from '../../../core/ui/estado-vacio';
import { capitalizar, fechaLegible } from '../../../core/ui/etiquetas';
import { Icono } from '../../../core/ui/icono';
import { Paginador } from '../../../core/ui/paginador';
import { Skeleton } from '../../../core/ui/skeleton';
import { AuditoriaService, FiltroAuditoria, RegistroAuditoria } from './auditoria.service';

const SIN_FILTROS: FiltroAuditoria = { accion: '', usuario: '', desde: '', hasta: '' };

/** Nombre legible de cada accion de la tabla auditoria (AccionAuditoria y TipoEventoAcceso). */
const NOMBRES_ACCION: Record<string, string> = {
  LOGIN_EXITOSO: 'Inicio de sesión',
  LOGIN_FALLIDO: 'Inicio de sesión fallido',
  LOGOUT: 'Cierre de sesión',
  USUARIO_CREADO: 'Usuario creado',
  USUARIO_ACTIVADO: 'Usuario activado',
  USUARIO_DESACTIVADO: 'Usuario desactivado',
  USUARIO_ROL_CAMBIADO: 'Rol cambiado',
  ALERTA_ACTUALIZADA: 'Alerta actualizada',
  DOMINIO_AGREGADO: 'Dominio agregado',
  DOMINIO_EDITADO: 'Dominio editado',
  DOMINIO_DESACTIVADO: 'Dominio desactivado',
  DOMINIO_REACTIVADO: 'Dominio reactivado',
  EMPRESA_CREADA: 'Empresa creada',
  EMPRESA_EDITADA: 'Empresa editada',
  EMPRESA_ACTIVADA: 'Empresa activada',
  EMPRESA_DESACTIVADA: 'Empresa desactivada',
};

export function nombreAccion(accion: string): string {
  return Object.hasOwn(NOMBRES_ACCION, accion)
    ? NOMBRES_ACCION[accion]
    : capitalizar(accion.toLowerCase().replaceAll('_', ' '));
}

/**
 * Registro de accesos (logins, logouts) y de acciones administrativas.
 * Solo para el ADMIN.
 */
@Component({
  selector: 'app-auditoria',
  imports: [FormsModule, DatePipe, Paginador, Icono, EstadoVacio, Skeleton],
  templateUrl: './auditoria.html',
  styleUrl: './auditoria.css'
})
export class Auditoria implements OnInit {

  private servicio = inject(AuditoriaService);

  filtro: FiltroAuditoria = { ...SIN_FILTROS };
  protected aplicado: FiltroAuditoria = { ...SIN_FILTROS };
  acciones = signal<string[]>([]);
  pagina = signal<Pagina<RegistroAuditoria> | null>(null);
  cargando = signal(true);
  error = signal<string | null>(null);

  /** Atajos para lo que mas se consulta: los accesos. */
  protected readonly rapidas = ['', 'LOGIN_EXITOSO', 'LOGIN_FALLIDO', 'LOGOUT'];

  protected readonly nombreAccion = nombreAccion;
  protected readonly fechaLegible = fechaLegible;

  ngOnInit(): void {
    this.servicio.acciones().subscribe({
      next: (lista) => this.acciones.set(lista),
      // Sin la lista el filtro igual sirve; el error de la tabla ya avisa
      error: () => this.acciones.set([])
    });
    this.cargar(0);
  }

  buscar(): void {
    this.cargar(0);
  }

  limpiar(): void {
    this.filtro = { ...SIN_FILTROS };
    this.cargar(0);
  }

  elegirAccion(accion: string): void {
    this.filtro.accion = accion;
    this.cargar(0);
  }

  quitar(campo: 'usuario' | 'desde' | 'hasta'): void {
    this.filtro[campo] = '';
    this.cargar(0);
  }

  protected hayFiltros(): boolean {
    return Object.values(this.aplicado).some((v) => v !== '');
  }

  cargar(numero: number): void {
    this.cargando.set(true);
    this.error.set(null);
    this.aplicado = { ...this.filtro };
    this.servicio.listar(this.filtro, numero).subscribe({
      next: (pagina) => {
        this.pagina.set(pagina);
        this.cargando.set(false);
      },
      error: (err) => {
        this.error.set(mensajeDeError(err, 'No se pudo cargar el registro.'));
        this.cargando.set(false);
      }
    });
  }

  /** El JSON del detalle como pares "clave: valor" faciles de leer. */
  pares(detalle: Record<string, unknown>): { clave: string; valor: string }[] {
    return Object.entries(detalle ?? {}).map(([clave, valor]) => ({
      clave: clave.replaceAll('_', ' '),
      valor: valor === null || valor === undefined ? '-' : typeof valor === 'object' ? JSON.stringify(valor) : String(valor)
    }));
  }

  /** LOGIN_FALLIDO y las desactivaciones se resaltan para verlas de un vistazo. */
  esDeAtencion(accion: string): boolean {
    return accion === 'LOGIN_FALLIDO' || accion.endsWith('_DESACTIVADO') || accion.endsWith('_DESACTIVADA');
  }
}

import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { mensajeDeError } from '../../../core/errores';
import { Pagina } from '../../../core/pagina';
import { Paginador } from '../../../core/ui/paginador';
import { AuditoriaService, FiltroAuditoria, RegistroAuditoria } from './auditoria.service';

/**
 * Registro de accesos (logins, logouts) y de acciones administrativas.
 * Solo para el ADMIN.
 */
@Component({
  selector: 'app-auditoria',
  imports: [FormsModule, DatePipe, Paginador],
  templateUrl: './auditoria.html',
  styleUrl: './auditoria.css'
})
export class Auditoria implements OnInit {

  private servicio = inject(AuditoriaService);

  filtro: FiltroAuditoria = { accion: '', usuario: '', desde: '', hasta: '' };
  acciones = signal<string[]>([]);
  pagina = signal<Pagina<RegistroAuditoria> | null>(null);
  cargando = signal(true);
  error = signal<string | null>(null);

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
    this.filtro = { accion: '', usuario: '', desde: '', hasta: '' };
    this.cargar(0);
  }

  cargar(numero: number): void {
    this.cargando.set(true);
    this.error.set(null);
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

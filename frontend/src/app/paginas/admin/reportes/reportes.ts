import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/auth/auth.service';
import { mensajeDeError } from '../../../core/errores';
import { Pagina } from '../../../core/pagina';
import { EstadoVacio } from '../../../core/ui/estado-vacio';
import { fechaLegible } from '../../../core/ui/etiquetas';
import { Icono } from '../../../core/ui/icono';
import { Paginador } from '../../../core/ui/paginador';
import { Skeleton } from '../../../core/ui/skeleton';
import { ToastService } from '../../../core/ui/toast.service';
import { Empresa } from '../../../empresa.model';
import { EmpresaService } from '../../../empresa.service';
import { PedidoReporte, ReporteEmpresa, ReportesService } from './reportes.service';

/** "2026-11-24" en hora local, como lo usa <input type="date">. */
export function fechaIso(fecha: Date): string {
  const mes = String(fecha.getMonth() + 1).padStart(2, '0');
  const dia = String(fecha.getDate()).padStart(2, '0');
  return `${fecha.getFullYear()}-${mes}-${dia}`;
}

/**
 * Reportes por empresa (HU-17): generar el de un período (por defecto, los
 * últimos 30 días), ver los ya generados y descargarlos en PDF. El ADMIN
 * elige la empresa; el administrador de empresa solo trabaja con la suya.
 */
@Component({
  selector: 'app-reportes',
  imports: [FormsModule, DatePipe, DecimalPipe, Icono, EstadoVacio, Skeleton, Paginador],
  templateUrl: './reportes.html',
})
export class Reportes implements OnInit {

  protected auth = inject(AuthService);
  private servicio = inject(ReportesService);
  private empresasServicio = inject(EmpresaService);
  private toast = inject(ToastService);

  empresas = signal<Empresa[]>([]);
  idEmpresa = '';
  desde = '';
  hasta = '';
  hoy = fechaIso(new Date());

  pagina = signal<Pagina<ReporteEmpresa> | null>(null);
  cargando = signal(true);
  error = signal<string | null>(null);
  generando = signal(false);
  ultimo = signal<ReporteEmpresa | null>(null);
  descargando = signal<string | null>(null);

  protected readonly fechaLegible = fechaLegible;

  ngOnInit(): void {
    const hace30 = new Date();
    hace30.setDate(hace30.getDate() - 29);
    this.desde = fechaIso(hace30);
    this.hasta = this.hoy;
    if (this.esAdmin()) {
      this.empresasServicio.listar().subscribe({
        next: (lista) => this.empresas.set(lista),
        error: (err) => this.toast.error(mensajeDeError(err, 'No se pudieron cargar las empresas.'))
      });
    }
    this.cargar(0);
  }

  esAdmin(): boolean {
    return this.auth.tieneRol('ADMIN');
  }

  cargar(numero: number): void {
    this.cargando.set(true);
    this.error.set(null);
    this.servicio.listar(this.esAdmin() ? this.idEmpresa : '', numero).subscribe({
      next: (p) => {
        this.pagina.set(p);
        this.cargando.set(false);
      },
      error: (err) => {
        this.error.set(mensajeDeError(err, 'No se pudieron cargar los reportes.'));
        this.cargando.set(false);
      }
    });
  }

  /** Al cambiar la empresa elegida, la lista muestra solo sus reportes. */
  elegirEmpresa(): void {
    this.ultimo.set(null);
    this.cargar(0);
  }

  generar(): void {
    if (this.esAdmin() && !this.idEmpresa) {
      this.toast.error('Elija la empresa del reporte.');
      return;
    }
    if (!this.desde || !this.hasta) {
      this.toast.error('Indique las fechas del período.');
      return;
    }
    const pedido: PedidoReporte = { desde: this.desde, hasta: this.hasta, ...(this.esAdmin() ? { idEmpresa: this.idEmpresa } : {}) };
    this.generando.set(true);
    this.servicio.generar(pedido).subscribe({
      next: (reporte) => {
        this.generando.set(false);
        this.ultimo.set(reporte);
        this.toast.exito(`Reporte de ${reporte.empresa} generado.`);
        this.cargar(0);
      },
      error: (err) => {
        this.generando.set(false);
        this.toast.error(mensajeDeError(err, 'No se pudo generar el reporte.'));
      }
    });
  }

  descargar(r: ReporteEmpresa): void {
    this.descargando.set(r.id);
    this.servicio.pdf(r.id).subscribe({
      next: (archivo) => {
        this.descargando.set(null);
        Reportes.guardarArchivo(archivo, `reporte-${r.empresa.toLowerCase().replace(/[^a-z0-9]+/g, '-')}-${r.desde}-al-${r.hasta}.pdf`);
      },
      error: (err) => {
        this.descargando.set(null);
        this.toast.error(mensajeDeError(err, 'No se pudo descargar el PDF.'));
      }
    });
  }

  /** Descarga un Blob con un enlace temporal (el PDF exige el token, no sirve un href directo). */
  static guardarArchivo(archivo: Blob, nombre: string): void {
    if (typeof URL.createObjectURL !== 'function') {
      return;
    }
    const enlace = document.createElement('a');
    enlace.href = URL.createObjectURL(archivo);
    enlace.download = nombre;
    enlace.click();
    setTimeout(() => URL.revokeObjectURL(enlace.href), 1000);
  }
}

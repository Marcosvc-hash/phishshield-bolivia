import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/auth/auth.service';
import { mensajeDeError } from '../../../core/errores';
import { Pagina } from '../../../core/pagina';
import { Paginador } from '../../../core/ui/paginador';
import { AnalisisFila, DetalleAnalisis, FiltroHistorial, HistorialService } from './historial.service';

/**
 * Historial de analisis: filtros, tabla paginada en el servidor y, al hacer
 * clic en una fila, el detalle con cada indicador y su aporte al puntaje.
 */
@Component({
  selector: 'app-historial',
  imports: [FormsModule, DatePipe, DecimalPipe, Paginador],
  templateUrl: './historial.html',
  styleUrl: './historial.css'
})
export class Historial implements OnInit {

  protected auth = inject(AuthService);
  private servicio = inject(HistorialService);

  /** Los campos del formulario; se aplican al pulsar "Buscar". */
  filtro: FiltroHistorial = { resultado: '', desde: '', hasta: '', q: '' };

  pagina = signal<Pagina<AnalisisFila> | null>(null);
  cargando = signal(true);
  error = signal<string | null>(null);

  detalle = signal<DetalleAnalisis | null>(null);
  cargandoDetalle = signal(false);

  ngOnInit(): void {
    this.cargar(0);
  }

  buscar(): void {
    this.detalle.set(null);
    this.cargar(0);
  }

  limpiar(): void {
    this.filtro = { resultado: '', desde: '', hasta: '', q: '' };
    this.buscar();
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
        this.error.set(mensajeDeError(err, 'No se pudo cargar el historial.'));
        this.cargando.set(false);
      }
    });
  }

  abrir(fila: AnalisisFila): void {
    this.cargandoDetalle.set(true);
    this.error.set(null);
    this.servicio.detalle(fila.id).subscribe({
      next: (d) => {
        this.detalle.set(d);
        this.cargandoDetalle.set(false);
      },
      error: (err) => {
        this.error.set(mensajeDeError(err, 'No se pudo abrir el analisis.'));
        this.cargandoDetalle.set(false);
      }
    });
  }

  /** Suma de los aportes: debe coincidir con el nivel de riesgo (tope 100). */
  sumaAportes(d: DetalleAnalisis): number {
    return d.indicadores.reduce((total, i) => total + Number(i.aporte), 0);
  }
}

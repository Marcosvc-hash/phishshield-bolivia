import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { explicar } from '../../../analizador/explicaciones';
import { AuthService } from '../../../core/auth/auth.service';
import { mensajeDeError } from '../../../core/errores';
import { Pagina } from '../../../core/pagina';
import { mostrarDetalleEnPantallaAngosta } from '../../../core/ui/desplazar';
import { EstadoVacio } from '../../../core/ui/estado-vacio';
import { fechaLegible, nombreEstadoAlerta, nombreSeveridad, nombreVeredicto } from '../../../core/ui/etiquetas';
import { Icono } from '../../../core/ui/icono';
import { Paginador } from '../../../core/ui/paginador';
import { Skeleton } from '../../../core/ui/skeleton';
import { ToastService } from '../../../core/ui/toast.service';
import { AnalisisFila, DetalleAnalisis, FiltroHistorial, HistorialService } from './historial.service';

const SIN_FILTROS: FiltroHistorial = { resultado: '', desde: '', hasta: '', q: '' };

/**
 * Historial de analisis: filtros, tabla paginada en el servidor y, al hacer
 * clic en una fila, el detalle con cada indicador y su aporte al puntaje.
 */
@Component({
  selector: 'app-historial',
  imports: [FormsModule, DatePipe, DecimalPipe, Paginador, Icono, EstadoVacio, Skeleton],
  templateUrl: './historial.html',
  styleUrl: './historial.css'
})
export class Historial implements OnInit {

  protected auth = inject(AuthService);
  private servicio = inject(HistorialService);
  private toast = inject(ToastService);

  /** Los campos del formulario; se aplican al pulsar "Buscar". */
  filtro: FiltroHistorial = { ...SIN_FILTROS };
  /** Los filtros con los que se pidio la pagina que se ve (para los chips). */
  protected aplicado: FiltroHistorial = { ...SIN_FILTROS };

  pagina = signal<Pagina<AnalisisFila> | null>(null);
  cargando = signal(true);
  error = signal<string | null>(null);

  detalle = signal<DetalleAnalisis | null>(null);
  cargandoDetalle = signal(false);

  protected readonly resultados = [
    { valor: '', nombre: 'Todos' },
    { valor: 'phishing', nombre: 'Phishing' },
    { valor: 'sospechoso', nombre: 'Sospechoso' },
    { valor: 'seguro', nombre: 'Seguro' },
  ];

  protected readonly explicar = explicar;
  protected readonly nombreVeredicto = nombreVeredicto;
  protected readonly nombreEstadoAlerta = nombreEstadoAlerta;
  protected readonly nombreSeveridad = nombreSeveridad;
  protected readonly fechaLegible = fechaLegible;

  ngOnInit(): void {
    this.cargar(0);
  }

  buscar(): void {
    this.detalle.set(null);
    this.cargar(0);
  }

  limpiar(): void {
    this.filtro = { ...SIN_FILTROS };
    this.buscar();
  }

  /** Los chips de resultado se aplican al instante. */
  elegirResultado(valor: string): void {
    this.filtro.resultado = valor;
    this.buscar();
  }

  /** Quita un filtro de texto o fecha desde su chip. */
  quitar(campo: 'q' | 'desde' | 'hasta'): void {
    this.filtro[campo] = '';
    this.buscar();
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
        mostrarDetalleEnPantallaAngosta();
      },
      error: (err) => {
        this.toast.error(mensajeDeError(err, 'No se pudo abrir el análisis.'));
        this.cargandoDetalle.set(false);
      }
    });
  }

  /** Suma de los aportes: debe coincidir con el nivel de riesgo (tope 100). */
  sumaAportes(d: DetalleAnalisis): number {
    return d.indicadores.reduce((total, i) => total + Number(i.aporte), 0);
  }
}

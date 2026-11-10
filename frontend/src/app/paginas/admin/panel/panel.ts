import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { AuthService } from '../../../core/auth/auth.service';
import { mensajeDeError } from '../../../core/errores';
import { EstadoVacio } from '../../../core/ui/estado-vacio';
import { Icono } from '../../../core/ui/icono';
import { Skeleton } from '../../../core/ui/skeleton';
import { GraficoAnalisis, fechaCorta } from './grafico-analisis';
import { GraficoVeredictos, TotalesVeredicto } from './grafico-veredictos';
import { PanelDatos } from './panel.model';
import { PanelService } from './panel.service';

/**
 * Panel de administracion: tarjetas de totales, graficos por dia y por
 * veredicto, top de entidades suplantadas y ultimos analisis. El ADMIN ve
 * toda la plataforma; el ADMIN_EMPRESA, solo su empresa (lo decide el backend).
 */
@Component({
  selector: 'app-panel',
  imports: [GraficoAnalisis, GraficoVeredictos, DatePipe, DecimalPipe, Icono, Skeleton, EstadoVacio],
  templateUrl: './panel.html',
  styleUrl: './panel.css'
})
export class Panel implements OnInit {

  static readonly DIAS = 30;

  protected auth = inject(AuthService);
  private servicio = inject(PanelService);

  datos = signal<PanelDatos | null>(null);
  cargando = signal(true);
  error = signal<string | null>(null);

  protected readonly dias = Panel.DIAS;
  protected readonly fechaCorta = fechaCorta;

  /** Para la barra proporcional del top: la entidad mas suplantada ocupa el 100%. */
  protected maximoTop = computed(() =>
    Math.max(1, ...(this.datos()?.topEntidades.map((e) => e.veces) ?? [1])));

  /** Solo los dias con actividad, para la tabla de datos del grafico. */
  protected diasConDatos = computed(() =>
    (this.datos()?.porDia ?? []).filter((d) => d.total > 0).reverse());

  /** Suma del periodo por veredicto, para la dona. */
  protected totalesPeriodo = computed<TotalesVeredicto>(() =>
    (this.datos()?.porDia ?? []).reduce(
      (t, d) => ({ phishing: t.phishing + d.phishing, sospechoso: t.sospechoso + d.sospechosos, seguro: t.seguro + d.seguros }),
      { phishing: 0, sospechoso: 0, seguro: 0 }));

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.servicio.obtener(Panel.DIAS).subscribe({
      next: (datos) => {
        this.datos.set(datos);
        this.cargando.set(false);
      },
      error: (err) => {
        this.error.set(mensajeDeError(err, 'No se pudo cargar el panel.'));
        this.cargando.set(false);
      }
    });
  }

  protected etiquetaVeredicto(resultado: string): string {
    switch (resultado) {
      case 'phishing': return 'Phishing';
      case 'sospechoso': return 'Sospechoso';
      default: return 'Seguro';
    }
  }

  /** Nombre visible del tipo de entidad (la base guarda "banco", "telco"...). */
  protected nombreTipo(tipo: string): string {
    const nombres: Record<string, string> = {
      banco: 'Banca', billetera: 'Billetera', gobierno: 'Gobierno', telco: 'Telecomunicaciones',
    };
    return nombres[tipo] ?? tipo;
  }
}

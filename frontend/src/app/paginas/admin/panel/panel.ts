import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { AuthService } from '../../../core/auth/auth.service';
import { mensajeDeError } from '../../../core/errores';
import { GraficoAnalisis, fechaCorta } from './grafico-analisis';
import { PanelDatos } from './panel.model';
import { PanelService } from './panel.service';

/**
 * Panel de administracion: tarjetas de totales, grafico por dia, top de
 * entidades suplantadas y ultimos analisis. El ADMIN ve toda la plataforma;
 * el ADMIN_EMPRESA, solo su empresa (lo decide el backend).
 */
@Component({
  selector: 'app-panel',
  imports: [GraficoAnalisis, DatePipe, DecimalPipe],
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
}

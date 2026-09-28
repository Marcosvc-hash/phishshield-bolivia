import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AnalisisService } from '../analisis.service';
import { Analisis } from '../analisis.model';

@Component({
  selector: 'app-analizador',
  imports: [CommonModule, FormsModule],
  templateUrl: './analizador.html',
  styleUrl: './analizador.css'
})
export class Analizador {

  private servicio = inject(AnalisisService);

  url = '';
  analizando = signal(false);
  resultado = signal<Analisis | null>(null);
  error = signal<string | null>(null);
  historial = signal<Analisis[]>([]);

  analizar(): void {
    const url = this.url.trim();

    if (url.length === 0) {
      this.error.set('Escriba una URL para analizar.');
      return;
    }

    this.analizando.set(true);
    this.error.set(null);
    this.resultado.set(null);

    this.servicio.analizar({ url, origen: 'web' }).subscribe({
      next: (analisis) => {
        this.resultado.set(analisis);
        this.analizando.set(false);
        this.cargarHistorial();
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(this.mensajeDe(err));
        this.analizando.set(false);
      }
    });
  }

  limpiar(): void {
    this.url = '';
    this.resultado.set(null);
    this.error.set(null);
  }

  cargarHistorial(): void {
    this.servicio.listar(10).subscribe({
      next: (datos) => this.historial.set(datos),
      error: () => this.historial.set([])
    });
  }

  /** Clase CSS segun el veredicto, para pintar la tarjeta. */
  claseDe(veredicto: string): string {
    return `veredicto-${veredicto}`;
  }

    /** Texto que se le muestra al usuario. */
  etiquetaDe(veredicto: string, indicadores: number): string {
    switch (veredicto) {
      case 'phishing': return 'Phishing detectado';
      case 'sospechoso': return 'Sospechoso';
      default:
        return indicadores > 0
          ? 'Riesgo bajo, con senales detectadas'
          : 'Sin senales de riesgo';
    }
  }

  private mensajeDe(err: HttpErrorResponse): string {
    if (err.status === 0) {
      return 'No se pudo conectar con el backend. Verifique que este ejecutandose en el puerto 8080.';
    }
    if (err.error?.mensaje) {
      return err.error.mensaje;
    }
    if (err.error?.errores?.length) {
      return err.error.errores.map((e: { mensaje: string }) => e.mensaje).join('. ');
    }
    return 'Ocurrio un error al analizar la URL.';
  }
}
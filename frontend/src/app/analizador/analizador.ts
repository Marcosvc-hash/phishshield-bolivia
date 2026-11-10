import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AnalisisService } from '../analisis.service';
import { Analisis, Veredicto } from '../analisis.model';
import { AuthService } from '../core/auth/auth.service';
import { Icono, NombreIcono } from '../core/ui/icono';
import { MedidorRiesgo } from '../core/ui/medidor-riesgo';
import { explicar } from './explicaciones';

@Component({
  selector: 'app-analizador',
  imports: [CommonModule, FormsModule, Icono, MedidorRiesgo],
  templateUrl: './analizador.html',
  styleUrl: './analizador.css'
})
export class Analizador implements OnInit {

  /** Mismo criterio que el backend: solo http o https, sin espacios. */
  private static readonly FORMATO_URL = /^https?:\/\/\S+$/i;

  /** Enlaces de prueba que se ofrecen debajo del buscador. */
  protected readonly ejemplos = [
    'https://www.bnb.com.bo',
    'http://bnb-bo-seguro.com/login',
    'http://bancounion-verificacion.net/cuenta',
  ];

  private servicio = inject(AnalisisService);
  private auth = inject(AuthService);

  url = '';
  analizando = signal(false);
  resultado = signal<Analisis | null>(null);
  error = signal<string | null>(null);
  historial = signal<Analisis[]>([]);

  /** Los indicadores que mas aportan, primero. */
  protected indicadores = computed(() =>
    [...(this.resultado()?.indicadores ?? [])].sort((a, b) => b.aporte - a.aporte));

  protected readonly explicar = explicar;

  ngOnInit(): void {
    this.cargarHistorial();
  }

  analizar(): void {
    const url = this.url.trim();

    if (url.length === 0) {
      this.error.set('Escriba una URL para analizar.');
      return;
    }

    if (!Analizador.FORMATO_URL.test(url)) {
      this.error.set('La URL debe empezar con http:// o https://, por ejemplo https://ejemplo.com');
      this.resultado.set(null);
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

  probarEjemplo(url: string): void {
    this.url = url;
    this.analizar();
  }

  limpiar(): void {
    this.url = '';
    this.resultado.set(null);
    this.error.set(null);
  }

  /**
   * El listado general muestra URL de todos los usuarios, por eso el backend
   * solo se lo da al ADMIN. A los demas no se les pide (evita un 403 seguro).
   */
  cargarHistorial(): void {
    if (!this.auth.tieneRol('ADMIN')) {
      this.historial.set([]);
      return;
    }
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
      case 'sospechoso': return 'Enlace sospechoso';
      default:
        return indicadores > 0
          ? 'Riesgo bajo, con señales detectadas'
          : 'Sin señales de riesgo';
    }
  }

  /** Que deberia hacer la persona con este enlace. */
  consejoDe(veredicto: Veredicto, indicadores: number): string {
    switch (veredicto) {
      case 'phishing':
        return 'No abra este enlace ni ingrese sus datos. Si le llegó por mensaje o correo, elimínelo y ' +
          'avise a la entidad por sus canales oficiales.';
      case 'sospechoso':
        return 'Tenga cuidado. Antes de escribir cualquier dato, confirme la dirección con la entidad por sus ' +
          'canales oficiales.';
      default:
        return indicadores > 0
          ? 'Las señales encontradas no alcanzan para considerarlo peligroso, pero revíselas antes de continuar.'
          : 'No encontramos señales de riesgo. Aun así, nunca comparta su PIN ni sus contraseñas.';
    }
  }

  iconoDe(veredicto: Veredicto): NombreIcono {
    return veredicto === 'phishing' ? 'escudo-alerta' : veredicto === 'sospechoso' ? 'alerta' : 'escudo-check';
  }

  nombreVeredicto(veredicto: Veredicto): string {
    return veredicto === 'phishing' ? 'Phishing' : veredicto === 'sospechoso' ? 'Sospechoso' : 'Seguro';
  }

  private mensajeDe(err: HttpErrorResponse): string {
    if (err.status === 0) {
      return 'No se pudo conectar con el servidor de análisis. Verifique que el backend esté ejecutándose en el puerto 8080.';
    }
    if (err.error?.mensaje) {
      return err.error.mensaje;
    }
    if (err.error?.errores?.length) {
      return err.error.errores.map((e: { mensaje: string }) => e.mensaje).join('. ');
    }
    return 'Ocurrió un error al analizar la URL.';
  }
}

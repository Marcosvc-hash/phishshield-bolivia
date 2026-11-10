import { Component, computed, input } from '@angular/core';

/** Circulo de radio 10 centrado: base de varios iconos. */
const CIRCULO = 'M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0z';
const ESCUDO = 'M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z';

/**
 * Trazos de cada icono, dibujados para una grilla de 24x24 con linea de 2 px
 * (estilo de linea, sin relleno). Son propios: no se usa ninguna libreria.
 */
const ICONOS = {
  escudo: [ESCUDO],
  'escudo-check': [ESCUDO, 'M9 12l2 2 4-4'],
  'escudo-alerta': [ESCUDO, 'M12 8v4', 'M12 16h.01'],
  alerta: ['M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z', 'M12 9v4', 'M12 17h.01'],
  check: ['M20 6L9 17l-5-5'],
  'check-circulo': [CIRCULO, 'M8 12l3 3 5-6'],
  x: ['M18 6L6 18', 'M6 6l12 12'],
  'x-circulo': [CIRCULO, 'M15 9l-6 6', 'M9 9l6 6'],
  info: [CIRCULO, 'M12 16v-4', 'M12 8h.01'],
  buscar: ['M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16z', 'M21 21l-4.35-4.35'],
  panel: ['M3 3h7v9H3z', 'M14 3h7v5h-7z', 'M14 12h7v9h-7z', 'M3 16h7v5H3z'],
  historial: ['M3 12a9 9 0 1 0 3-6.7L3 8', 'M3 3v5h5', 'M12 7v5l4 2'],
  campana: ['M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9', 'M13.73 21a2 2 0 0 1-3.46 0'],
  lista: ['M8 6h13', 'M8 12h13', 'M8 18h13', 'M3 6h.01', 'M3 12h.01', 'M3 18h.01'],
  usuarios: ['M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2', 'M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z',
    'M23 21v-2a4 4 0 0 0-3-3.87', 'M16 3.13a4 4 0 0 1 0 7.75'],
  usuario: ['M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2', 'M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z'],
  empresa: ['M3 21h18', 'M5 21V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16', 'M9 7h1', 'M14 7h1', 'M9 11h1',
    'M14 11h1', 'M9 15h1', 'M14 15h1'],
  registro: ['M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2', 'M9 2h6v4H9z',
    'M9 12h6', 'M9 16h4'],
  salir: ['M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4', 'M16 17l5-5-5-5', 'M21 12H9'],
  menu: ['M3 6h18', 'M3 12h18', 'M3 18h18'],
  'chevron-izq': ['M15 18l-6-6 6-6'],
  'chevron-der': ['M9 18l6-6-6-6'],
  'chevron-abajo': ['M6 9l6 6 6-6'],
  ojo: ['M1 12s4-8 11-8 11 8 11 8-4 8-11 8S1 12 1 12z', 'M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z'],
  'ojo-tachado': ['M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94',
    'M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19',
    'M14.12 14.12a3 3 0 1 1-4.24-4.24', 'M1 1l22 22'],
  editar: ['M12 20h9', 'M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4z'],
  apagar: ['M18.36 6.64a9 9 0 1 1-12.73 0', 'M12 2v10'],
  refrescar: ['M23 4v6h-6', 'M1 20v-6h6', 'M3.51 9a9 9 0 0 1 14.85-3.36L23 10', 'M1 14l4.64 4.36A9 9 0 0 0 20.49 15'],
  globo: [CIRCULO, 'M2 12h20', 'M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z'],
  enlace: ['M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71',
    'M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71'],
  candado: ['M7 11h10a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2z', 'M8 11V7a4 4 0 0 1 8 0v4'],
  'candado-abierto': ['M7 11h10a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2z', 'M8 11V7a4 4 0 0 1 7.9-1'],
  ia: ['M7 5h10a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z', 'M9 9h6v6H9z',
    'M9 2v3', 'M15 2v3', 'M9 19v3', 'M15 19v3', 'M2 9h3', 'M2 15h3', 'M19 9h3', 'M19 15h3'],
  reglas: ['M3 6l1.5 1.5L7 5', 'M3 12l1.5 1.5L7 11', 'M3 18l1.5 1.5L7 17', 'M11 6h10', 'M11 12h10', 'M11 18h10'],
  banco: ['M3 21h18', 'M3 10h18', 'M5 6l7-3 7 3', 'M6 10v8', 'M10 10v8', 'M14 10v8', 'M18 10v8'],
  billetera: ['M20 12V8H6a2 2 0 0 1 0-4h12v4', 'M4 6v12a2 2 0 0 0 2 2h14v-4', 'M18 12a2 2 0 0 0 0 4h4v-4z'],
  senal: ['M2 20h.01', 'M7 20v-4', 'M12 20v-8', 'M17 20V8', 'M22 4v16'],
  reloj: [CIRCULO, 'M12 6v6l4 2'],
  actividad: ['M22 12h-4l-3 9L9 3l-3 9H2'],
  'flecha-der': ['M5 12h14', 'M12 5l7 7-7 7'],
  mas: ['M12 5v14', 'M5 12h14'],
  calendario: ['M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z', 'M16 2v4', 'M8 2v4', 'M3 10h18'],
  tendencia: ['M23 6l-9.5 9.5-5-5L1 18', 'M17 6h6v6'],
  correo: ['M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z', 'M22 6l-10 7L2 6'],
  capas: ['M12 2L2 7l10 5 10-5-10-5z', 'M2 17l10 5 10-5', 'M2 12l10 5 10-5'],
  servidor: ['M4 3h16a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z',
    'M4 14h16a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1v-5a1 1 0 0 1 1-1z', 'M7 6.5h.01', 'M7 17.5h.01'],
  etiqueta: ['M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z', 'M7 7h.01'],
  copia: ['M20 9h-9a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h9a2 2 0 0 0 2-2v-9a2 2 0 0 0-2-2z',
    'M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1'],
  prohibido: [CIRCULO, 'M4.93 4.93l14.14 14.14'],
  llave: ['M21 2l-2 2', 'M15.5 7.5l3 3L22 7l-3-3', 'M15.5 7.5L19 4', 'M11.39 11.61a5.5 5.5 0 1 1-7.78 7.78 5.5 5.5 0 0 1 7.78-7.78z',
    'M11.39 11.61L15.5 7.5'],
  mensaje: ['M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z'],
  imagen: ['M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z', 'M8.5 10a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z',
    'M21 15l-5-5L5 21'],
  regalo: ['M20 12v10H4V12', 'M2 7h20v5H2z', 'M12 22V7', 'M12 7H7.5a2.5 2.5 0 0 1 0-5C11 2 12 7 12 7z',
    'M12 7h4.5a2.5 2.5 0 0 0 0-5C13 2 12 7 12 7z'],
  guardar: ['M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z', 'M17 21v-8H7v8', 'M7 3v5h8'],
} as const satisfies Record<string, readonly string[]>;

export type NombreIcono = keyof typeof ICONOS;

/**
 * Icono SVG en linea. Por defecto es decorativo (aria-hidden): el texto de al
 * lado ya dice lo que significa. Si va solo, pase "etiqueta" para que el
 * lector de pantalla lo lea.
 *
 * Uso: <app-icono nombre="escudo" [tamano]="20" />
 */
@Component({
  selector: 'app-icono',
  template: `
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor"
         stroke-linecap="round" stroke-linejoin="round" focusable="false"
         [attr.width]="tamano()" [attr.height]="tamano()" [attr.stroke-width]="grosor()"
         [attr.aria-hidden]="etiqueta() ? null : 'true'"
         [attr.role]="etiqueta() ? 'img' : null"
         [attr.aria-label]="etiqueta() || null">
      @for (d of trazos(); track $index) {
        <path [attr.d]="d" />
      }
    </svg>
  `,
  styles: `
    :host { display: inline-flex; flex-shrink: 0; line-height: 0; }
  `
})
export class Icono {
  readonly nombre = input.required<NombreIcono>();
  readonly tamano = input(20);
  readonly grosor = input(2);
  readonly etiqueta = input<string | null>(null);

  protected trazos = computed(() => ICONOS[this.nombre()]);
}

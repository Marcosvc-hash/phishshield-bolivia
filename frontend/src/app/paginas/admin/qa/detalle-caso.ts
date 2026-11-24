import { Component, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Icono } from '../../../core/ui/icono';
import { ResultadoQa } from './qa.service';

/**
 * Detalle de un caso dentro de una ejecucion: la observacion, el tipo de
 * fallo, los pasos con lo observado, la evidencia de API y las metricas.
 */
@Component({
  selector: 'app-qa-detalle-caso',
  imports: [DecimalPipe, Icono],
  template: `
    @let r = resultado();
    <p class="observacion" [class.fallo]="r.estado === 'fail'">{{ r.observacionResumida }}</p>
    @if (r.tipoFallo) {
      <dl class="lista-datos">
        <dt>Tipo de fallo</dt>
        <dd>{{ r.tipoFallo === 'funcional' ? 'Funcional (posible defecto)' : 'Infraestructura (algo no respondió)' }}</dd>
        @if (r.pasoFallido) { <dt>Paso fallido</dt><dd>{{ r.pasoFallido }}</dd> }
      </dl>
    }
    @if (r.pasos.length > 0) {
      <h3 class="subtitulo-seccion">Pasos</h3>
      <ol class="pasos">
        @for (p of r.pasos; track p.numero) {
          <li class="paso" [class.fallo]="p.estado === 'fallo'">
            <div class="paso-cabecera">
              <app-icono [nombre]="p.estado === 'fallo' ? 'x-circulo' : 'check-circulo'" [tamano]="16" />
              <span>{{ p.descripcion }}</span>
            </div>
            @if (p.resultadoObservado) { <p class="paso-observado">{{ p.resultadoObservado }}</p> }
            @if (p.detalleApi) {
              <details class="evidencia">
                <summary>Evidencia de API</summary>
                <pre class="mono">{{ p.detalleApi }}</pre>
              </details>
            }
            @if (p.rutaCaptura) {
              <p class="texto-secundario paso-captura"><app-icono nombre="imagen" [tamano]="14" /> Captura guardada (en el reporte PDF)</p>
            }
          </li>
        }
      </ol>
    }
    @if (r.metricas.length > 0) {
      <h3 class="subtitulo-seccion">Métricas</h3>
      <div class="tabla-contenedor">
        <table class="tabla">
          <thead><tr><th scope="col">Métrica</th><th scope="col">Escalón</th><th scope="col">Valor</th><th scope="col">Umbral</th><th scope="col">Evaluación</th></tr></thead>
          <tbody>
            @for (m of r.metricas; track m.nombre + (m.escalon ?? '')) {
              <tr>
                <td>{{ m.nombre }}</td>
                <td>{{ m.escalon ?? '—' }}</td>
                <td class="numero">{{ m.valor | number: '1.0-4' }} {{ m.unidad }}</td>
                <td class="numero">@if (m.umbral !== null) { {{ m.umbral | number: '1.0-4' }} {{ m.unidad }} } @else { — }</td>
                <td>
                  @if (m.cumple === null) { <span class="texto-secundario">Informativa</span> }
                  @else { <span class="badge" [class]="m.cumple ? 'badge-exito' : 'badge-peligro'">{{ m.cumple ? 'Cumple' : 'No cumple' }}</span> }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }
  `,
  styles: `
    :host { display: block; padding: 0 var(--esp-4) var(--esp-4); }
    .observacion { font-size: var(--t-sm); color: var(--texto-2); margin-bottom: var(--esp-3); }
    .observacion.fallo { color: var(--peligro); }
    .subtitulo-seccion { margin: var(--esp-3) 0 var(--esp-2); }
    .pasos { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: var(--esp-2); }
    .paso { border-left: 3px solid var(--exito-borde); padding: var(--esp-1) var(--esp-3); }
    .paso.fallo { border-left-color: var(--peligro); background: var(--peligro-suave); }
    .paso-cabecera { display: flex; align-items: flex-start; gap: var(--esp-2); font-size: var(--t-sm); font-weight: 500; }
    .paso-cabecera app-icono { color: var(--exito); margin-top: 2px; flex-shrink: 0; }
    .paso.fallo .paso-cabecera app-icono { color: var(--peligro); }
    .paso-observado { margin: 2px 0 0 24px; font-size: var(--t-xs); color: var(--texto-2); word-break: break-word; }
    .paso-captura { display: flex; align-items: center; gap: 4px; margin-left: 24px; font-size: var(--t-xs); }
    .evidencia { margin: var(--esp-1) 0 0 24px; }
    .evidencia summary { cursor: pointer; font-size: var(--t-xs); color: var(--primario); font-weight: 600; }
    .evidencia pre {
      margin-top: var(--esp-1); max-height: 260px; overflow: auto; padding: var(--esp-2) var(--esp-3);
      background: var(--superficie-2); border: 1px solid var(--borde); border-radius: var(--radio-sm);
      font-size: 11px; white-space: pre-wrap; word-break: break-all;
    }
  `
})
export class DetalleCaso {
  readonly resultado = input.required<ResultadoQa>();
}

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../../../core/api';
import { Pagina, parametros } from '../../../core/pagina';

export type EstadoEjecucion = 'pendiente' | 'en_curso' | 'finalizada' | 'fallida';
export type ResultadoGlobal = 'aprobada' | 'con_fallos' | 'incompleta';
export type EstadoCaso = 'pass' | 'fail' | 'skipped' | 'not_run';
export type FormatoReporte = 'pdf' | 'html';

/** Un caso del catalogo (GET /api/admin/qa/casos). */
export interface CasoQa {
  codigo: string;
  tipo: string;
  tipoEtiqueta: string;
  suite: string;
  suiteEtiqueta: string;
  herramienta: string;
  herramientaEtiqueta: string;
  orden: number;
  nombre: string;
  objetivo: string;
  precondiciones: string;
  datosPrueba: string;
  resultadoEsperado: string;
  implementado: boolean;
}

export interface ResumenEjecucion {
  total: number;
  pass: number;
  fail: number;
  skipped: number;
  notRun: number;
  terminados: number;
  porcentajeExito: number;
}

/** Fila del historial (GET /api/admin/qa/ejecuciones). */
export interface FilaEjecucion {
  id: string;
  tipo: string;
  suite: string;
  suiteEtiqueta: string;
  alcance: string;
  correo: string;
  estado: EstadoEjecucion;
  resultadoGlobal: ResultadoGlobal | null;
  creadaEn: string;
  inicio: string | null;
  fin: string | null;
  duracionMs: number | null;
  resumen: ResumenEjecucion;
  estadoCorreo: string;
}

export interface PasoQa {
  numero: number;
  descripcion: string;
  resultadoObservado: string | null;
  estado: 'ok' | 'fallo';
  rutaCaptura: string | null;
  detalleApi: string | null;
  registradoEn: string;
}

export interface MetricaQa {
  nombre: string;
  valor: number;
  unidad: string;
  umbral: number | null;
  cumple: boolean | null;
  escalon: number | null;
}

export interface ResultadoQa {
  codigo: string;
  nombre: string;
  herramienta: string;
  orden: number;
  estado: EstadoCaso;
  tipoFallo: 'funcional' | 'infraestructura' | null;
  mensajeError: string | null;
  pasoFallido: string | null;
  observacion: string | null;
  observacionResumida: string;
  inicio: string | null;
  fin: string | null;
  duracionMs: number | null;
  pasos: PasoQa[];
  metricas: MetricaQa[];
}

/** Detalle de una ejecucion (GET /api/admin/qa/ejecuciones/{id}). */
export interface EjecucionQa {
  id: string;
  tipo: string;
  suite: string;
  suiteEtiqueta: string;
  alcance: string;
  correo: string;
  ambiente: string;
  parametros: Record<string, string>;
  estado: EstadoEjecucion;
  activa: boolean;
  resultadoGlobal: ResultadoGlobal | null;
  creadaEn: string;
  inicio: string | null;
  fin: string | null;
  duracionMs: number | null;
  mensajeError: string | null;
  estadoCorreo: string;
  errorCorreo: string | null;
  correoEnviadoEn: string | null;
  tieneReportes: boolean;
  resumen: ResumenEjecucion;
  resultados: ResultadoQa[];
}

/** Cuerpo de POST /api/admin/qa/ejecuciones: una suite completa o casos sueltos de una suite. */
export interface PedidoEjecucion {
  suite?: string;
  codigos?: string[];
  correo?: string;
  parametros?: Record<string, string>;
}

/** Centro de Pruebas QA (solo ADMIN). */
@Injectable({ providedIn: 'root' })
export class QaService {

  private http = inject(HttpClient);
  private readonly url = `${API_URL}/admin/qa`;

  casos(): Observable<CasoQa[]> {
    return this.http.get<CasoQa[]>(`${this.url}/casos`);
  }

  iniciar(pedido: PedidoEjecucion): Observable<EjecucionQa> {
    return this.http.post<EjecucionQa>(`${this.url}/ejecuciones`, pedido);
  }

  listar(pagina: number, tamano = 10): Observable<Pagina<FilaEjecucion>> {
    return this.http.get<Pagina<FilaEjecucion>>(`${this.url}/ejecuciones`, { params: parametros({ pagina, tamano }) });
  }

  detalle(id: string): Observable<EjecucionQa> {
    return this.http.get<EjecucionQa>(`${this.url}/ejecuciones/${id}`);
  }

  detener(id: string): Observable<EjecucionQa> {
    return this.http.post<EjecucionQa>(`${this.url}/ejecuciones/${id}/detener`, {});
  }

  /** El reporte como archivo; se pide con HttpClient para que lleve el token. */
  reporte(id: string, formato: FormatoReporte): Observable<Blob> {
    return this.http.get(`${this.url}/ejecuciones/${id}/reporte.${formato}`, { responseType: 'blob' });
  }
}

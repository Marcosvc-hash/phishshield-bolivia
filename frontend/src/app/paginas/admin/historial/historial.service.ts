import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../../../core/api';
import { Pagina, parametros } from '../../../core/pagina';

export type ResultadoAnalisis = 'seguro' | 'sospechoso' | 'phishing';

/** Fila de GET /api/admin/analisis (AnalisisFilaResponse en el backend). */
export interface AnalisisFila {
  id: string;
  url: string;
  dominio: string | null;
  resultado: ResultadoAnalisis;
  nivelRiesgo: number;
  fecha: string;
  usuario: string | null;
  empresa: string | null;
  estadoAlerta: string | null;
}

export interface IndicadorDetalle {
  codigo: string;
  descripcion: string;
  valor: string | null;
  aporte: number;
}

/** GET /api/admin/analisis/{id} (DetalleAnalisisResponse). */
export interface DetalleAnalisis {
  id: string;
  url: string;
  dominio: string | null;
  resultado: ResultadoAnalisis;
  nivelRiesgo: number;
  origen: string;
  tiempoMs: number | null;
  fecha: string;
  usuario: string | null;
  empresa: string | null;
  modeloIa: string | null;
  estadoAlerta: string | null;
  severidadAlerta: string | null;
  indicadores: IndicadorDetalle[];
}

export interface FiltroHistorial {
  resultado: string;
  desde: string;   // yyyy-MM-dd, como lo da <input type="date">
  hasta: string;
  q: string;
}

@Injectable({ providedIn: 'root' })
export class HistorialService {

  private http = inject(HttpClient);
  private readonly url = `${API_URL}/admin/analisis`;

  listar(filtro: FiltroHistorial, pagina: number, tamano = 20): Observable<Pagina<AnalisisFila>> {
    return this.http.get<Pagina<AnalisisFila>>(this.url, {
      params: parametros({ ...filtro, pagina, tamano })
    });
  }

  detalle(id: string): Observable<DetalleAnalisis> {
    return this.http.get<DetalleAnalisis>(`${this.url}/${id}`);
  }
}

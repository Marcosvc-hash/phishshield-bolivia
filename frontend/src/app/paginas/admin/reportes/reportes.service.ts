import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../../../core/api';
import { Pagina, parametros } from '../../../core/pagina';

/** Un reporte por empresa (GET/POST /api/admin/reportes). */
export interface ReporteEmpresa {
  id: string;
  idEmpresa: string;
  empresa: string;
  desde: string;
  hasta: string;
  dias: number;
  totalAnalisis: number;
  totalPhishing: number;
  totalAlertas: number;
  porcentajePhishing: number;
  generadoPor: string | null;
  fechaGeneracion: string;
}

export interface PedidoReporte {
  idEmpresa?: string;
  desde: string;
  hasta: string;
}

/** Reportes por empresa (HU-17): ADMIN ve todas las empresas; ADMIN_EMPRESA, la suya. */
@Injectable({ providedIn: 'root' })
export class ReportesService {

  private http = inject(HttpClient);
  private readonly url = `${API_URL}/admin/reportes`;

  listar(idEmpresa: string, pagina: number, tamano = 20): Observable<Pagina<ReporteEmpresa>> {
    return this.http.get<Pagina<ReporteEmpresa>>(this.url, { params: parametros({ idEmpresa, pagina, tamano }) });
  }

  generar(pedido: PedidoReporte): Observable<ReporteEmpresa> {
    return this.http.post<ReporteEmpresa>(this.url, pedido);
  }

  /** El PDF como archivo; se pide con HttpClient para que lleve el token. */
  pdf(id: string): Observable<Blob> {
    return this.http.get(`${this.url}/${id}/pdf`, { responseType: 'blob' });
  }
}

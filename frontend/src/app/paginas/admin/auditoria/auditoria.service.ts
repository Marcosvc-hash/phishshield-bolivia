import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../../../core/api';
import { Pagina, parametros } from '../../../core/pagina';

/** Fila de GET /api/admin/auditoria (RegistroAuditoriaResponse). */
export interface RegistroAuditoria {
  fecha: string;
  accion: string;
  usuario: string | null;
  entidad: string | null;
  detalle: Record<string, unknown>;
  ip: string | null;
}

export interface FiltroAuditoria {
  accion: string;
  usuario: string;
  desde: string;
  hasta: string;
}

@Injectable({ providedIn: 'root' })
export class AuditoriaService {

  private http = inject(HttpClient);
  private readonly url = `${API_URL}/admin/auditoria`;

  listar(filtro: FiltroAuditoria, pagina: number, tamano = 20): Observable<Pagina<RegistroAuditoria>> {
    return this.http.get<Pagina<RegistroAuditoria>>(this.url, {
      params: parametros({ ...filtro, pagina, tamano })
    });
  }

  acciones(): Observable<string[]> {
    return this.http.get<string[]>(`${this.url}/acciones`);
  }
}

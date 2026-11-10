import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../../../core/api';
import { Pagina, parametros } from '../../../core/pagina';

export type TipoLista = 'blanca' | 'negra' | 'gris';

/** Fila de GET /api/admin/dominios (DominioResponse). */
export interface DominioListado {
  id: string;
  dominio: string;
  lista: TipoLista;
  entidad: string | null;
  fuente: string;
  motivo: string | null;
  activo: boolean;
  creadoEn: string;
}

export interface EntidadSuplantada {
  nombre: string;
  tipo: string;
  dominioOficial: string | null;
}

export interface DatosDominio {
  dominio: string;
  lista: string;
  motivo: string;
  entidad: string;
}

@Injectable({ providedIn: 'root' })
export class DominiosService {

  private http = inject(HttpClient);
  private readonly url = `${API_URL}/admin/dominios`;

  listar(lista: string, activo: string, q: string, pagina: number, tamano = 20): Observable<Pagina<DominioListado>> {
    return this.http.get<Pagina<DominioListado>>(this.url, { params: parametros({ lista, activo, q, pagina, tamano }) });
  }

  agregar(datos: DatosDominio): Observable<DominioListado> {
    return this.http.post<DominioListado>(this.url, datos);
  }

  editar(id: string, datos: DatosDominio): Observable<DominioListado> {
    return this.http.put<DominioListado>(`${this.url}/${id}`, datos);
  }

  cambiarEstado(id: string, activo: boolean): Observable<DominioListado> {
    return this.http.patch<DominioListado>(`${this.url}/${id}/estado`, { activo });
  }

  entidades(): Observable<EntidadSuplantada[]> {
    return this.http.get<EntidadSuplantada[]>(`${API_URL}/admin/entidades`);
  }
}

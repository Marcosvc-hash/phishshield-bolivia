import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from './core/api';
import { Empresa, EmpresaDatos } from './empresa.model';

@Injectable({ providedIn: 'root' })
export class EmpresaService {

  private http = inject(HttpClient);
  private readonly url = `${API_URL}/empresas`;

  listar(): Observable<Empresa[]> {
    return this.http.get<Empresa[]>(this.url);
  }

  crear(datos: EmpresaDatos): Observable<Empresa> {
    return this.http.post<Empresa>(this.url, datos);
  }

  editar(id: string, datos: EmpresaDatos): Observable<Empresa> {
    return this.http.put<Empresa>(`${this.url}/${id}`, datos);
  }

  cambiarEstado(id: string, activo: boolean): Observable<Empresa> {
    return this.http.patch<Empresa>(`${this.url}/${id}/estado`, { activo });
  }
}

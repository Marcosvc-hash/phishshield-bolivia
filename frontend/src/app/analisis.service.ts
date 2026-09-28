import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Analisis, AnalizarUrlRequest } from './analisis.model';

@Injectable({ providedIn: 'root' })
export class AnalisisService {

  private http = inject(HttpClient);
  private readonly url = 'http://localhost:8080/api/analisis';

  analizar(peticion: AnalizarUrlRequest): Observable<Analisis> {
    return this.http.post<Analisis>(this.url, peticion);
  }

  listar(limite = 10): Observable<Analisis[]> {
    return this.http.get<Analisis[]>(`${this.url}?limite=${limite}`);
  }
}
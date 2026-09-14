import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Empresa } from './empresa.model';

@Injectable({ providedIn: 'root' })
export class EmpresaService {

  private http = inject(HttpClient);
  private readonly url = 'http://localhost:8080/api/empresas';

  listar(): Observable<Empresa[]> {
    return this.http.get<Empresa[]>(this.url);
  }
}
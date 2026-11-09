import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../../../core/api';
import { PanelDatos } from './panel.model';

@Injectable({ providedIn: 'root' })
export class PanelService {

  private http = inject(HttpClient);

  /** El backend decide el alcance segun el rol del token (toda la plataforma o una empresa). */
  obtener(dias = 30): Observable<PanelDatos> {
    return this.http.get<PanelDatos>(`${API_URL}/admin/panel`, { params: { dias } });
  }
}

import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { API_URL } from '../../../core/api';
import { Pagina, parametros } from '../../../core/pagina';

export type EstadoAlerta = 'pendiente' | 'en_revision' | 'revisada' | 'descartada';
export type SeveridadAlerta = 'baja' | 'media' | 'alta' | 'critica';

/** Fila de GET /api/admin/alertas (AlertaResponse). */
export interface Alerta {
  id: string;
  idAnalisis: string;
  severidad: SeveridadAlerta;
  estado: EstadoAlerta;
  url: string;
  dominio: string | null;
  nivelRiesgo: number;
  empresa: string | null;
  usuario: string | null;
  asignado: string | null;
  notas: string | null;
  fechaNotificacion: string;
  fechaCierre: string | null;
}

export interface CambioAlerta {
  estado?: EstadoAlerta;
  notas?: string;
}

@Injectable({ providedIn: 'root' })
export class AlertasService {

  private http = inject(HttpClient);
  private readonly url = `${API_URL}/admin/alertas`;

  /**
   * Cantidad de alertas pendientes, para el numero del menu lateral.
   * Es un signal compartido: el layout lo muestra y la pagina de alertas
   * lo refresca despues de cada cambio.
   */
  readonly pendientes = signal(0);

  listar(estado: string, severidad: string, pagina: number, tamano = 20): Observable<Pagina<Alerta>> {
    return this.http.get<Pagina<Alerta>>(this.url, { params: parametros({ estado, severidad, pagina, tamano }) });
  }

  actualizar(id: string, cambio: CambioAlerta): Observable<Alerta> {
    return this.http.patch<Alerta>(`${this.url}/${id}`, cambio).pipe(tap(() => this.refrescarPendientes()));
  }

  refrescarPendientes(): void {
    this.http.get<{ cantidad: number }>(`${this.url}/pendientes`).subscribe({
      next: (r) => this.pendientes.set(r.cantidad),
      // Si falla, el menu simplemente no muestra el numero
      error: () => this.pendientes.set(0)
    });
  }
}

import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/auth/auth.service';
import { mensajeDeError } from '../../../core/errores';
import { Pagina } from '../../../core/pagina';
import { Paginador } from '../../../core/ui/paginador';
import { Alerta, AlertasService, CambioAlerta, EstadoAlerta } from './alertas.service';

interface Accion {
  etiqueta: string;
  estado: EstadoAlerta;
  peligro?: boolean;
}

/**
 * Bandeja de alertas: filtros por estado y severidad y, al elegir una,
 * botones para avanzarla (pendiente -> en revision -> revisada/descartada)
 * y un cuadro de notas. Si un cambio no se permite, lo rechaza el backend
 * (la regla vive en el dominio); aca solo se ofrecen los botones que tienen
 * sentido para no confundir.
 */
@Component({
  selector: 'app-alertas',
  imports: [FormsModule, DatePipe, DecimalPipe, Paginador],
  templateUrl: './alertas.html',
  styleUrl: './alertas.css'
})
export class Alertas implements OnInit {

  protected auth = inject(AuthService);
  private servicio = inject(AlertasService);

  /** Por defecto se ven las pendientes: es lo que hay que atender. */
  estado = 'pendiente';
  severidad = '';

  pagina = signal<Pagina<Alerta> | null>(null);
  cargando = signal(true);
  error = signal<string | null>(null);
  aviso = signal<string | null>(null);

  seleccionada = signal<Alerta | null>(null);
  notas = '';
  guardando = signal(false);

  ngOnInit(): void {
    this.cargar(0);
  }

  cargar(numero: number): void {
    this.cargando.set(true);
    this.error.set(null);
    this.servicio.listar(this.estado, this.severidad, numero).subscribe({
      next: (pagina) => {
        this.pagina.set(pagina);
        this.cargando.set(false);
      },
      error: (err) => {
        this.error.set(mensajeDeError(err, 'No se pudieron cargar las alertas.'));
        this.cargando.set(false);
      }
    });
  }

  filtrar(): void {
    this.seleccionada.set(null);
    this.cargar(0);
  }

  elegir(alerta: Alerta): void {
    this.seleccionada.set(alerta);
    this.notas = alerta.notas ?? '';
    this.aviso.set(null);
  }

  /** Los botones que corresponden al estado actual. */
  acciones(a: Alerta): Accion[] {
    switch (a.estado) {
      case 'pendiente': return [{ etiqueta: 'Tomar en revision', estado: 'en_revision' }];
      case 'en_revision': return [
        { etiqueta: 'Marcar revisada', estado: 'revisada' },
        { etiqueta: 'Descartar (falso positivo)', estado: 'descartada', peligro: true },
      ];
      default: return [];
    }
  }

  esCerrada(a: Alerta): boolean {
    return a.estado === 'revisada' || a.estado === 'descartada';
  }

  /** Cada accion manda tambien las notas escritas, asi se guardan en el mismo paso. */
  aplicar(a: Alerta, estado: EstadoAlerta): void {
    this.guardar(a, { estado, notas: this.notas });
  }

  guardarNotas(a: Alerta): void {
    this.guardar(a, { notas: this.notas });
  }

  private guardar(a: Alerta, cambio: CambioAlerta): void {
    this.guardando.set(true);
    this.error.set(null);
    this.servicio.actualizar(a.id, cambio).subscribe({
      next: (actualizada) => {
        this.seleccionada.set(actualizada);
        this.notas = actualizada.notas ?? '';
        this.pagina.update((p) => p && {
          ...p, contenido: p.contenido.map((x) => (x.id === actualizada.id ? actualizada : x))
        });
        this.aviso.set('Cambios guardados.');
        this.guardando.set(false);
      },
      error: (err) => {
        this.error.set(mensajeDeError(err, 'No se pudo guardar el cambio.'));
        this.guardando.set(false);
      }
    });
  }

  nombreEstado(estado: EstadoAlerta): string {
    return estado === 'en_revision' ? 'en revision' : estado;
  }
}

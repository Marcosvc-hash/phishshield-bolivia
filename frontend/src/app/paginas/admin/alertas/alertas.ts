import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { filter } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { mensajeDeError } from '../../../core/errores';
import { Pagina } from '../../../core/pagina';
import { ConfirmacionService } from '../../../core/ui/confirmacion.service';
import { EstadoVacio } from '../../../core/ui/estado-vacio';
import { nombreEstadoAlerta, nombreSeveridad } from '../../../core/ui/etiquetas';
import { Icono, NombreIcono } from '../../../core/ui/icono';
import { Paginador } from '../../../core/ui/paginador';
import { Skeleton } from '../../../core/ui/skeleton';
import { ToastService } from '../../../core/ui/toast.service';
import { Alerta, AlertasService, CambioAlerta, EstadoAlerta } from './alertas.service';

interface Accion {
  etiqueta: string;
  estado: EstadoAlerta;
  icono: NombreIcono;
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
  imports: [FormsModule, DatePipe, DecimalPipe, Paginador, Icono, EstadoVacio, Skeleton],
  templateUrl: './alertas.html',
  styleUrl: './alertas.css'
})
export class Alertas implements OnInit {

  protected auth = inject(AuthService);
  private servicio = inject(AlertasService);
  private toast = inject(ToastService);
  private confirmacion = inject(ConfirmacionService);

  /** Por defecto se ven las pendientes: es lo que hay que atender. */
  estado = 'pendiente';
  severidad = '';

  pagina = signal<Pagina<Alerta> | null>(null);
  cargando = signal(true);
  error = signal<string | null>(null);

  seleccionada = signal<Alerta | null>(null);
  notas = '';
  guardando = signal(false);

  protected readonly estados = [
    { valor: 'pendiente', nombre: 'Pendientes' },
    { valor: 'en_revision', nombre: 'En revisión' },
    { valor: 'revisada', nombre: 'Revisadas' },
    { valor: 'descartada', nombre: 'Descartadas' },
    { valor: '', nombre: 'Todas' },
  ];
  protected readonly severidades = [
    { valor: '', nombre: 'Todas' },
    { valor: 'critica', nombre: 'Crítica' },
    { valor: 'alta', nombre: 'Alta' },
    { valor: 'media', nombre: 'Media' },
    { valor: 'baja', nombre: 'Baja' },
  ];

  protected readonly nombreEstado = nombreEstadoAlerta;
  protected readonly nombreSeveridad = nombreSeveridad;

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

  elegirEstado(valor: string): void {
    this.estado = valor;
    this.filtrar();
  }

  elegirSeveridad(valor: string): void {
    this.severidad = valor;
    this.filtrar();
  }

  elegir(alerta: Alerta): void {
    this.seleccionada.set(alerta);
    this.notas = alerta.notas ?? '';
  }

  /** Los botones que corresponden al estado actual. */
  acciones(a: Alerta): Accion[] {
    switch (a.estado) {
      case 'pendiente': return [{ etiqueta: 'Tomar en revisión', estado: 'en_revision', icono: 'usuario' }];
      case 'en_revision': return [
        { etiqueta: 'Marcar revisada', estado: 'revisada', icono: 'check' },
        { etiqueta: 'Descartar (falso positivo)', estado: 'descartada', icono: 'x-circulo', peligro: true },
      ];
      default: return [];
    }
  }

  esCerrada(a: Alerta): boolean {
    return a.estado === 'revisada' || a.estado === 'descartada';
  }

  /**
   * Cada accion manda tambien las notas escritas, asi se guardan en el mismo
   * paso. Descartar cierra la alerta para siempre: antes se pide confirmacion.
   */
  aplicar(a: Alerta, estado: EstadoAlerta): void {
    if (estado !== 'descartada') {
      this.guardar(a, { estado, notas: this.notas });
      return;
    }
    this.confirmacion.pedir({
      titulo: '¿Descartar esta alerta?',
      mensaje: `Se marcará como falso positivo y quedará cerrada; ya no se podrá modificar. URL: ${a.url}`,
      confirmar: 'Descartar alerta',
      peligro: true,
    }).pipe(filter(Boolean)).subscribe(() => this.guardar(a, { estado, notas: this.notas }));
  }

  guardarNotas(a: Alerta): void {
    this.guardar(a, { notas: this.notas });
  }

  private guardar(a: Alerta, cambio: CambioAlerta): void {
    this.guardando.set(true);
    this.servicio.actualizar(a.id, cambio).subscribe({
      next: (actualizada) => {
        this.seleccionada.set(actualizada);
        this.notas = actualizada.notas ?? '';
        this.pagina.update((p) => p && {
          ...p, contenido: p.contenido.map((x) => (x.id === actualizada.id ? actualizada : x))
        });
        this.toast.exito(cambio.estado
          ? `Alerta marcada como “${nombreEstadoAlerta(actualizada.estado).toLowerCase()}”.`
          : 'Notas guardadas.');
        this.guardando.set(false);
      },
      error: (err) => {
        this.toast.error(mensajeDeError(err, 'No se pudo guardar el cambio.'));
        this.guardando.set(false);
      }
    });
  }
}

import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { mensajeDeError } from '../../../core/errores';
import { Pagina } from '../../../core/pagina';
import { Paginador } from '../../../core/ui/paginador';
import { DatosDominio, DominioListado, DominiosService, EntidadSuplantada } from './dominios.service';

const FORMULARIO_VACIO: DatosDominio = { dominio: '', lista: 'negra', motivo: '', entidad: '' };

/**
 * Listas blanca y negra (solo ADMIN). "Eliminar" desactiva la fila; el
 * formato del dominio y los choques entre listas los valida el backend.
 */
@Component({
  selector: 'app-dominios',
  imports: [FormsModule, DatePipe, Paginador],
  templateUrl: './dominios.html',
  styleUrl: './dominios.css'
})
export class Dominios implements OnInit {

  private servicio = inject(DominiosService);

  // Filtros
  lista = '';
  activo = 'true';
  q = '';

  pagina = signal<Pagina<DominioListado> | null>(null);
  entidades = signal<EntidadSuplantada[]>([]);
  cargando = signal(true);
  error = signal<string | null>(null);
  aviso = signal<string | null>(null);

  /** Formulario de alta o de edicion (si editando tiene un id). */
  formulario: DatosDominio = { ...FORMULARIO_VACIO };
  editando = signal<DominioListado | null>(null);
  guardando = signal(false);

  ngOnInit(): void {
    this.servicio.entidades().subscribe({
      next: (lista) => this.entidades.set(lista),
      error: () => this.entidades.set([])
    });
    this.cargar(0);
  }

  cargar(numero: number): void {
    this.cargando.set(true);
    this.servicio.listar(this.lista, this.activo, this.q, numero).subscribe({
      next: (pagina) => {
        this.pagina.set(pagina);
        this.cargando.set(false);
      },
      error: (err) => {
        this.error.set(mensajeDeError(err, 'No se pudieron cargar las listas.'));
        this.cargando.set(false);
      }
    });
  }

  buscar(): void {
    this.error.set(null);
    this.cargar(0);
  }

  empezarEdicion(d: DominioListado): void {
    this.editando.set(d);
    this.formulario = { dominio: d.dominio, lista: d.lista, motivo: d.motivo ?? '', entidad: d.entidad ?? '' };
    this.aviso.set(null);
  }

  cancelar(): void {
    this.editando.set(null);
    this.formulario = { ...FORMULARIO_VACIO };
  }

  guardar(): void {
    const editando = this.editando();
    const peticion = editando
      ? this.servicio.editar(editando.id, this.formulario)
      : this.servicio.agregar(this.formulario);

    this.guardando.set(true);
    this.error.set(null);
    peticion.subscribe({
      next: (d) => {
        this.aviso.set(editando
          ? `Se actualizo ${d.dominio}.`
          : `${d.dominio} quedo en la lista ${d.lista}. Los analisis anteriores de cualquier URL se recalcularan.`);
        this.cancelar();
        this.guardando.set(false);
        this.cargar(this.pagina()?.pagina ?? 0);
      },
      error: (err) => {
        this.error.set(mensajeDeError(err, 'No se pudo guardar el dominio.'));
        this.guardando.set(false);
      }
    });
  }

  alternar(d: DominioListado): void {
    this.error.set(null);
    this.servicio.cambiarEstado(d.id, !d.activo).subscribe({
      next: (actualizado) => {
        this.pagina.update((p) => p && {
          ...p, contenido: p.contenido.map((x) => (x.id === actualizado.id ? actualizado : x))
        });
        this.aviso.set(actualizado.activo ? `${d.dominio} se reactivo.` : `${d.dominio} se desactivo (no se borra).`);
      },
      error: (err) => this.error.set(mensajeDeError(err, 'No se pudo cambiar el estado.'))
    });
  }
}

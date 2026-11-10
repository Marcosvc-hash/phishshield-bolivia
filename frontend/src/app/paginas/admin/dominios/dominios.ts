import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { filter } from 'rxjs';
import { mensajeDeError } from '../../../core/errores';
import { Pagina } from '../../../core/pagina';
import { ConfirmacionService } from '../../../core/ui/confirmacion.service';
import { EstadoVacio } from '../../../core/ui/estado-vacio';
import { nombreFuente, nombreLista } from '../../../core/ui/etiquetas';
import { Icono } from '../../../core/ui/icono';
import { Paginador } from '../../../core/ui/paginador';
import { Skeleton } from '../../../core/ui/skeleton';
import { ToastService } from '../../../core/ui/toast.service';
import { DatosDominio, DominioListado, DominiosService, EntidadSuplantada } from './dominios.service';

const FORMULARIO_VACIO: DatosDominio = { dominio: '', lista: 'negra', motivo: '', entidad: '' };

/**
 * Listas blanca y negra (solo ADMIN). "Desactivar" pone activo = false y
 * nunca borra la fila; el formato del dominio y los choques entre listas
 * los valida el backend.
 */
@Component({
  selector: 'app-dominios',
  imports: [FormsModule, DatePipe, Paginador, Icono, EstadoVacio, Skeleton],
  templateUrl: './dominios.html',
  styleUrl: './dominios.css'
})
export class Dominios implements OnInit {

  private servicio = inject(DominiosService);
  private toast = inject(ToastService);
  private confirmacion = inject(ConfirmacionService);

  // Filtros
  lista = '';
  activo = 'true';
  q = '';

  pagina = signal<Pagina<DominioListado> | null>(null);
  entidades = signal<EntidadSuplantada[]>([]);
  cargando = signal(true);
  error = signal<string | null>(null);

  /** Formulario de alta o de edicion (si editando tiene un id). */
  formulario: DatosDominio = { ...FORMULARIO_VACIO };
  editando = signal<DominioListado | null>(null);
  mostrarFormulario = signal(false);
  guardando = signal(false);

  protected readonly listas = [
    { valor: '', nombre: 'Todas' },
    { valor: 'negra', nombre: 'Negra' },
    { valor: 'blanca', nombre: 'Blanca' },
    { valor: 'gris', nombre: 'Gris' },
  ];
  protected readonly estados = [
    { valor: 'true', nombre: 'Activos' },
    { valor: 'false', nombre: 'Desactivados' },
    { valor: '', nombre: 'Todos' },
  ];

  protected readonly nombreLista = nombreLista;
  protected readonly nombreFuente = nombreFuente;

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

  elegirLista(valor: string): void {
    this.lista = valor;
    this.buscar();
  }

  elegirEstado(valor: string): void {
    this.activo = valor;
    this.buscar();
  }

  abrirFormulario(): void {
    this.editando.set(null);
    this.formulario = { ...FORMULARIO_VACIO };
    this.mostrarFormulario.set(true);
    this.enfocar('campo-dominio');
  }

  empezarEdicion(d: DominioListado): void {
    this.editando.set(d);
    this.formulario = { dominio: d.dominio, lista: d.lista, motivo: d.motivo ?? '', entidad: d.entidad ?? '' };
    this.mostrarFormulario.set(true);
    this.enfocar('campo-motivo');
  }

  cancelar(): void {
    this.editando.set(null);
    this.formulario = { ...FORMULARIO_VACIO };
    this.mostrarFormulario.set(false);
  }

  guardar(): void {
    const editando = this.editando();
    const peticion = editando
      ? this.servicio.editar(editando.id, this.formulario)
      : this.servicio.agregar(this.formulario);

    this.guardando.set(true);
    peticion.subscribe({
      next: (d) => {
        this.toast.exito(editando
          ? `Se actualizó ${d.dominio}.`
          : `${d.dominio} quedó en la lista ${nombreLista(d.lista).toLowerCase()}. Los análisis anteriores se recalcularán.`);
        this.cancelar();
        this.guardando.set(false);
        this.cargar(this.pagina()?.pagina ?? 0);
      },
      error: (err) => {
        this.toast.error(mensajeDeError(err, 'No se pudo guardar el dominio.'));
        this.guardando.set(false);
      }
    });
  }

  /** Desactivar pide confirmacion (cambia los veredictos); reactivar no. */
  alternar(d: DominioListado): void {
    if (!d.activo) {
      this.cambiarEstado(d);
      return;
    }
    this.confirmacion.pedir({
      titulo: `¿Desactivar ${d.dominio}?`,
      mensaje: `Deja de contar para la lista ${nombreLista(d.lista).toLowerCase()} y los análisis se recalcularán. ` +
        'La fila no se borra: puede reactivarla cuando quiera.',
      confirmar: 'Desactivar',
      peligro: true,
    }).pipe(filter(Boolean)).subscribe(() => this.cambiarEstado(d));
  }

  private cambiarEstado(d: DominioListado): void {
    this.servicio.cambiarEstado(d.id, !d.activo).subscribe({
      next: (actualizado) => {
        this.pagina.update((p) => p && {
          ...p, contenido: p.contenido.map((x) => (x.id === actualizado.id ? actualizado : x))
        });
        this.toast.exito(actualizado.activo ? `${d.dominio} se reactivó.` : `${d.dominio} se desactivó (no se borra).`);
      },
      error: (err) => this.toast.error(mensajeDeError(err, 'No se pudo cambiar el estado.'))
    });
  }

  /** Lleva el foco al formulario recien abierto (teclado y lectores de pantalla). */
  private enfocar(id: string): void {
    setTimeout(() => document.getElementById(id)?.focus());
  }
}

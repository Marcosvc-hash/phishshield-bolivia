import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { filter } from 'rxjs';
import { EmpresaService } from '../../../empresa.service';
import { Empresa, EmpresaDatos } from '../../../empresa.model';
import { mensajeDeError } from '../../../core/errores';
import { paginarLocal } from '../../../core/pagina';
import { ConfirmacionService } from '../../../core/ui/confirmacion.service';
import { EstadoVacio } from '../../../core/ui/estado-vacio';
import { nombrePlan } from '../../../core/ui/etiquetas';
import { Icono } from '../../../core/ui/icono';
import { Paginador } from '../../../core/ui/paginador';
import { Skeleton } from '../../../core/ui/skeleton';
import { ToastService } from '../../../core/ui/toast.service';

const VACIO: EmpresaDatos = { nombre: '', nit: '', sector: '', ciudad: '', plan: 'basico' };

/**
 * Empresas cliente (solo ADMIN): listar, crear, editar y activar/desactivar.
 * Una empresa inactiva no recibe usuarios nuevos; sus usuarios actuales
 * siguen entrando (ver docs/guia-modulo-admin.md).
 *
 * El backend devuelve la lista completa: busqueda, chips y paginacion se
 * resuelven en el navegador.
 */
@Component({
  selector: 'app-empresas',
  imports: [FormsModule, DatePipe, Icono, Paginador, EstadoVacio, Skeleton],
  templateUrl: './empresas.html',
  styleUrl: './empresas.css'
})
export class Empresas implements OnInit {

  private empresaService = inject(EmpresaService);
  private toast = inject(ToastService);
  private confirmacion = inject(ConfirmacionService);

  empresas = signal<Empresa[]>([]);
  cargando = signal(true);
  error = signal<string | null>(null);

  formulario: EmpresaDatos = { ...VACIO };
  /** null = el formulario crea; con valor = edita esa empresa. */
  editando = signal<Empresa | null>(null);
  mostrarFormulario = signal(false);
  guardando = signal(false);

  busqueda = signal('');
  filtroEstado = signal('');
  numeroPagina = signal(0);

  protected filtradas = computed(() => {
    const texto = this.busqueda().trim().toLowerCase();
    return this.empresas().filter((e) =>
      (!texto || [e.nombre, e.nit, e.sector, e.ciudad].some((v) => v?.toLowerCase().includes(texto))) &&
      (this.filtroEstado() === '' || String(e.activa) === this.filtroEstado()));
  });

  protected pagina = computed(() => paginarLocal(this.filtradas(), this.numeroPagina()));

  protected readonly estados = [
    { valor: '', nombre: 'Todas' },
    { valor: 'true', nombre: 'Activas' },
    { valor: 'false', nombre: 'Inactivas' },
  ];

  protected readonly nombrePlan = nombrePlan;

  ngOnInit(): void {
    this.empresaService.listar().subscribe({
      next: (datos) => {
        this.empresas.set(datos);
        this.cargando.set(false);
      },
      error: (err) => {
        this.error.set(mensajeDeError(err, 'No se pudieron cargar las empresas.'));
        this.cargando.set(false);
      }
    });
  }

  buscarTexto(texto: string): void {
    this.busqueda.set(texto);
    this.numeroPagina.set(0);
  }

  elegirEstado(valor: string): void {
    this.filtroEstado.set(valor);
    this.numeroPagina.set(0);
  }

  abrirFormulario(): void {
    this.editando.set(null);
    this.formulario = { ...VACIO };
    this.mostrarFormulario.set(true);
    setTimeout(() => document.getElementById('empresa-nombre')?.focus());
  }

  empezarEdicion(e: Empresa): void {
    this.editando.set(e);
    this.formulario = { nombre: e.nombre, nit: e.nit ?? '', sector: e.sector ?? '', ciudad: e.ciudad ?? '', plan: e.plan };
    this.mostrarFormulario.set(true);
    setTimeout(() => document.getElementById('empresa-nombre')?.focus());
  }

  cancelar(): void {
    this.editando.set(null);
    this.formulario = { ...VACIO };
    this.mostrarFormulario.set(false);
  }

  guardar(): void {
    const editando = this.editando();
    const peticion = editando
      ? this.empresaService.editar(editando.id, this.formulario)
      : this.empresaService.crear(this.formulario);

    this.guardando.set(true);
    peticion.subscribe({
      next: (guardada) => {
        this.empresas.update((lista) => editando
          ? lista.map((x) => (x.id === guardada.id ? guardada : x))
          : [...lista, guardada].sort((a, b) => a.nombre.localeCompare(b.nombre)));
        this.toast.exito(editando ? `Se actualizó ${guardada.nombre}.` : `Se registró ${guardada.nombre}.`);
        this.cancelar();
        this.guardando.set(false);
      },
      error: (err) => {
        this.toast.error(mensajeDeError(err, 'No se pudo guardar la empresa.'));
        this.guardando.set(false);
      }
    });
  }

  /** Desactivar se confirma (deja de recibir usuarios nuevos); activar va directo. */
  alternar(e: Empresa): void {
    if (!e.activa) {
      this.cambiarEstado(e);
      return;
    }
    this.confirmacion.pedir({
      titulo: `¿Desactivar ${e.nombre}?`,
      mensaje: 'Ya no se le podrán crear usuarios nuevos. Sus usuarios actuales seguirán entrando con normalidad.',
      confirmar: 'Desactivar',
      peligro: true,
    }).pipe(filter(Boolean)).subscribe(() => this.cambiarEstado(e));
  }

  private cambiarEstado(e: Empresa): void {
    this.empresaService.cambiarEstado(e.id, !e.activa).subscribe({
      next: (actualizada) => {
        this.empresas.update((lista) => lista.map((x) => (x.id === actualizada.id ? actualizada : x)));
        this.toast.exito(actualizada.activa
          ? `${actualizada.nombre} se activó.`
          : `${actualizada.nombre} se desactivó: ya no se le pueden crear usuarios.`);
      },
      error: (err) => this.toast.error(mensajeDeError(err, 'No se pudo cambiar el estado.'))
    });
  }
}

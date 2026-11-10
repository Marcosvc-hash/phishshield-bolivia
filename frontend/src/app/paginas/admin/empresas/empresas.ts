import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { EmpresaService } from '../../../empresa.service';
import { Empresa, EmpresaDatos } from '../../../empresa.model';
import { mensajeDeError } from '../../../core/errores';

const VACIO: EmpresaDatos = { nombre: '', nit: '', sector: '', ciudad: '', plan: 'basico' };

/**
 * Empresas cliente (solo ADMIN): listar, crear, editar y activar/desactivar.
 * Una empresa inactiva no recibe usuarios nuevos; sus usuarios actuales
 * siguen entrando (ver docs/guia-modulo-admin.md).
 */
@Component({
  selector: 'app-empresas',
  imports: [FormsModule],
  templateUrl: './empresas.html',
  styleUrl: './empresas.css'
})
export class Empresas implements OnInit {

  private empresaService = inject(EmpresaService);

  empresas = signal<Empresa[]>([]);
  cargando = signal(true);
  error = signal<string | null>(null);
  aviso = signal<string | null>(null);

  formulario: EmpresaDatos = { ...VACIO };
  /** null = el formulario crea; con valor = edita esa empresa. */
  editando = signal<Empresa | null>(null);
  guardando = signal(false);

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

  empezarEdicion(e: Empresa): void {
    this.editando.set(e);
    this.formulario = { nombre: e.nombre, nit: e.nit ?? '', sector: e.sector ?? '', ciudad: e.ciudad ?? '', plan: e.plan };
    this.aviso.set(null);
  }

  cancelar(): void {
    this.editando.set(null);
    this.formulario = { ...VACIO };
  }

  guardar(): void {
    const editando = this.editando();
    const peticion = editando
      ? this.empresaService.editar(editando.id, this.formulario)
      : this.empresaService.crear(this.formulario);

    this.guardando.set(true);
    this.error.set(null);
    peticion.subscribe({
      next: (guardada) => {
        this.empresas.update((lista) => editando
          ? lista.map((x) => (x.id === guardada.id ? guardada : x))
          : [...lista, guardada].sort((a, b) => a.nombre.localeCompare(b.nombre)));
        this.aviso.set(editando ? `Se actualizo ${guardada.nombre}.` : `Se registro ${guardada.nombre}.`);
        this.cancelar();
        this.guardando.set(false);
      },
      error: (err) => {
        this.error.set(mensajeDeError(err, 'No se pudo guardar la empresa.'));
        this.guardando.set(false);
      }
    });
  }

  alternar(e: Empresa): void {
    this.error.set(null);
    this.empresaService.cambiarEstado(e.id, !e.activa).subscribe({
      next: (actualizada) => {
        this.empresas.update((lista) => lista.map((x) => (x.id === actualizada.id ? actualizada : x)));
        this.aviso.set(actualizada.activa
          ? `${actualizada.nombre} se activo.`
          : `${actualizada.nombre} se desactivo: ya no se le pueden crear usuarios.`);
      },
      error: (err) => this.error.set(mensajeDeError(err, 'No se pudo cambiar el estado.'))
    });
  }
}

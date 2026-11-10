import { Component, OnInit, inject, signal } from '@angular/core';
import { EmpresaService } from '../../../empresa.service';
import { Empresa } from '../../../empresa.model';
import { mensajeDeError } from '../../../core/errores';

/** La tabla de empresas que antes estaba en la pagina publica; ahora solo para ADMIN. */
@Component({
  selector: 'app-empresas',
  templateUrl: './empresas.html',
  styleUrl: './empresas.css'
})
export class Empresas implements OnInit {

  private empresaService = inject(EmpresaService);

  empresas = signal<Empresa[]>([]);
  cargando = signal(true);
  error = signal<string | null>(null);

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
}

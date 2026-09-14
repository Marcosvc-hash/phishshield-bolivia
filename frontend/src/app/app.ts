import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EmpresaService } from './empresa.service';
import { Empresa } from './empresa.model';

@Component({
  selector: 'app-root',
  imports: [CommonModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {

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
        this.error.set('No se pudo conectar con el backend. Verifique que este ejecutandose en el puerto 8080.');
        this.cargando.set(false);
        console.error(err);
      }
    });
  }
}
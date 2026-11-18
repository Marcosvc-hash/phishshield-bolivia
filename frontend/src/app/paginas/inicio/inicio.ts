import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Analizador } from '../../analizador/analizador';
import { Icono, NombreIcono } from '../../core/ui/icono';
import { Marca } from '../../core/ui/marca';

interface EntidadProtegida {
  nombre: string;
  tipo: string;
  icono: NombreIcono;
}

/**
 * Pagina publica para visitantes sin sesion: el analizador de URL, como
 * funciona y las entidades que el motor protege. Quien ya inicio sesion no
 * llega aca: invitadoGuard lo manda al analizador dentro del panel
 * (/admin/analizar), que usa el mismo componente <app-analizador>.
 */
@Component({
  selector: 'app-inicio',
  imports: [Analizador, RouterLink, Icono, Marca],
  templateUrl: './inicio.html',
  styleUrl: './inicio.css'
})
export class Inicio {
  protected readonly anio = new Date().getFullYear();

  /**
   * Las mismas de entidades_suplantadas (02_seed.sql y V7). Van fijas porque
   * /api/admin/entidades es solo para el ADMIN. Solo nombres: sin logotipos.
   */
  protected readonly entidades: EntidadProtegida[] = [
    { nombre: 'Banco Nacional de Bolivia', tipo: 'Banca', icono: 'banco' },
    { nombre: 'Banco Unión', tipo: 'Banca', icono: 'banco' },
    { nombre: 'Banco Mercantil Santa Cruz', tipo: 'Banca', icono: 'banco' },
    { nombre: 'Banco BISA', tipo: 'Banca', icono: 'banco' },
    { nombre: 'Banco Ganadero', tipo: 'Banca', icono: 'banco' },
    { nombre: 'Tigo Money', tipo: 'Billetera móvil', icono: 'billetera' },
    { nombre: 'Impuestos Nacionales', tipo: 'Gobierno', icono: 'empresa' },
    { nombre: 'Segip', tipo: 'Gobierno', icono: 'empresa' },
    { nombre: 'Ende', tipo: 'Servicios', icono: 'empresa' },
    { nombre: 'Entel', tipo: 'Telecomunicaciones', icono: 'senal' },
  ];
}

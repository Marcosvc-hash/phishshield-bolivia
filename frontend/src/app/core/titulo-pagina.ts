import { Injectable, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';

/**
 * Pone en la pestana del navegador "Alertas · PhishShield Bolivia" a partir
 * del "title" de cada ruta (app.routes.ts). En C# seria como un filtro que
 * arma el ViewData["Title"] de cada pagina.
 */
@Injectable({ providedIn: 'root' })
export class TituloPagina extends TitleStrategy {

  private titulo = inject(Title);

  override updateTitle(estado: RouterStateSnapshot): void {
    const propio = this.buildTitle(estado);
    this.titulo.setTitle(propio ? `${propio} · PhishShield Bolivia` : 'PhishShield Bolivia');
  }
}

import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription, filter, interval, switchMap } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { mensajeDeError } from '../../../core/errores';
import { Pagina } from '../../../core/pagina';
import { ConfirmacionService } from '../../../core/ui/confirmacion.service';
import { EstadoVacio } from '../../../core/ui/estado-vacio';
import { Icono } from '../../../core/ui/icono';
import { Paginador } from '../../../core/ui/paginador';
import { Skeleton } from '../../../core/ui/skeleton';
import { ToastService } from '../../../core/ui/toast.service';
import { DetalleCaso } from './detalle-caso';
import {
  CasoQa, EjecucionQa, EstadoCaso, EstadoEjecucion, FilaEjecucion, FormatoReporte, PedidoEjecucion,
  QaService, ResultadoGlobal
} from './qa.service';

interface GrupoSuite {
  suite: string;
  etiqueta: string;
  casos: CasoQa[];
  implementados: number;
}

interface GrupoTipo {
  tipo: string;
  etiqueta: string;
  suites: GrupoSuite[];
}

const ESTADOS_EJECUCION: Record<EstadoEjecucion, string> = {
  pendiente: 'Pendiente', en_curso: 'En curso', finalizada: 'Finalizada', fallida: 'Interrumpida',
};
const RESULTADOS: Record<ResultadoGlobal, string> = {
  aprobada: 'Aprobada', con_fallos: 'Con fallos', incompleta: 'Incompleta',
};

/**
 * Centro de Pruebas QA (solo ADMIN): catalogo de casos agrupado por tipo y
 * suite, lanzamiento por suite completa o por casos marcados, avance en vivo
 * (se consulta cada 2 s mientras la ejecucion esta activa), historial y
 * detalle con pasos, evidencias y descarga de los reportes PDF y HTML.
 *
 * Los casos se tipifican solo por tipo (funcional / no funcional) y suite.
 */
@Component({
  selector: 'app-qa',
  imports: [FormsModule, DatePipe, DecimalPipe, Icono, EstadoVacio, Skeleton, Paginador, DetalleCaso],
  templateUrl: './qa.html',
  styleUrl: './qa.css'
})
export class Qa implements OnInit {

  /** Cada cuanto se consulta el avance de una ejecucion activa. */
  static readonly INTERVALO_MS = 2000;

  protected auth = inject(AuthService);
  private servicio = inject(QaService);
  private toast = inject(ToastService);
  private confirmacion = inject(ConfirmacionService);
  private destruir = inject(DestroyRef);

  // Catalogo y seleccion
  casos = signal<CasoQa[]>([]);
  cargandoCatalogo = signal(true);
  errorCatalogo = signal<string | null>(null);
  seleccion = signal<ReadonlySet<string>>(new Set());
  desplegadas = signal<ReadonlySet<string>>(new Set());
  correo = '';
  parametrosTexto = '';
  lanzando = signal(false);

  // Historial y detalle
  historial = signal<Pagina<FilaEjecucion> | null>(null);
  cargandoHistorial = signal(true);
  detalle = signal<EjecucionQa | null>(null);
  casoAbierto = signal<string | null>(null);
  deteniendo = signal(false);
  descargando = signal<FormatoReporte | null>(null);

  private seguimiento: Subscription | null = null;

  /** Tipo -> suites -> casos, en el orden en que llegan del backend (catalogo ordenado). */
  protected grupos = computed<GrupoTipo[]>(() => {
    const tipos: GrupoTipo[] = [];
    for (const caso of this.casos()) {
      let tipo = tipos.find((t) => t.tipo === caso.tipo);
      if (!tipo) {
        tipo = { tipo: caso.tipo, etiqueta: caso.tipoEtiqueta, suites: [] };
        tipos.push(tipo);
      }
      let suite = tipo.suites.find((s) => s.suite === caso.suite);
      if (!suite) {
        suite = { suite: caso.suite, etiqueta: caso.suiteEtiqueta, casos: [], implementados: 0 };
        tipo.suites.push(suite);
      }
      suite.casos.push(caso);
      if (caso.implementado) {
        suite.implementados++;
      }
    }
    return tipos;
  });

  /** Suite de los casos marcados (solo se combinan casos de una misma suite). */
  protected suiteSeleccion = computed(() => {
    const primero = this.seleccion().values().next().value;
    return primero ? this.casos().find((c) => c.codigo === primero)?.suite ?? null : null;
  });

  protected progreso = computed(() => {
    const d = this.detalle();
    return d && d.resumen.total > 0 ? Math.round((d.resumen.terminados / d.resumen.total) * 100) : 0;
  });

  ngOnInit(): void {
    this.destruir.onDestroy(() => this.dejarDeSeguir());
    this.cargarCatalogo();
    this.cargarHistorial(0, true);
  }

  // ------------------------------------------------------------------
  // Catalogo y seleccion
  // ------------------------------------------------------------------

  cargarCatalogo(): void {
    this.cargandoCatalogo.set(true);
    this.errorCatalogo.set(null);
    this.servicio.casos().subscribe({
      next: (casos) => {
        this.casos.set(casos);
        this.cargandoCatalogo.set(false);
      },
      error: (err) => {
        this.errorCatalogo.set(mensajeDeError(err, 'No se pudo cargar el catálogo de casos.'));
        this.cargandoCatalogo.set(false);
      }
    });
  }

  /** Las suites se muestran plegadas: el catalogo completo (27 casos) es largo. */
  estaDesplegada(suite: string): boolean {
    return this.desplegadas().has(suite);
  }

  alternarSuite(suite: string): void {
    const actual = new Set(this.desplegadas());
    if (!actual.delete(suite)) {
      actual.add(suite);
    }
    this.desplegadas.set(actual);
  }

  marcadosEn(grupo: GrupoSuite): number {
    return grupo.casos.filter((c) => this.seleccion().has(c.codigo)).length;
  }

  estaMarcado(caso: CasoQa): boolean {
    return this.seleccion().has(caso.codigo);
  }

  /** Marcar un caso de otra suite empieza una seleccion nueva. */
  alternar(caso: CasoQa): void {
    const actual = new Set(this.seleccion());
    if (actual.has(caso.codigo)) {
      actual.delete(caso.codigo);
    } else {
      if (this.suiteSeleccion() && this.suiteSeleccion() !== caso.suite) {
        actual.clear();
        this.toast.info('Solo se combinan casos de una misma suite: se empezó una selección nueva.');
      }
      actual.add(caso.codigo);
    }
    this.seleccion.set(actual);
  }

  limpiarSeleccion(): void {
    this.seleccion.set(new Set());
  }

  ejecutarSuite(grupo: GrupoSuite): void {
    this.lanzar({ suite: grupo.suite }, `Suite ${grupo.etiqueta} en marcha.`);
  }

  ejecutarSeleccion(): void {
    const codigos = this.casos().filter((c) => this.seleccion().has(c.codigo)).map((c) => c.codigo);
    if (codigos.length > 0) {
      this.lanzar({ codigos }, `Ejecución de ${codigos.length} caso(s) en marcha.`);
    }
  }

  /** "usuarios=10" por linea -> {usuarios: "10"}. Las lineas sin "=" se ignoran. */
  static leerParametros(texto: string): Record<string, string> {
    const resultado: Record<string, string> = {};
    for (const linea of texto.split(/\r?\n/)) {
      const i = linea.indexOf('=');
      if (i > 0) {
        resultado[linea.slice(0, i).trim()] = linea.slice(i + 1).trim();
      }
    }
    return resultado;
  }

  private lanzar(pedido: PedidoEjecucion, aviso: string): void {
    const correo = this.correo.trim();
    const parametros = Qa.leerParametros(this.parametrosTexto);
    const cuerpo: PedidoEjecucion = {
      ...pedido,
      ...(correo ? { correo } : {}),
      ...(Object.keys(parametros).length ? { parametros } : {}),
    };
    this.lanzando.set(true);
    this.servicio.iniciar(cuerpo).subscribe({
      next: (ejecucion) => {
        this.lanzando.set(false);
        this.limpiarSeleccion();
        this.toast.exito(aviso);
        this.mostrar(ejecucion);
        this.cargarHistorial(0);
      },
      error: (err) => {
        this.lanzando.set(false);
        this.toast.error(mensajeDeError(err, 'No se pudo iniciar la ejecución.'));
      }
    });
  }

  // ------------------------------------------------------------------
  // Historial, detalle y avance en vivo
  // ------------------------------------------------------------------

  /** Al entrar, si hay una ejecucion activa se abre sola y se sigue su avance. */
  cargarHistorial(numero: number, abrirActiva = false): void {
    this.cargandoHistorial.set(true);
    this.servicio.listar(numero).subscribe({
      next: (pagina) => {
        this.historial.set(pagina);
        this.cargandoHistorial.set(false);
        const activa = pagina.contenido.find((f) => f.estado === 'pendiente' || f.estado === 'en_curso');
        if (abrirActiva && activa) {
          this.abrir(activa);
        }
      },
      error: (err) => {
        this.cargandoHistorial.set(false);
        this.toast.error(mensajeDeError(err, 'No se pudo cargar el historial de ejecuciones.'));
      }
    });
  }

  abrir(fila: FilaEjecucion): void {
    this.servicio.detalle(fila.id).subscribe({
      next: (ejecucion) => this.mostrar(ejecucion),
      error: (err) => this.toast.error(mensajeDeError(err, 'No se pudo abrir la ejecución.'))
    });
  }

  private mostrar(ejecucion: EjecucionQa): void {
    if (this.detalle()?.id !== ejecucion.id) {
      this.casoAbierto.set(null);
    }
    this.detalle.set(ejecucion);
    if (ejecucion.activa) {
      this.seguir(ejecucion.id);
    } else {
      this.dejarDeSeguir();
    }
  }

  /** Consulta el detalle cada 2 s hasta que la ejecucion deja de estar activa. */
  private seguir(id: string): void {
    this.dejarDeSeguir();
    this.seguimiento = interval(Qa.INTERVALO_MS).pipe(
      switchMap(() => this.servicio.detalle(id)),
      filter(() => this.detalle()?.id === id)
    ).subscribe({
      next: (ejecucion) => {
        this.detalle.set(ejecucion);
        if (!ejecucion.activa) {
          this.dejarDeSeguir();
          this.cargarHistorial(this.historial()?.pagina ?? 0);
          this.avisarFin(ejecucion);
        }
      },
      error: () => this.dejarDeSeguir()
    });
  }

  private dejarDeSeguir(): void {
    this.seguimiento?.unsubscribe();
    this.seguimiento = null;
  }

  private avisarFin(e: EjecucionQa): void {
    const texto = `${e.suiteEtiqueta}: ${e.resumen.pass} PASS, ${e.resumen.fail} FAIL, `
      + `${e.resumen.skipped} SKIPPED, ${e.resumen.notRun} NOT_RUN.`;
    if (e.estado === 'fallida') {
      this.toast.info(`Ejecución interrumpida. ${texto}`);
    } else if (e.resultadoGlobal === 'aprobada') {
      this.toast.exito(`Ejecución aprobada. ${texto}`);
    } else {
      this.toast.info(`Ejecución terminada (${this.nombreResultado(e.resultadoGlobal).toLowerCase()}). ${texto}`);
    }
  }

  detener(e: EjecucionQa): void {
    this.confirmacion.pedir({
      titulo: '¿Detener la ejecución?',
      mensaje: 'El caso que está corriendo termina; los demás quedan NOT_RUN y la ejecución queda interrumpida.',
      confirmar: 'Detener',
      peligro: true,
    }).pipe(filter(Boolean)).subscribe(() => {
      this.deteniendo.set(true);
      this.servicio.detener(e.id).subscribe({
        next: (actualizada) => {
          this.deteniendo.set(false);
          this.toast.info('Se pidió detener la ejecución.');
          this.mostrar(actualizada);
          if (!actualizada.activa) {
            this.cargarHistorial(this.historial()?.pagina ?? 0);
          }
        },
        error: (err) => {
          this.deteniendo.set(false);
          this.toast.error(mensajeDeError(err, 'No se pudo detener la ejecución.'));
        }
      });
    });
  }

  alternarCaso(codigo: string): void {
    this.casoAbierto.set(this.casoAbierto() === codigo ? null : codigo);
  }

  descargar(e: EjecucionQa, formato: FormatoReporte): void {
    this.descargando.set(formato);
    this.servicio.reporte(e.id, formato).subscribe({
      next: (archivo) => {
        this.descargando.set(null);
        Qa.guardarArchivo(archivo, `reporte-qa-${e.suite}-${e.id.slice(0, 8)}.${formato}`);
      },
      error: (err) => {
        this.descargando.set(null);
        this.toast.error(mensajeDeError(err, 'No se pudo descargar el reporte.'));
      }
    });
  }

  /** Descarga un Blob con un enlace temporal (el reporte exige el token, no sirve un href directo). */
  static guardarArchivo(archivo: Blob, nombre: string): void {
    if (typeof URL.createObjectURL !== 'function') {
      return;
    }
    const enlace = document.createElement('a');
    enlace.href = URL.createObjectURL(archivo);
    enlace.download = nombre;
    enlace.click();
    setTimeout(() => URL.revokeObjectURL(enlace.href), 1000);
  }

  // ------------------------------------------------------------------
  // Textos y clases
  // ------------------------------------------------------------------

  nombreEstado(estado: EstadoEjecucion): string {
    return ESTADOS_EJECUCION[estado] ?? estado;
  }

  nombreResultado(resultado: ResultadoGlobal | null): string {
    return resultado ? RESULTADOS[resultado] ?? resultado : 'Sin resultado';
  }

  claseResultado(resultado: ResultadoGlobal | null, estado?: EstadoEjecucion): string {
    if (estado === 'pendiente' || estado === 'en_curso') {
      return 'badge-info';
    }
    switch (resultado) {
      case 'aprobada': return 'badge-exito';
      case 'con_fallos': return 'badge-peligro';
      case 'incompleta': return 'badge-alerta';
      default: return '';
    }
  }

  claseEstadoCaso(estado: EstadoCaso): string {
    switch (estado) {
      case 'pass': return 'badge-exito';
      case 'fail': return 'badge-peligro';
      case 'skipped': return 'badge-alerta';
      default: return '';
    }
  }

  /** PASS, FAIL, SKIPPED, NOT_RUN: los nombres que se usan en testing. */
  etiquetaCaso(estado: EstadoCaso): string {
    return estado.toUpperCase();
  }

  duracion(ms: number | null): string {
    if (ms === null || ms === undefined) {
      return '—';
    }
    if (ms < 1000) {
      return `${ms} ms`;
    }
    if (ms < 60_000) {
      return `${(ms / 1000).toFixed(1).replace('.', ',')} s`;
    }
    const s = Math.round(ms / 1000);
    return `${Math.floor(s / 60)} min ${String(s % 60).padStart(2, '0')} s`;
  }
}

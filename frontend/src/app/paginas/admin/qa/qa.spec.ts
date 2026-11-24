import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { Qa } from './qa';
import { CasoQa, EjecucionQa, FilaEjecucion } from './qa.service';
import { ConfirmacionService } from '../../../core/ui/confirmacion.service';

describe('Qa (Centro de Pruebas)', () => {
  const URL = 'http://localhost:8080/api/admin/qa';

  let fixture: ComponentFixture<Qa>;
  let http: HttpTestingController;

  const caso = (codigo: string, suite: string, tipo: string, implementado = true): CasoQa => ({
    codigo, tipo, tipoEtiqueta: tipo === 'funcional' ? 'Funcional' : 'No funcional',
    suite, suiteEtiqueta: suite === 'smoke' ? 'Smoke' : suite === 'sanity' ? 'Sanity' : 'Performance / carga',
    herramienta: 'api', herramientaEtiqueta: 'API (Rest Assured)', orden: 1, nombre: 'Caso ' + codigo,
    objetivo: 'obj', precondiciones: 'pre', datosPrueba: 'datos', resultadoEsperado: 'esp', implementado,
  });

  const CATALOGO = [
    caso('TC-SMK-001', 'smoke', 'funcional'),
    caso('TC-SMK-005', 'smoke', 'funcional', false),
    caso('TC-SAN-001', 'sanity', 'funcional'),
    caso('TC-PERF-001', 'performance', 'no_funcional', false),
  ];

  const resumen = (pass: number, notRun: number) => ({
    total: pass + notRun, pass, fail: 0, skipped: 0, notRun, terminados: pass, porcentajeExito: 0,
  });

  const ejecucion = (estado: EjecucionQa['estado'], pass = 0): EjecucionQa => ({
    id: 'e1', tipo: 'funcional', suite: 'smoke', suiteEtiqueta: 'Smoke', alcance: 'suite_completa',
    correo: 'qa@phishshield.bo', ambiente: 'phishshield_qa', parametros: {}, estado,
    activa: estado === 'pendiente' || estado === 'en_curso',
    resultadoGlobal: estado === 'finalizada' ? 'aprobada' : null,
    creadaEn: '2026-11-24T10:00:00-04:00', inicio: null, fin: null, duracionMs: null, mensajeError: null,
    estadoCorreo: 'pendiente', errorCorreo: null, correoEnviadoEn: null, tieneReportes: estado === 'finalizada',
    resumen: resumen(pass, 2 - pass),
    resultados: [
      { codigo: 'TC-SMK-001', nombre: 'La API responde', herramienta: 'api', orden: 1,
        estado: pass > 0 ? 'pass' : 'not_run', tipoFallo: null, mensajeError: null, pasoFallido: null, observacion: null,
        observacionResumida: 'Sin observaciones', inicio: null, fin: null, duracionMs: 120,
        pasos: [{ numero: 1, descripcion: 'Enviar GET /api/auth/yo', resultadoObservado: 'HTTP 401', estado: 'ok',
          rutaCaptura: null, detalleApi: 'GET http://localhost:8080/api/auth/yo', registradoEn: '2026-11-24T10:00:01-04:00' }],
        metricas: [] },
      { codigo: 'TC-SMK-005', nombre: 'Login en la interfaz', herramienta: 'ui', orden: 2, estado: 'not_run',
        tipoFallo: null, mensajeError: null, pasoFallido: null, observacion: pass > 1 ? null : 'Se implementa en una fase posterior',
        observacionResumida: 'Se implementa en una fase posterior', inicio: null, fin: null, duracionMs: null,
        pasos: [], metricas: [] },
    ],
  });

  const fila = (estado: FilaEjecucion['estado']): FilaEjecucion => ({
    id: 'e1', tipo: 'funcional', suite: 'smoke', suiteEtiqueta: 'Smoke', alcance: 'suite_completa',
    correo: 'qa@phishshield.bo', estado, resultadoGlobal: estado === 'finalizada' ? 'aprobada' : null,
    creadaEn: '2026-11-24T10:00:00-04:00', inicio: null, fin: null, duracionMs: 1500, resumen: resumen(1, 1),
    estadoCorreo: 'pendiente',
  });

  const pagina = (contenido: FilaEjecucion[]) => ({ contenido, pagina: 0, tamano: 10, totalElementos: contenido.length, totalPaginas: 1 });

  /** Responde las peticiones iniciales: catalogo, historial y capacidades opcionales. */
  async function cargar(historial: FilaEjecucion[] = [], asistenteDisponible = false) {
    http.expectOne(`${URL}/casos`).flush(CATALOGO);
    http.expectOne((r) => r.url === `${URL}/ejecuciones` && r.method === 'GET').flush(pagina(historial));
    http.expectOne(`${URL}/capacidades`).flush({ correoConfigurado: false, asistenteDisponible });
    fixture.detectChanges();
    await fixture.whenStable();
  }

  const texto = (selector: string) => (fixture.nativeElement.querySelector(selector) as HTMLElement | null)?.textContent ?? '';

  beforeEach(async () => {
    localStorage.clear();
    localStorage.setItem('phishshield.tokenAcceso', 't');
    localStorage.setItem('phishshield.usuario',
      JSON.stringify({ id: 'yo', nombreCompleto: 'Admin QA', email: 'qa@phishshield.bo', rol: 'ADMIN' }));

    await TestBed.configureTestingModule({
      imports: [Qa],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Qa);
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.useRealTimers();
    http.verify();
    localStorage.clear();
  });

  it('agrupa el catálogo por tipo y por suite y marca los casos sin programar', async () => {
    await cargar();

    const tipos = [...fixture.nativeElement.querySelectorAll('.grupo-tipo .subtitulo-seccion')].map((h: HTMLElement) => h.textContent?.trim());
    expect(tipos).toEqual(['Pruebas funcionales', 'Pruebas no funcionales']);
    const suites = [...fixture.nativeElement.querySelectorAll('.suite')].map((s: HTMLElement) => s.dataset['suite']);
    expect(suites).toEqual(['smoke', 'sanity', 'performance']);
    expect(fixture.nativeElement.querySelectorAll('.pendiente-fase').length).toBe(2);
    expect(texto('[data-suite="smoke"] .suite-cuenta')).toContain('2 caso(s) · 1 programado(s)');
  });

  it('ejecutar una suite manda el POST y consulta el avance cada 2 s hasta que termina', async () => {
    await cargar();
    vi.useFakeTimers();

    (fixture.nativeElement.querySelector('[data-suite="smoke"] .suite-cabecera .btn') as HTMLButtonElement).click();
    const post = http.expectOne((r) => r.url === `${URL}/ejecuciones` && r.method === 'POST');
    expect(post.request.body).toEqual({ suite: 'smoke' });
    post.flush(ejecucion('pendiente'));
    http.expectOne((r) => r.url === `${URL}/ejecuciones` && r.method === 'GET').flush(pagina([fila('pendiente')]));
    fixture.detectChanges();

    expect(texto('.estado-ejecucion')).toContain('Pendiente');
    expect(texto('.resultados')).toContain('En espera');

    vi.advanceTimersByTime(Qa.INTERVALO_MS);
    http.expectOne(`${URL}/ejecuciones/e1`).flush(ejecucion('en_curso', 1));
    fixture.detectChanges();
    expect(texto('.avance-texto')).toContain('1 de 2');

    vi.advanceTimersByTime(Qa.INTERVALO_MS);
    http.expectOne(`${URL}/ejecuciones/e1`).flush(ejecucion('finalizada', 2));
    // Al terminar se refresca el historial y se deja de consultar
    http.expectOne((r) => r.url === `${URL}/ejecuciones` && r.method === 'GET').flush(pagina([fila('finalizada')]));
    fixture.detectChanges();
    expect(texto('.estado-ejecucion')).toContain('Aprobada');

    vi.advanceTimersByTime(Qa.INTERVALO_MS * 3);
    http.expectNone(`${URL}/ejecuciones/e1`);
  });

  it('las suites empiezan plegadas y se despliegan al pulsar su nombre', async () => {
    await cargar();
    const lista = () => fixture.nativeElement.querySelector('#casos-smoke') as HTMLElement;
    expect(lista().hidden).toBe(true);

    (fixture.nativeElement.querySelector('[data-suite="smoke"] .suite-plegar') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(lista().hidden).toBe(false);
    expect(fixture.nativeElement.querySelector('[data-suite="smoke"] .suite-plegar').getAttribute('aria-expanded')).toBe('true');
  });

  it('solo combina casos de una misma suite', async () => {
    await cargar();
    const componente = fixture.componentInstance;

    componente.alternar(CATALOGO[0]);
    componente.alternar(CATALOGO[1]);
    expect([...componente.seleccion()]).toEqual(['TC-SMK-001', 'TC-SMK-005']);

    componente.alternar(CATALOGO[2]);
    expect([...componente.seleccion()]).toEqual(['TC-SAN-001']);
  });

  it('ejecutar la selección manda los códigos, el correo y los parámetros', async () => {
    await cargar();
    const componente = fixture.componentInstance;
    componente.alternar(CATALOGO[0]);
    componente.correo = 'docente@ejemplo.bo';
    componente.parametrosTexto = 'usuarios=10\nlinea sin igual\numbralP95Ms = 2000';

    componente.ejecutarSeleccion();

    const post = http.expectOne((r) => r.url === `${URL}/ejecuciones` && r.method === 'POST');
    expect(post.request.body).toEqual({
      codigos: ['TC-SMK-001'], correo: 'docente@ejemplo.bo', parametros: { usuarios: '10', umbralP95Ms: '2000' },
    });
    post.flush(ejecucion('finalizada', 2));
    http.expectOne((r) => r.url === `${URL}/ejecuciones` && r.method === 'GET').flush(pagina([fila('finalizada')]));
    expect(componente.seleccion().size).toBe(0);
  });

  it('si el backend rechaza el lanzamiento, muestra su mensaje', async () => {
    await cargar();
    fixture.componentInstance.ejecutarSuite({ suite: 'smoke', etiqueta: 'Smoke', casos: [], implementados: 0 });

    http.expectOne((r) => r.url === `${URL}/ejecuciones` && r.method === 'POST')
      .flush({ estado: 409, mensaje: 'Ya hay una ejecución en curso' }, { status: 409, statusText: 'Conflict' });
    expect(fixture.componentInstance.lanzando()).toBe(false);
    expect(fixture.componentInstance.detalle()).toBeNull();
  });

  it('al entrar abre sola la ejecución activa del historial', async () => {
    await cargar([fila('en_curso')]);

    http.expectOne(`${URL}/ejecuciones/e1`).flush(ejecucion('en_curso', 1));
    fixture.detectChanges();

    expect(texto('#titulo-ejecucion')).toContain('Smoke');
    expect(fixture.nativeElement.querySelector('.acciones-ejecucion .btn-peligro')).not.toBeNull();
    fixture.destroy();
  });

  it('un caso se despliega con sus pasos y la evidencia de API', async () => {
    await cargar([fila('finalizada')]);
    fixture.componentInstance.abrir(fila('finalizada'));
    http.expectOne(`${URL}/ejecuciones/e1`).flush(ejecucion('finalizada', 2));
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('.resultado-fila') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(texto('.pasos')).toContain('Enviar GET /api/auth/yo');
    expect(texto('.pasos')).toContain('HTTP 401');
    expect(texto('.evidencia pre')).toContain('GET http://localhost:8080/api/auth/yo');
  });

  it('"Ver captura" pide la imagen del paso con su ruta y la muestra', async () => {
    await cargar([fila('finalizada')]);
    fixture.componentInstance.abrir(fila('finalizada'));
    const conCaptura = ejecucion('finalizada', 2);
    conCaptura.resultados[0].pasos[0].rutaCaptura = 'e1/capturas/TC-SMK-001-01-login.png';
    http.expectOne(`${URL}/ejecuciones/e1`).flush(conCaptura);
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('.resultado-fila') as HTMLButtonElement).click();
    fixture.detectChanges();
    const crear = vi.fn(() => 'blob:captura');
    vi.stubGlobal('URL', Object.assign(URL, { createObjectURL: crear, revokeObjectURL: vi.fn() }));

    (fixture.nativeElement.querySelector('.paso-captura') as HTMLButtonElement).click();
    const peticion = http.expectOne((r) => r.url === `${URL}/ejecuciones/e1/evidencias`);
    expect(peticion.request.params.get('ruta')).toBe('e1/capturas/TC-SMK-001-01-login.png');
    peticion.flush(new Blob(['png']));
    fixture.detectChanges();

    expect(crear).toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('.captura img')).not.toBeNull();
    vi.unstubAllGlobals();
  });

  it('detener pide confirmación y manda el POST', async () => {
    await cargar([fila('en_curso')]);
    http.expectOne(`${URL}/ejecuciones/e1`).flush(ejecucion('en_curso', 1));
    fixture.detectChanges();
    vi.spyOn(TestBed.inject(ConfirmacionService), 'pedir').mockReturnValue(of(true));

    fixture.componentInstance.detener(fixture.componentInstance.detalle()!);

    http.expectOne(`${URL}/ejecuciones/e1/detener`).flush({ ...ejecucion('fallida', 1), mensajeError: 'Detenida por el usuario' });
    http.expectOne((r) => r.url === `${URL}/ejecuciones` && r.method === 'GET').flush(pagina([fila('fallida')]));
    fixture.detectChanges();
    expect(texto('.tarjeta.ejecucion')).toContain('Detenida por el usuario');
  });

  it('descarga el reporte PDF como archivo (con el token, no con un enlace directo)', async () => {
    await cargar([fila('finalizada')]);
    fixture.componentInstance.abrir(fila('finalizada'));
    http.expectOne(`${URL}/ejecuciones/e1`).flush(ejecucion('finalizada', 2));
    fixture.detectChanges();
    const guardar = vi.spyOn(Qa, 'guardarArchivo').mockImplementation(() => undefined);

    fixture.componentInstance.descargar(fixture.componentInstance.detalle()!, 'pdf');

    const peticion = http.expectOne(`${URL}/ejecuciones/e1/reporte.pdf`);
    expect(peticion.request.responseType).toBe('blob');
    peticion.flush(new Blob(['%PDF']));
    expect(guardar).toHaveBeenCalledWith(expect.any(Blob), 'reporte-qa-smoke-e1.pdf');
  });

  it('sin asistente de IA no muestra sus botones y avisa que el correo no está configurado', async () => {
    await cargar([fila('finalizada')]);
    fixture.componentInstance.abrir(fila('finalizada'));
    const conFail = ejecucion('finalizada', 1);
    conFail.resultados[1] = { ...conFail.resultados[1], estado: 'fail', tipoFallo: 'funcional', observacionResumida: 'Fallo funcional' };
    conFail.estadoCorreo = 'no_configurado';
    http.expectOne(`${URL}/ejecuciones/e1`).flush(conFail);
    fixture.detectChanges();
    (fixture.nativeElement.querySelectorAll('.resultado-fila')[1] as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.resumir-ia')).toBeNull();
    expect(fixture.nativeElement.querySelector('.analizar-ia')).toBeNull();
    expect(texto('.linea-correo')).toContain('no configurado');
    expect(texto('.lanzador')).toContain('MAIL_*');
  });

  it('con asistente, analiza un FAIL y resume la ejecución mostrando el aviso', async () => {
    await cargar([fila('finalizada')], true);
    fixture.componentInstance.abrir(fila('finalizada'));
    const conFail = ejecucion('finalizada', 1);
    conFail.resultados[1] = { ...conFail.resultados[1], estado: 'fail', tipoFallo: 'funcional', observacionResumida: 'Fallo funcional' };
    http.expectOne(`${URL}/ejecuciones/e1`).flush(conFail);
    fixture.detectChanges();
    (fixture.nativeElement.querySelectorAll('.resultado-fila')[1] as HTMLButtonElement).click();
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('.analizar-ia') as HTMLButtonElement).click();
    http.expectOne(`${URL}/ejecuciones/e1/casos/TC-SMK-005/analisis-ia`)
      .flush({ causaProbable: 'El frontend no estaba levantado', evidencia: 'Paso 1', aviso: 'La IA no decide PASS/FAIL' });
    fixture.detectChanges();
    expect(texto('app-qa-detalle-caso .ia')).toContain('El frontend no estaba levantado');

    (fixture.nativeElement.querySelector('.resumir-ia') as HTMLButtonElement).click();
    http.expectOne(`${URL}/ejecuciones/e1/resumen-ia`).flush({ resumen: 'Todo bien salvo un caso', aviso: 'La IA no decide PASS/FAIL' });
    fixture.detectChanges();
    expect(texto('.resumen-ia')).toContain('Todo bien salvo un caso');
    expect(texto('.resumen-ia')).toContain('La IA no decide PASS/FAIL');
  });

  it('lee los parámetros "clave=valor" uno por línea', async () => {
    await cargar();
    expect(Qa.leerParametros('a=1\n\n b = dos \r\nsin igual\n=sin clave')).toEqual({ a: '1', b: 'dos' });
  });
});

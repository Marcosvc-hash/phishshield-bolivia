import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Toasts } from './toasts';
import { ToastService } from './toast.service';

describe('Toasts', () => {
  let fixture: ComponentFixture<Toasts>;
  let servicio: ToastService;

  beforeEach(async () => {
    vi.useFakeTimers();
    await TestBed.configureTestingModule({ imports: [Toasts] }).compileComponents();
    servicio = TestBed.inject(ToastService);
    fixture = TestBed.createComponent(Toasts);
    fixture.detectChanges();
  });

  afterEach(() => vi.useRealTimers());

  const toasts = () => [...fixture.nativeElement.querySelectorAll('.toast')] as HTMLElement[];

  it('muestra el éxito como status y el error como alert', () => {
    servicio.exito('Cambios guardados.');
    servicio.error('No se pudo guardar.');
    fixture.detectChanges();

    expect(toasts().map((t) => t.getAttribute('role'))).toEqual(['status', 'alert']);
    expect(toasts()[1].textContent).toContain('No se pudo guardar.');
  });

  it('desaparece solo después de unos segundos', () => {
    servicio.exito('Listo');
    vi.advanceTimersByTime(ToastService.DURACION_MS.exito + 1);
    fixture.detectChanges();
    expect(toasts().length).toBe(0);
  });

  it('se puede cerrar con su botón', () => {
    servicio.info('Aviso');
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('button[aria-label="Cerrar aviso"]') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(servicio.toasts().length).toBe(0);
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ModalConfirmacion } from './modal-confirmacion';
import { ConfirmacionService } from './confirmacion.service';

describe('ModalConfirmacion', () => {
  let fixture: ComponentFixture<ModalConfirmacion>;
  let servicio: ConfirmacionService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ModalConfirmacion] }).compileComponents();
    servicio = TestBed.inject(ConfirmacionService);
    fixture = TestBed.createComponent(ModalConfirmacion);
    fixture.detectChanges();
  });

  async function abrir(): Promise<boolean[]> {
    const respuestas: boolean[] = [];
    servicio.pedir({ titulo: '¿Desactivar?', mensaje: 'Ya no podrá entrar.', confirmar: 'Desactivar', peligro: true })
      .subscribe((ok) => respuestas.push(ok));
    fixture.detectChanges();
    await fixture.whenStable();
    return respuestas;
  }

  const boton = (texto: string) => ([...fixture.nativeElement.querySelectorAll('button')] as HTMLButtonElement[])
    .find((b) => b.textContent?.trim() === texto)!;

  it('muestra el título y lleva el foco a Cancelar', async () => {
    await abrir();
    const modal = fixture.nativeElement.querySelector('[role="alertdialog"]') as HTMLElement;
    expect(modal.textContent).toContain('¿Desactivar?');
    expect(document.activeElement).toBe(boton('Cancelar'));
    expect(boton('Desactivar').classList).toContain('btn-peligro');
  });

  it('confirmar responde true y cierra el modal', async () => {
    const respuestas = await abrir();
    boton('Desactivar').click();
    fixture.detectChanges();
    expect(respuestas).toEqual([true]);
    expect(fixture.nativeElement.querySelector('[role="alertdialog"]')).toBeNull();
  });

  it('Escape cancela', async () => {
    const respuestas = await abrir();
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    expect(respuestas).toEqual([false]);
  });
});

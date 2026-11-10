/**
 * En pantallas angostas el panel de detalle (historial, alertas) se muestra
 * arriba de la tabla; si la persona hizo clic en una fila de mas abajo, se
 * lleva la vista hasta el detalle para que vea que se abrio.
 */
export function mostrarDetalleEnPantallaAngosta(): void {
  if (typeof window.matchMedia !== 'function' || !window.matchMedia('(max-width: 1180px)').matches) {
    return;
  }
  setTimeout(() => document.querySelector('.detalle-lateral')?.scrollIntoView({ behavior: 'smooth', block: 'start' }));
}

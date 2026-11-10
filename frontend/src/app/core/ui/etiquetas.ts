/**
 * Textos visibles de los valores que guarda la base ("en_revision",
 * "critica", "negra"...). La base y la API siguen en ASCII; la pantalla
 * los muestra con tildes y mayuscula inicial.
 */

const VEREDICTOS: Record<string, string> = { phishing: 'Phishing', sospechoso: 'Sospechoso', seguro: 'Seguro' };
const ESTADOS_ALERTA: Record<string, string> = {
  pendiente: 'Pendiente', en_revision: 'En revisión', revisada: 'Revisada', descartada: 'Descartada',
};
const SEVERIDADES: Record<string, string> = { critica: 'Crítica', alta: 'Alta', media: 'Media', baja: 'Baja' };
const LISTAS: Record<string, string> = { negra: 'Negra', blanca: 'Blanca', gris: 'Gris' };
const ESTADOS_USUARIO: Record<string, string> = {
  activo: 'Activo', inactivo: 'Inactivo', bloqueado: 'Bloqueado', pendiente: 'Pendiente',
};
const PLANES: Record<string, string> = { basico: 'Básico', profesional: 'Profesional', empresarial: 'Empresarial' };
const FUENTES: Record<string, string> = {
  semilla: 'Carga inicial', manual: 'Manual', reporte_usuario: 'Reporte de usuario', analista: 'Analista',
  automatico: 'Automático',
};

function buscar(tabla: Record<string, string>, valor: string | null | undefined): string {
  if (!valor) {
    return '—';
  }
  return Object.hasOwn(tabla, valor) ? tabla[valor] : capitalizar(valor.replaceAll('_', ' '));
}

export function capitalizar(texto: string): string {
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

export const nombreVeredicto = (v: string | null | undefined) => buscar(VEREDICTOS, v);
export const nombreEstadoAlerta = (v: string | null | undefined) => buscar(ESTADOS_ALERTA, v);
export const nombreSeveridad = (v: string | null | undefined) => buscar(SEVERIDADES, v);
export const nombreLista = (v: string | null | undefined) => buscar(LISTAS, v);
export const nombreEstadoUsuario = (v: string | null | undefined) => buscar(ESTADOS_USUARIO, v);
export const nombrePlan = (v: string | null | undefined) => buscar(PLANES, v);
export const nombreFuente = (v: string | null | undefined) => buscar(FUENTES, v);

/** "2026-11-01" (lo que da <input type="date">) -> "01/11/2026" para los chips de filtro. */
export function fechaLegible(iso: string): string {
  const [anio, mes, dia] = iso.split('-');
  return anio && mes && dia ? `${dia}/${mes}/${anio}` : iso;
}

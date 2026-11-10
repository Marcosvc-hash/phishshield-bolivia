/** Respuesta paginada del backend (PaginaResponse). La primera pagina es la 0. */
export interface Pagina<T> {
  contenido: T[];
  pagina: number;
  tamano: number;
  totalElementos: number;
  totalPaginas: number;
}

/**
 * Arma los parametros de una peticion GET quitando los vacios: un filtro
 * sin valor no se manda (el backend lo toma como "sin filtro").
 */
export function parametros(valores: Record<string, string | number | null | undefined>): Record<string, string> {
  const resultado: Record<string, string> = {};
  for (const [clave, valor] of Object.entries(valores)) {
    if (valor !== null && valor !== undefined && String(valor).trim() !== '') {
      resultado[clave] = String(valor).trim();
    }
  }
  return resultado;
}

/** Respuesta paginada del backend (PaginaResponse). La primera pagina es la 0. */
export interface Pagina<T> {
  contenido: T[];
  pagina: number;
  tamano: number;
  totalElementos: number;
  totalPaginas: number;
}

/**
 * Pagina en el navegador una lista que el backend devuelve completa
 * (usuarios, empresas), con la misma forma que una pagina del servidor,
 * para usar el mismo <app-paginador>. Si la pagina pedida ya no existe
 * (porque un filtro achico la lista) devuelve la ultima.
 */
export function paginarLocal<T>(lista: T[], pagina: number, tamano = 20): Pagina<T> {
  const totalPaginas = Math.ceil(lista.length / tamano);
  const actual = Math.max(0, Math.min(pagina, totalPaginas - 1));
  return {
    contenido: lista.slice(actual * tamano, (actual + 1) * tamano),
    pagina: actual,
    tamano,
    totalElementos: lista.length,
    totalPaginas,
  };
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

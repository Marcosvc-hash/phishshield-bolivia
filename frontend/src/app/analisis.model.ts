export interface Indicador {
  codigo: string;
  detalle: string | null;
  aporte: number;
}

export type Veredicto = 'seguro' | 'sospechoso' | 'phishing';

export interface Analisis {
  id: string;
  url: string;
  dominio: string | null;
  tipo: string;
  origen: string;
  resultado: Veredicto;
  nivelRiesgo: number;
  requiereAlerta: boolean;
  tiempoMs: number | null;
  fechaAnalisis: string;
  indicadores: Indicador[];
}

export interface AnalizarUrlRequest {
  url: string;
  origen?: string;
  idUsuario?: number;
  idEmpresa?: number;
}
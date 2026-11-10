/** Respuesta de GET /api/admin/panel (PanelResponse en el backend). */
export interface PanelDatos {
  resumen: {
    totalAnalisis: number;
    totalPhishing: number;
    porcentajePhishing: number;
    alertasAbiertas: number;
    usuariosActivos: number;
  };
  porDia: AnalisisDia[];
  topEntidades: { entidad: string; tipo: string; veces: number }[];
  ultimosAnalisis: {
    id: string;
    contenido: string;
    dominio: string | null;
    resultado: 'seguro' | 'sospechoso' | 'phishing';
    nivelRiesgo: number;
    fecha: string;
    usuario: string | null;
    empresa: string | null;
  }[];
  porEmpresa: {
    empresa: string;
    totalAnalisis: number;
    totalPhishing: number;
    riesgoPromedio: number | null;
    ultimoAnalisis: string | null;
  }[];
}

export interface AnalisisDia {
  dia: string;          // AAAA-MM-DD, en hora de Bolivia
  total: number;
  phishing: number;
  sospechosos: number;
  seguros: number;
}

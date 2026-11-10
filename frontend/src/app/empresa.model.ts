export interface Empresa {
  id: string;
  nombre: string;
  nit: string | null;
  sector: string | null;
  ciudad: string | null;
  plan: string;
  activa: boolean;
  creadoEn: string;
}

/** Cuerpo de POST /api/empresas y PUT /api/empresas/{id}. */
export interface EmpresaDatos {
  nombre: string;
  nit: string;
  sector: string;
  ciudad: string;
  plan: string;
}

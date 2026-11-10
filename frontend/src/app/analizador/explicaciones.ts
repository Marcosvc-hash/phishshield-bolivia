import { NombreIcono } from '../core/ui/icono';

/** Como se le muestra a una persona comun cada indicador del motor. */
export interface Explicacion {
  titulo: string;
  explicacion: string;
  icono: NombreIcono;
}

/**
 * Una entrada por codigo de la tabla reglas_deteccion. Los textos van en
 * lenguaje simple: quien analiza un enlace no tiene por que saber que es un
 * TLD o el typosquatting.
 */
export const EXPLICACIONES: Readonly<Record<string, Explicacion>> = {
  URL_LISTA_NEGRA: {
    titulo: 'Dominio reportado como fraude',
    explicacion: 'Este sitio ya fue denunciado como fraudulento y figura en la lista negra de PhishShield.',
    icono: 'prohibido',
  },
  URL_TYPOSQUATTING: {
    titulo: 'Imita el nombre de una entidad conocida',
    explicacion: 'La dirección se parece mucho a la de un banco o una entidad boliviana, pero no es la oficial. ' +
      'Es un truco para engañar a simple vista.',
    icono: 'copia',
  },
  URL_IP_LITERAL: {
    titulo: 'Usa números en lugar de un nombre',
    explicacion: 'Los sitios legítimos usan un nombre de dominio. Un enlace que solo tiene una dirección IP ' +
      'suele esconder quién está detrás.',
    icono: 'servidor',
  },
  URL_SIN_HTTPS: {
    titulo: 'Conexión sin cifrar',
    explicacion: 'El sitio no usa HTTPS: lo que escribas, como una contraseña, viaja sin protección.',
    icono: 'candado-abierto',
  },
  URL_TLD_SOSPECHOSO: {
    titulo: 'Terminación poco confiable',
    explicacion: 'La dirección termina en una extensión (.xyz, .top, .info…) muy usada en fraudes porque es barata ' +
      'y fácil de registrar.',
    icono: 'etiqueta',
  },
  URL_DOMINIO_NUEVO: {
    titulo: 'Sitio recién creado',
    explicacion: 'El dominio se registró hace menos de 30 días. Los fraudes suelen usar sitios nuevos que ' +
      'desaparecen pronto.',
    icono: 'calendario',
  },
  URL_SUBDOMINIOS: {
    titulo: 'Dirección demasiado larga',
    explicacion: 'El enlace encadena muchos nombres antes del dominio real para que parezca de otra entidad.',
    icono: 'capas',
  },
  URL_RUTA_SENSIBLE: {
    titulo: 'Busca tus datos de acceso',
    explicacion: 'La dirección incluye palabras como “login”, “cuenta”, “clave” o “pin”, típicas de las páginas ' +
      'que intentan robar credenciales.',
    icono: 'llave',
  },
  IA_MODELO_URL: {
    titulo: 'Detectado por inteligencia artificial',
    explicacion: 'El modelo de inteligencia artificial detectó patrones típicos de sitios de phishing en el ' +
      'nombre del dominio.',
    icono: 'ia',
  },
  TXT_URGENCIA: {
    titulo: 'Te presiona con urgencia',
    explicacion: 'El mensaje amenaza con bloquear tu cuenta o exige actuar de inmediato para que no lo pienses dos veces.',
    icono: 'reloj',
  },
  TXT_PIDE_CREDENCIAL: {
    titulo: 'Pide tu contraseña o PIN',
    explicacion: 'Ninguna entidad seria pide tu contraseña, PIN o código de verificación por mensaje.',
    icono: 'llave',
  },
  TXT_PREMIO: {
    titulo: 'Promete premios',
    explicacion: 'Ofrece bonos, sorteos o regalos: un gancho clásico para que entregues tus datos.',
    icono: 'regalo',
  },
  MAIL_SPF_FALLA: {
    titulo: 'Remitente no verificado',
    explicacion: 'El correo no pasa los controles que confirman que realmente lo envió quien dice enviarlo.',
    icono: 'correo',
  },
  MAIL_DOMINIO_DISTINTO: {
    titulo: 'El remitente no coincide con la marca',
    explicacion: 'El correo dice venir de una entidad, pero se envió desde otro dominio.',
    icono: 'correo',
  },
  IMG_LOGO_ALTERADO: {
    titulo: 'Logotipo alterado',
    explicacion: 'El logotipo tiene diferencias con el oficial, señal de una copia.',
    icono: 'imagen',
  },
};

/** Para un codigo nuevo que todavia no tiene texto: nunca se muestra el codigo suelto. */
export const EXPLICACION_POR_DEFECTO: Explicacion = {
  titulo: 'Otra señal de riesgo',
  explicacion: 'PhishShield encontró en este enlace una característica que suele aparecer en sitios fraudulentos.',
  icono: 'alerta',
};

export function explicar(codigo: string): Explicacion {
  // Object.hasOwn evita que "constructor" o "toString" devuelvan algo raro
  return Object.hasOwn(EXPLICACIONES, codigo) ? EXPLICACIONES[codigo] : EXPLICACION_POR_DEFECTO;
}

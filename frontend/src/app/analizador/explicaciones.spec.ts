import { EXPLICACION_POR_DEFECTO, explicar } from './explicaciones';

describe('explicar', () => {
  it('explica el indicador de la IA en lenguaje simple', () => {
    expect(explicar('IA_MODELO_URL').explicacion)
      .toContain('El modelo de inteligencia artificial detectó patrones típicos de sitios de phishing');
  });

  it('un código desconocido recibe el texto por defecto, nunca el código', () => {
    const e = explicar('URL_REGLA_NUEVA');
    expect(e).toBe(EXPLICACION_POR_DEFECTO);
    expect(e.titulo).not.toContain('URL_');
  });

  it('no se confunde con propiedades heredadas de Object', () => {
    expect(explicar('constructor')).toBe(EXPLICACION_POR_DEFECTO);
    expect(explicar('toString')).toBe(EXPLICACION_POR_DEFECTO);
  });
});

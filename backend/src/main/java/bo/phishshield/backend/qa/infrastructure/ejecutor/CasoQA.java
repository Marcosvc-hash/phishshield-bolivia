package bo.phishshield.backend.qa.infrastructure.ejecutor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Une un metodo de prueba con su caso del catalogo (qa_casos.codigo).
 * Los metadatos (objetivo, datos, resultado esperado) viven en la base;
 * el metodo solo lleva el codigo:
 *
 * <pre>
 *   &#64;Test
 *   &#64;CasoQA("TC-SMK-001")
 *   void laApiResponde(ContextoCaso ctx) { ... }
 * </pre>
 *
 * Equivale a un atributo de C# ([CasoQA("TC-SMK-001")]) leido por reflexion.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CasoQA {

    String value();
}

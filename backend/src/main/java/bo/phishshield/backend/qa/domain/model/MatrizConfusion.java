package bo.phishshield.backend.qa.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Matriz de confusion de un detector de phishing ("positivo" aqui es el
 * termino estadistico: la URL se marco como phishing; no es una
 * clasificacion de casos de prueba).
 *
 *                     detectada    no detectada
 *   phishing real        vp            fn
 *   legitima real        fp            vn
 */
public record MatrizConfusion(int vp, int fp, int vn, int fn) {

    public MatrizConfusion {
        if (vp < 0 || fp < 0 || vn < 0 || fn < 0) {
            throw new IllegalArgumentException("Los conteos de la matriz no pueden ser negativos");
        }
    }

    public static MatrizConfusion vacia() {
        return new MatrizConfusion(0, 0, 0, 0);
    }

    /** Suma una URL: su etiqueta real y lo que dijo el detector. */
    public MatrizConfusion con(boolean esPhishing, boolean detectada) {
        if (esPhishing) {
            return detectada ? new MatrizConfusion(vp + 1, fp, vn, fn) : new MatrizConfusion(vp, fp, vn, fn + 1);
        }
        return detectada ? new MatrizConfusion(vp, fp + 1, vn, fn) : new MatrizConfusion(vp, fp, vn + 1, fn);
    }

    public int total() {
        return vp + fp + vn + fn;
    }

    /** De las que marco como phishing, cuantas lo eran: vp / (vp + fp). 0 si no marco ninguna. */
    public BigDecimal precision() {
        return division(vp, vp + fp);
    }

    /** De las de phishing, cuantas detecto: vp / (vp + fn). */
    public BigDecimal recall() {
        return division(vp, vp + fn);
    }

    /** Media armonica de precision y recall. */
    public BigDecimal f1() {
        BigDecimal p = precision();
        BigDecimal r = recall();
        BigDecimal suma = p.add(r);
        if (suma.signum() == 0) {
            return BigDecimal.ZERO.setScale(4);
        }
        return p.multiply(r).multiply(BigDecimal.TWO).divide(suma, 4, RoundingMode.HALF_UP);
    }

    /** Aciertos sobre el total: (vp + vn) / total. */
    public BigDecimal exactitud() {
        return division(vp + vn, total());
    }

    private static BigDecimal division(int numerador, int denominador) {
        if (denominador == 0) {
            return BigDecimal.ZERO.setScale(4);
        }
        return BigDecimal.valueOf(numerador).divide(BigDecimal.valueOf(denominador), 4, RoundingMode.HALF_UP);
    }
}

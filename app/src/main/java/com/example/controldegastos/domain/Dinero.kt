package com.example.controldegastos.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Utilidades monetarias.
 *
 * El dinero se convierte a centavos (Long) para evitar errores de punto
 * flotante al sumar. El formato siempre usa USD porque la app muestra "$".
 */
object Dinero {

    private val localeMoneda: Locale = Locale.US

    /** Convierte un monto en dólares a centavos, redondeando al centavo más cercano. */
    fun aCentavos(monto: Double): Long =
        BigDecimal.valueOf(monto)
            .multiply(BigDecimal(100))
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()

    /** Convierte centavos a un BigDecimal con dos decimales. */
    fun deCentavos(centavos: Long): BigDecimal =
        BigDecimal.valueOf(centavos, 2)

    /** Formatea centavos como moneda, por ejemplo: $1,200.00 o -$75.50. */
    fun formatear(centavos: Long): String {
        val formato = NumberFormat.getCurrencyInstance(localeMoneda)
        formato.currency = Currency.getInstance("USD")
        return formato.format(deCentavos(centavos))
    }

    /** Formatea un gasto con signo negativo. */
    fun formatearGasto(centavos: Long): String = "-" + formatear(centavos)

    /** Suma montos en dólares usando centavos para evitar acumulación de error. */
    fun sumarEnCentavos(montos: Iterable<Double>): Long =
        montos.fold(0L) { acumulado, monto -> acumulado + aCentavos(monto) }
}

package com.example.controldegastos.domain

import com.example.controldegastos.model.Gasto
import com.example.controldegastos.model.Ingreso

/**
 * Resultado del balance, expresado en centavos.
 */
data class Balance(
    val presupuestoCentavos: Long,
    val totalGastadoCentavos: Long,
    val disponibleCentavos: Long
)

object BalanceCalculator {

    fun calcular(gastos: List<Gasto>, ingresos: List<Ingreso>): Balance {
        val presupuesto = Dinero.sumarEnCentavos(ingresos.map { it.monto })
        val totalGastado = Dinero.sumarEnCentavos(gastos.map { it.monto })
        return Balance(
            presupuestoCentavos = presupuesto,
            totalGastadoCentavos = totalGastado,
            disponibleCentavos = presupuesto - totalGastado
        )
    }
}

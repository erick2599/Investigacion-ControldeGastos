package com.example.controldegastos.domain

import com.example.controldegastos.model.Gasto
import com.example.controldegastos.model.Ingreso
import org.junit.Assert.assertEquals
import org.junit.Test

class BalanceCalculatorTest {

    @Test
    fun `calcula presupuesto gastado y disponible`() {
        val gastos = listOf(
            Gasto(id = "1", monto = 10.50),
            Gasto(id = "2", monto = 0.25)
        )
        val ingresos = listOf(
            Ingreso(id = "1", monto = 100.0),
            Ingreso(id = "2", monto = 25.75)
        )

        val balance = BalanceCalculator.calcular(gastos, ingresos)

        assertEquals(12575L, balance.presupuestoCentavos)
        assertEquals(1075L, balance.totalGastadoCentavos)
        assertEquals(11500L, balance.disponibleCentavos)
    }

    @Test
    fun `devuelve ceros cuando no hay movimientos`() {
        val balance = BalanceCalculator.calcular(emptyList(), emptyList())

        assertEquals(0L, balance.presupuestoCentavos)
        assertEquals(0L, balance.totalGastadoCentavos)
        assertEquals(0L, balance.disponibleCentavos)
    }

    @Test
    fun `el disponible puede ser negativo`() {
        val gastos = listOf(Gasto(id = "1", monto = 50.0))
        val ingresos = listOf(Ingreso(id = "1", monto = 20.0))

        val balance = BalanceCalculator.calcular(gastos, ingresos)

        assertEquals(-3000L, balance.disponibleCentavos)
    }
}

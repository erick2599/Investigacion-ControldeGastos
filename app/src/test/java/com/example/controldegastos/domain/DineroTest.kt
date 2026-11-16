package com.example.controldegastos.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DineroTest {

    @Test
    fun `convierte dolares a centavos redondeando`() {
        assertEquals(1000L, Dinero.aCentavos(10.0))
        assertEquals(1050L, Dinero.aCentavos(10.50))
        assertEquals(1001L, Dinero.aCentavos(10.005))
    }

    @Test
    fun `suma sin acumular errores de punto flotante`() {
        val total = Dinero.sumarEnCentavos(listOf(0.1, 0.2))

        assertEquals(30L, total)
    }

    @Test
    fun `formatea montos en dolares`() {
        assertEquals("\$1,234.56", Dinero.formatear(123456L))
    }

    @Test
    fun `formatea gastos con signo negativo`() {
        assertEquals("-\$75.50", Dinero.formatearGasto(7550L))
    }

    @Test
    fun `convierte de vuelta de centavos a decimal`() {
        assertEquals("12.34", Dinero.deCentavos(1234L).toPlainString())
    }
}

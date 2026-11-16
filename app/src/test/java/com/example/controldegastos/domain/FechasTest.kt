package com.example.controldegastos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FechasTest {

    @Test
    fun `ahora genera texto y milisegundos coherentes`() {
        val sello = Fechas.ahora()

        assertTrue(sello.millis > 0L)
        assertTrue(sello.texto.matches(Regex("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}")))
    }

    @Test
    fun `fechaOrdenable prefiere fechaMillis`() {
        assertEquals(1234L, Fechas.fechaOrdenable(1234L, "01/01/2000 00:00"))
    }

    @Test
    fun `fechaOrdenable interpreta registros antiguos sin fechaMillis`() {
        val millis = Fechas.fechaOrdenable(0L, "31/12/2020 23:59")

        assertTrue(millis > 0L)
    }

    @Test
    fun `fechaOrdenable devuelve cero si el texto no es valido`() {
        assertEquals(0L, Fechas.fechaOrdenable(0L, "fecha inválida"))
        assertEquals(0L, Fechas.fechaOrdenable(0L, ""))
    }

    @Test
    fun `formatear y parsear conservan el minuto`() {
        val millis = 1_700_000_000_123L
        val texto = Fechas.formatear(millis)

        val diferencia = kotlin.math.abs(Fechas.parsear(texto) - millis)
        assertTrue(diferencia < 60_000L)
    }
}

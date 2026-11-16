package com.example.controldegastos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidadorTest {

    @Test
    fun `parsea montos con punto decimal`() {
        assertEquals(12.5, Validador.parsearMonto("12.5")!!, 0.0001)
    }

    @Test
    fun `parsea montos con coma decimal`() {
        assertEquals(12.5, Validador.parsearMonto("12,5")!!, 0.0001)
    }

    @Test
    fun `parsea montos con separador de miles`() {
        assertEquals(1234.56, Validador.parsearMonto("1.234,56")!!, 0.0001)
    }

    @Test
    fun `ignora el simbolo de moneda`() {
        assertEquals(99.99, Validador.parsearMonto("\$ 99.99")!!, 0.0001)
    }

    @Test
    fun `rechaza textos que no son numeros`() {
        assertNull(Validador.parsearMonto("abc"))
        assertNull(Validador.parsearMonto(""))
        assertNull(Validador.parsearMonto("Infinity"))
    }

    @Test
    fun `valida un gasto correcto`() {
        assertTrue(Validador.validarGasto("Combustible", "25.50", "Transporte").esValido)
    }

    @Test
    fun `rechaza gasto con titulo vacio`() {
        val resultado = Validador.validarGasto("  ", "25.50", "Transporte")
        assertEquals(ErrorValidacion.TITULO_VACIO, resultado.error)
    }

    @Test
    fun `rechaza gasto con titulo demasiado largo`() {
        val resultado = Validador.validarGasto("a".repeat(Validador.TITULO_MAXIMO + 1), "1", "Otros")
        assertEquals(ErrorValidacion.TITULO_LARGO, resultado.error)
    }

    @Test
    fun `rechaza gasto con monto no positivo`() {
        assertEquals(
            ErrorValidacion.MONTO_NO_POSITIVO,
            Validador.validarGasto("Almuerzo", "0", "Alimentación").error
        )
        assertEquals(
            ErrorValidacion.MONTO_NO_POSITIVO,
            Validador.validarGasto("Almuerzo", "-5", "Alimentación").error
        )
    }

    @Test
    fun `rechaza gasto con monto invalido`() {
        assertEquals(
            ErrorValidacion.MONTO_INVALIDO,
            Validador.validarGasto("Almuerzo", "mucho", "Alimentación").error
        )
    }

    @Test
    fun `rechaza gasto sin categoria`() {
        assertEquals(
            ErrorValidacion.CATEGORIA_VACIA,
            Validador.validarGasto("Almuerzo", "5", " ").error
        )
    }

    @Test
    fun `valida un ingreso correcto`() {
        assertTrue(Validador.validarIngreso("Quincena", "500", "Salario").esValido)
    }

    @Test
    fun `rechaza ingreso sin concepto`() {
        assertEquals(
            ErrorValidacion.CONCEPTO_VACIO,
            Validador.validarIngreso("", "500", "Salario").error
        )
    }

    @Test
    fun `valida correos electronicos`() {
        assertTrue(Validador.esEmailValido("usuario@dominio.com"))
        assertTrue(Validador.esEmailValido("nombre.apellido+tag@sub.dominio.org"))
        assertFalse(Validador.esEmailValido("usuario"))
        assertFalse(Validador.esEmailValido("usuario@dominio"))
        assertFalse(Validador.esEmailValido("@dominio.com"))
    }

    @Test
    fun `valida la longitud minima de la contrasena`() {
        assertFalse(Validador.validarContrasena("12345").esValido)
        assertTrue(Validador.validarContrasena("123456").esValido)
    }

    @Test
    fun `valida la confirmacion de contrasena`() {
        assertTrue(Validador.validarConfirmacion("secreto", "secreto").esValido)
        assertEquals(
            ErrorValidacion.CONTRASENAS_NO_COINCIDEN,
            Validador.validarConfirmacion("secreto", "otro").error
        )
    }
}

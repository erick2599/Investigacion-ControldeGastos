package com.example.controldegastos.domain

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manejo de fechas de los movimientos.
 *
 * Se conserva el texto `dd/MM/yyyy HH:mm` por compatibilidad con los registros
 * existentes y se agrega `fechaMillis` para ordenar correctamente.
 */
object Fechas {

    private const val PATRON = "dd/MM/yyyy HH:mm"

    data class Sello(val texto: String, val millis: Long)

    fun ahora(): Sello {
        val millis = System.currentTimeMillis()
        return Sello(texto = formatear(millis), millis = millis)
    }

    fun formatear(millis: Long): String =
        SimpleDateFormat(PATRON, Locale.getDefault()).format(Date(millis))

    fun parsear(texto: String): Long {
        if (texto.isBlank()) return 0L
        return runCatching {
            SimpleDateFormat(PATRON, Locale.getDefault()).parse(texto)?.time ?: 0L
        }.getOrDefault(0L)
    }

    /**
     * Devuelve la fecha ordenable de un movimiento. Si es un registro nuevo usa
     * `fechaMillis`; si es antiguo intenta interpretar el texto guardado.
     */
    fun fechaOrdenable(fechaMillis: Long, fechaTexto: String): Long =
        if (fechaMillis > 0L) fechaMillis else parsear(fechaTexto)
}

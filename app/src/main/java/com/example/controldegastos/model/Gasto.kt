package com.example.controldegastos.model

/**
 * Gasto almacenado en Firestore.
 *
 * Se conserva [monto] como Double por compatibilidad con registros antiguos,
 * pero todos los cálculos y el formato se hacen en centavos (ver `Dinero`).
 * [fechaMillis] permite ordenar cronológicamente sin depender del texto.
 */
data class Gasto(
    val id: String = "",
    val titulo: String = "",
    val monto: Double = 0.0,
    val categoriaLaboral: String = "",
    val fecha: String = "",
    val fechaMillis: Long = 0L,
    val rutaFotoRecibo: String? = null,
    val rutaReciboStorage: String? = null
)

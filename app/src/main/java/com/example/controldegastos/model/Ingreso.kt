package com.example.controldegastos.model

/**
 * Ingreso almacenado en Firestore.
 *
 * [monto] se mantiene como Double por compatibilidad; los cálculos se hacen
 * en centavos. [fechaMillis] permite ordenar cronológicamente.
 */
data class Ingreso(
    val id: String = "",
    val concepto: String = "",
    val monto: Double = 0.0,
    val categoria: String = "",
    val fecha: String = "",
    val fechaMillis: Long = 0L
)

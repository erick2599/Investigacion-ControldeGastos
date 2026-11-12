package com.example.controldegastos.model

data class Ingreso(
    val id: String = "",
    val concepto: String = "",
    val monto: Double = 0.0,
    val categoria: String = "",
    val fecha: String = ""
)

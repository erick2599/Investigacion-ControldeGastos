package com.example.controldegastos.model

data class Ingreso(
    val id: String,
    val concepto: String,
    val monto: Double,
    val categoria: String, // "Salario", "Bono", "Remesa", "Otros"
    val fecha: String
)

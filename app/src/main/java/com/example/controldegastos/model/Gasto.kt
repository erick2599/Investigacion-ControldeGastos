package com.example.controldegastos.model

data class Gasto(
    val id: String = "",
    val titulo: String = "",
    val monto: Double = 0.0,
    val categoriaLaboral: String = "",
    val fecha: String = "",
    val rutaFotoRecibo: String? = null
)

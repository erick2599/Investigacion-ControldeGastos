package com.example.controldegastos.model

data class Gasto(
    val id: String,
    val titulo: String,            // Ej: "Combustible camión de entregas"
    val monto: Double,             // Ej: 45.50
    val categoriaLaboral: String,  // Ej: "Transporte", "Materiales", "Viáticos"
    val fecha: String,             // Ej: "15/10/2026"
    val rutaFotoRecibo: String? = null // Ubicación en memoria de la foto del recibo
)

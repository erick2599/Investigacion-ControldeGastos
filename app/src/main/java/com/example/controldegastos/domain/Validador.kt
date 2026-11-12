package com.example.controldegastos.domain

/** Errores de validación que la UI traduce a mensajes de texto. */
enum class ErrorValidacion {
    TITULO_VACIO,
    TITULO_LARGO,
    MONTO_INVALIDO,
    MONTO_NO_POSITIVO,
    MONTO_EXCESIVO,
    CATEGORIA_VACIA,
    CONCEPTO_VACIO,
    EMAIL_INVALIDO,
    CONTRASENA_CORTA,
    CONTRASENAS_NO_COINCIDEN
}

data class ResultadoValidacion(val error: ErrorValidacion? = null) {
    val esValido: Boolean get() = error == null
}

/**
 * Validaciones puras (sin Android) para poder probarlas en la JVM.
 */
object Validador {

    const val MONTO_MAXIMO = 1_000_000_000.0
    const val TITULO_MAXIMO = 120
    const val CONCEPTO_MAXIMO = 120
    const val CONTRASENA_MINIMA = 6

    private val EMAIL_REGEX =
        Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    /**
     * Convierte un texto de monto a Double aceptando coma o punto decimal y
     * separadores de miles simples. Devuelve null si no es un número válido.
     */
    fun parsearMonto(texto: String): Double? {
        val limpio = texto.trim().replace("$", "").replace(" ", "")
        if (limpio.isEmpty()) return null

        val normalizado = when {
            limpio.count { it == ',' } == 1 && limpio.count { it == '.' } == 0 ->
                limpio.replace(',', '.')
            limpio.count { it == ',' } == 1 && limpio.count { it == '.' } >= 1 ->
                limpio.replace(".", "").replace(',', '.')
            else -> limpio
        }

        val valor = normalizado.toDoubleOrNull() ?: return null
        return if (valor.isFinite()) valor else null
    }

    fun validarGasto(titulo: String, montoTexto: String, categoria: String): ResultadoValidacion {
        if (titulo.isBlank()) return ResultadoValidacion(ErrorValidacion.TITULO_VACIO)
        if (titulo.length > TITULO_MAXIMO) return ResultadoValidacion(ErrorValidacion.TITULO_LARGO)

        val monto = parsearMonto(montoTexto)
            ?: return ResultadoValidacion(ErrorValidacion.MONTO_INVALIDO)
        if (monto <= 0.0) return ResultadoValidacion(ErrorValidacion.MONTO_NO_POSITIVO)
        if (monto > MONTO_MAXIMO) return ResultadoValidacion(ErrorValidacion.MONTO_EXCESIVO)

        if (categoria.isBlank()) return ResultadoValidacion(ErrorValidacion.CATEGORIA_VACIA)
        return ResultadoValidacion()
    }

    fun validarIngreso(concepto: String, montoTexto: String, categoria: String): ResultadoValidacion {
        if (concepto.isBlank()) return ResultadoValidacion(ErrorValidacion.CONCEPTO_VACIO)
        if (concepto.length > CONCEPTO_MAXIMO) return ResultadoValidacion(ErrorValidacion.CONCEPTO_VACIO)

        val monto = parsearMonto(montoTexto)
            ?: return ResultadoValidacion(ErrorValidacion.MONTO_INVALIDO)
        if (monto <= 0.0) return ResultadoValidacion(ErrorValidacion.MONTO_NO_POSITIVO)
        if (monto > MONTO_MAXIMO) return ResultadoValidacion(ErrorValidacion.MONTO_EXCESIVO)

        if (categoria.isBlank()) return ResultadoValidacion(ErrorValidacion.CATEGORIA_VACIA)
        return ResultadoValidacion()
    }

    fun esEmailValido(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

    fun validarContrasena(contrasena: String): ResultadoValidacion =
        if (contrasena.length < CONTRASENA_MINIMA) {
            ResultadoValidacion(ErrorValidacion.CONTRASENA_CORTA)
        } else {
            ResultadoValidacion()
        }

    fun validarConfirmacion(contrasena: String, confirmacion: String): ResultadoValidacion =
        if (contrasena != confirmacion) {
            ResultadoValidacion(ErrorValidacion.CONTRASENAS_NO_COINCIDEN)
        } else {
            ResultadoValidacion()
        }
}

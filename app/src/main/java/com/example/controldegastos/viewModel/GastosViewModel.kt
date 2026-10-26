package com.example.controldegastos.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.controldegastos.model.Gasto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Estado único que consume la pantalla principal.
 * Se recalcula automáticamente cada vez que cambia la lista de gastos
 * o el presupuesto.
 */
data class GastosUiState(
    val presupuesto: Double = 0.0,
    val gastos: List<Gasto> = emptyList(),
    val totalGastado: Double = 0.0,
    val disponible: Double = 0.0
)

class GastosViewModel : ViewModel() {

    // Presupuesto base temporal para pruebas (puedes cambiarlo después)
    private val _presupuestoInicial = MutableStateFlow(1200.0)
    val presupuestoInicial: StateFlow<Double> = _presupuestoInicial

    // Lista reactiva que contendrá los gastos que el usuario registre
    private val _listaGastos = MutableStateFlow<List<Gasto>>(emptyList())
    val listaGastos: StateFlow<List<Gasto>> = _listaGastos

    val uiState: StateFlow<GastosUiState> =
        combine(_listaGastos, _presupuestoInicial) { gastos, presupuesto ->
            val total = gastos.sumOf { it.monto }
            GastosUiState(
                presupuesto = presupuesto,
                gastos = gastos,
                totalGastado = total,
                disponible = presupuesto - total
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GastosUiState(presupuesto = _presupuestoInicial.value)
        )

    // Añade un nuevo gasto al inicio de la lista
    fun agregarGasto(gasto: Gasto) {
        _listaGastos.value = listOf(gasto) + _listaGastos.value
    }

    // Elimina un gasto por su identificador
    fun eliminarGasto(id: String) {
        _listaGastos.value = _listaGastos.value.filterNot { it.id == id }
    }

    // Permite cambiar el presupuesto (queda listo para una pantalla de ajustes)
    fun establecerPresupuesto(monto: Double) {
        if (monto > 0) _presupuestoInicial.value = monto
    }

    fun obtenerTotalGastado(): Double = _listaGastos.value.sumOf { it.monto }

    fun obtenerDineroDisponible(): Double = _presupuestoInicial.value - obtenerTotalGastado()
}

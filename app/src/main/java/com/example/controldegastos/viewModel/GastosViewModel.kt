package com.example.controldegastos.viewModel

import androidx.lifecycle.ViewModel
import com.example.controldegastos.model.Gasto
import kotlinx.flow.MutableStateFlow
import kotlinx.flow.StateFlow



class GastosViewModel : ViewModel() {

    // Asignamos un presupuesto base temporal para pruebas (puedes cambiarlo después)
    private val _presupuestoInicial = MutableStateFlow(1200.0)
    val presupuestoInicial: StateFlow<Double> = _presupuestoInicial

    // Lista reactiva que contendrá los gastos que el usuario registre
    private val _listaGastos = MutableStateFlow<List<Gasto>>(emptyList())
    val listaGastos: StateFlow<List<Gasto>> = _listaGastos

    // Añade un nuevo gasto a la lista existente
    fun agregarGasto(gasto: Gasto) {
        val listaActual = _listaGastos.value.toMutableList()
        listaActual.add(gasto)
        _listaGastos.value = listaActual
    }

    // Suma todos los montos acumulados
    fun obtenerTotalGastado(): Double {
        return _listaGastos.value.sumOf { it.monto }
    }

    // Resta el dinero gastado del presupuesto original de la actividad laboral
    fun obtenerDineroDisponible(): Double {
        return _presupuestoInicial.value - obtenerTotalGastado()
    }
}

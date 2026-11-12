package com.example.controldegastos.viewModel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.controldegastos.data.FirebaseGastosRepository
import com.example.controldegastos.data.GastosRepository
import com.example.controldegastos.domain.BalanceCalculator
import com.example.controldegastos.model.Gasto
import com.example.controldegastos.model.Ingreso
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GastosUiState(
    val presupuestoCentavos: Long = 0L,
    val gastos: List<Gasto> = emptyList(),
    val totalGastadoCentavos: Long = 0L,
    val disponibleCentavos: Long = 0L,
    val cargando: Boolean = false,
    val guardando: Boolean = false
)

sealed interface UiEvent {
    data object GastoGuardado : UiEvent
    data object IngresoGuardado : UiEvent
    data object GastoEliminado : UiEvent
    data object ErrorGuardarGasto : UiEvent
    data object ErrorGuardarIngreso : UiEvent
    data object ErrorEliminarGasto : UiEvent
    data object ErrorCargarDatos : UiEvent
}

class GastosViewModel(
    private val repositorio: GastosRepository
) : ViewModel() {

    private val _guardando = MutableStateFlow(false)
    private val _eventos = MutableSharedFlow<UiEvent>(extraBufferCapacity = 4)
    val eventos: SharedFlow<UiEvent> = _eventos.asSharedFlow()

    private val _nombreUsuario = MutableStateFlow<String?>(null)
    val nombreUsuario: StateFlow<String?> = _nombreUsuario

    val uiState: StateFlow<GastosUiState> = combine(
        repositorio.observarGastos().conManejoDeError(),
        repositorio.observarIngresos().conManejoDeError(),
        _guardando
    ) { gastos, ingresos, guardando ->
        val balance = BalanceCalculator.calcular(gastos, ingresos)
        GastosUiState(
            presupuestoCentavos = balance.presupuestoCentavos,
            gastos = gastos,
            totalGastadoCentavos = balance.totalGastadoCentavos,
            disponibleCentavos = balance.disponibleCentavos,
            cargando = false,
            guardando = guardando
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GastosUiState(cargando = true)
    )

    init {
        viewModelScope.launch {
            repositorio.migrarDatosLegacy().onFailure {
                Log.w(TAG, "No se pudo migrar la ruta antigua por email", it)
            }
            _nombreUsuario.value = repositorio.obtenerNombreUsuario()
        }
    }

    fun agregarGasto(gasto: Gasto, reciboUri: Uri?) {
        if (_guardando.value) return
        viewModelScope.launch {
            _guardando.value = true
            repositorio.agregarGasto(gasto, reciboUri)
                .onSuccess { _eventos.emit(UiEvent.GastoGuardado) }
                .onFailure { _eventos.emit(UiEvent.ErrorGuardarGasto) }
            _guardando.value = false
        }
    }

    fun agregarIngreso(ingreso: Ingreso) {
        if (_guardando.value) return
        viewModelScope.launch {
            _guardando.value = true
            repositorio.agregarIngreso(ingreso)
                .onSuccess { _eventos.emit(UiEvent.IngresoGuardado) }
                .onFailure { _eventos.emit(UiEvent.ErrorGuardarIngreso) }
            _guardando.value = false
        }
    }

    fun eliminarGasto(gasto: Gasto) {
        if (_guardando.value) return
        viewModelScope.launch {
            _guardando.value = true
            repositorio.eliminarGasto(gasto)
                .onSuccess { _eventos.emit(UiEvent.GastoEliminado) }
                .onFailure { _eventos.emit(UiEvent.ErrorEliminarGasto) }
            _guardando.value = false
        }
    }

    private fun <T> Flow<List<T>>.conManejoDeError(): Flow<List<T>> = catch { error ->
        if (error is CancellationException) throw error
        Log.e(TAG, "No se pudieron cargar los datos", error)
        _eventos.tryEmit(UiEvent.ErrorCargarDatos)
        emit(emptyList())
    }

    companion object {
        private const val TAG = "GastosViewModel"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                GastosViewModel(FirebaseGastosRepository())
            }
        }
    }
}

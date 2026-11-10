package com.example.controldegastos.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.controldegastos.model.Gasto
import com.example.controldegastos.model.Ingreso
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class GastosUiState(
    val presupuesto: Double = 0.0,
    val gastos: List<Gasto> = emptyList(),
    val totalGastado: Double = 0.0,
    val disponible: Double = 0.0
)

class GastosViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _listaIngresos = MutableStateFlow<List<Ingreso>>(emptyList())
    val listaIngresos: StateFlow<List<Ingreso>> = _listaIngresos

    private val _listaGastos = MutableStateFlow<List<Gasto>>(emptyList())
    val listaGastos: StateFlow<List<Gasto>> = _listaGastos

    // El uiState unificado calcula automáticamente las operaciones numéricas en tiempo real
    val uiState: StateFlow<GastosUiState> =
        combine(_listaGastos, _listaIngresos) { gastos, ingresos ->
            val totalIngresos = ingresos.sumOf { it.monto }
            val totalGastos = gastos.sumOf { it.monto }
            GastosUiState(
                presupuesto = totalIngresos,
                gastos = gastos,
                totalGastado = totalGastos,
                disponible = totalIngresos - totalGastos
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GastosUiState()
        )

    init {
        // En cuanto el ViewModel se crea en la pantalla principal, empezamos a escuchar Firebase
        escucharDatosDesdeFirebase()
    }

    /**
     * Escucha en tiempo real (Realtime Listeners) las colecciones de Firebase.
     * Cualquier cambio en la nube se reflejará en la tablet al instante.
     */
    private fun escucharDatosDesdeFirebase() {
        val emailUsuario = auth.currentUser?.email ?: return

        // 1. Escuchar Historial de Gastos
        db.collection("usuarios").document(emailUsuario).collection("gastos")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val listaDocumentos = snapshot.toObjects(Gasto::class.java)
                    // Ordenamos cronológicamente si es necesario o los enviamos directo
                    _listaGastos.value = listaDocumentos
                }
            }

        // 2. Escuchar Historial de Ingresos
        db.collection("usuarios").document(emailUsuario).collection("ingresos")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val listaDocumentos = snapshot.toObjects(Ingreso::class.java)
                    _listaIngresos.value = listaDocumentos
                }
            }
    }

    /**
     * Guarda un nuevo gasto en una subcolección dentro del documento del usuario.
     */
    fun agregarGasto(gasto: Gasto) {
        val emailUsuario = auth.currentUser?.email ?: return

        db.collection("usuarios").document(emailUsuario).collection("gastos")
            .document(gasto.id)
            .set(gasto)
    }

    /**
     * Guarda un nuevo ingreso de forma acumulativa en la subcolección correspondiente.
     */
    fun agregarIngreso(ingreso: Ingreso) {
        val emailUsuario = auth.currentUser?.email ?: return

        db.collection("usuarios").document(emailUsuario).collection("ingresos")
            .document(ingreso.id)
            .set(ingreso)
    }

    /**
     * Elimina físicamente un gasto de la nube usando su identificador único.
     */
    fun eliminarGasto(id: String) {
        val emailUsuario = auth.currentUser?.email ?: return

        db.collection("usuarios").document(emailUsuario).collection("gastos")
            .document(id)
            .delete()
    }

    fun obtenerTotalGastado(): Double = _listaGastos.value.sumOf { it.monto }
    fun obtenerPresupuestoTotal(): Double = _listaIngresos.value.sumOf { it.monto }
    fun obtenerDineroDisponible(): Double = obtenerPresupuestoTotal() - obtenerTotalGastado()
}

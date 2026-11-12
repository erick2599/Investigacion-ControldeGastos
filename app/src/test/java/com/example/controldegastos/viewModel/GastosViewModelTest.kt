package com.example.controldegastos.viewModel

import android.net.Uri
import com.example.controldegastos.MainDispatcherRule
import com.example.controldegastos.data.GastosRepository
import com.example.controldegastos.model.Gasto
import com.example.controldegastos.model.Ingreso
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GastosViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repositorio = FakeGastosRepository()

    @Test
    fun `calcula el balance en centavos`() = runTest {
        repositorio.ingresosFlow.value = listOf(Ingreso(id = "i1", monto = 100.0))
        repositorio.gastosFlow.value = listOf(
            Gasto(id = "g1", monto = 30.50),
            Gasto(id = "g2", monto = 0.25)
        )

        val viewModel = GastosViewModel(repositorio)
        val coleccion = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertEquals(10_000L, estado.presupuestoCentavos)
        assertEquals(3_075L, estado.totalGastadoCentavos)
        assertEquals(6_925L, estado.disponibleCentavos)
        assertEquals(2, estado.gastos.size)

        coleccion.cancel()
    }

    @Test
    fun `emite evento al guardar un gasto`() = runTest {
        val eventos = mutableListOf<UiEvent>()
        val viewModel = GastosViewModel(repositorio)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.eventos.collect { eventos.add(it) }
        }

        viewModel.agregarGasto(Gasto(id = "g1", titulo = "Café", monto = 2.5), null)
        advanceUntilIdle()

        assertEquals(listOf(UiEvent.GastoGuardado), eventos)
        assertEquals(1, repositorio.gastosGuardados.size)
    }

    @Test
    fun `emite error cuando falla el guardado`() = runTest {
        repositorio.fallarEscritura = true
        val eventos = mutableListOf<UiEvent>()
        val viewModel = GastosViewModel(repositorio)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.eventos.collect { eventos.add(it) }
        }

        viewModel.agregarGasto(Gasto(id = "g1", monto = 1.0), null)
        advanceUntilIdle()

        assertEquals(listOf(UiEvent.ErrorGuardarGasto), eventos)
        assertTrue(repositorio.gastosGuardados.isEmpty())
    }

    @Test
    fun `emite evento al agregar un ingreso`() = runTest {
        val eventos = mutableListOf<UiEvent>()
        val viewModel = GastosViewModel(repositorio)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.eventos.collect { eventos.add(it) }
        }

        viewModel.agregarIngreso(Ingreso(id = "i1", concepto = "Quincena", monto = 500.0))
        advanceUntilIdle()

        assertEquals(listOf(UiEvent.IngresoGuardado), eventos)
        assertEquals(1, repositorio.ingresosGuardados.size)
    }

    @Test
    fun `emite evento al eliminar un gasto`() = runTest {
        val eventos = mutableListOf<UiEvent>()
        val gasto = Gasto(id = "g1", titulo = "Café", monto = 2.5)
        val viewModel = GastosViewModel(repositorio)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.eventos.collect { eventos.add(it) }
        }

        viewModel.eliminarGasto(gasto)
        advanceUntilIdle()

        assertEquals(listOf(UiEvent.GastoEliminado), eventos)
        assertEquals(listOf(gasto), repositorio.gastosEliminados)
    }

    @Test
    fun `carga el nombre del usuario y migra datos antiguos al iniciar`() = runTest {
        val viewModel = GastosViewModel(repositorio)
        advanceUntilIdle()

        assertEquals("Ana", viewModel.nombreUsuario.value)
        assertTrue(repositorio.migracionEjecutada)
    }

    private class FakeGastosRepository : GastosRepository {

        val gastosFlow = MutableStateFlow<List<Gasto>>(emptyList())
        val ingresosFlow = MutableStateFlow<List<Ingreso>>(emptyList())
        val gastosGuardados = mutableListOf<Gasto>()
        val ingresosGuardados = mutableListOf<Ingreso>()
        val gastosEliminados = mutableListOf<Gasto>()
        var fallarEscritura = false
        var migracionEjecutada = false
        var nombreUsuario: String? = "Ana"

        override fun observarGastos(): Flow<List<Gasto>> = gastosFlow

        override fun observarIngresos(): Flow<List<Ingreso>> = ingresosFlow

        override suspend fun obtenerNombreUsuario(): String? = nombreUsuario

        override suspend fun agregarGasto(gasto: Gasto, reciboUri: Uri?): Result<Gasto> {
            if (fallarEscritura) return Result.failure(IllegalStateException("fallo simulado"))
            gastosGuardados += gasto
            gastosFlow.value = gastosFlow.value + gasto
            return Result.success(gasto)
        }

        override suspend fun agregarIngreso(ingreso: Ingreso): Result<Unit> {
            if (fallarEscritura) return Result.failure(IllegalStateException("fallo simulado"))
            ingresosGuardados += ingreso
            ingresosFlow.value = ingresosFlow.value + ingreso
            return Result.success(Unit)
        }

        override suspend fun eliminarGasto(gasto: Gasto): Result<Unit> {
            if (fallarEscritura) return Result.failure(IllegalStateException("fallo simulado"))
            gastosEliminados += gasto
            gastosFlow.value = gastosFlow.value.filterNot { it.id == gasto.id }
            return Result.success(Unit)
        }

        override suspend fun migrarDatosLegacy(): Result<Unit> {
            migracionEjecutada = true
            return Result.success(Unit)
        }
    }
}

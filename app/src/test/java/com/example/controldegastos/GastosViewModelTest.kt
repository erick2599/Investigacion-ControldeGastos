package com.example.controldegastos

import com.example.controldegastos.model.Gasto
import com.example.controldegastos.viewModel.GastosViewModel
import org.junit.Assert.assertEquals
import org.junit.Test

class GastosViewModelTest {

    private fun gasto(id: String, monto: Double) = Gasto(
        id = id,
        titulo = "Gasto $id",
        monto = monto,
        categoriaLaboral = "Operaciones",
        fecha = "15/10/2026"
    )

    @Test
    fun sinGastos_totalEsCeroYDisponibleEsElPresupuesto() {
        val vm = GastosViewModel()

        assertEquals(0.0, vm.obtenerTotalGastado(), 0.001)
        assertEquals(1200.0, vm.obtenerDineroDisponible(), 0.001)
    }

    @Test
    fun agregarGasto_acumulaElTotalYDescuentaDelPresupuesto() {
        val vm = GastosViewModel()

        vm.agregarGasto(gasto("1", 45.50))
        vm.agregarGasto(gasto("2", 75.50))

        assertEquals(2, vm.listaGastos.value.size)
        assertEquals(121.0, vm.obtenerTotalGastado(), 0.001)
        assertEquals(1079.0, vm.obtenerDineroDisponible(), 0.001)
    }
}

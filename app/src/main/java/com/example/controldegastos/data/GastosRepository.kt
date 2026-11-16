package com.example.controldegastos.data

import android.net.Uri
import com.example.controldegastos.model.Gasto
import com.example.controldegastos.model.Ingreso
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de acceso a datos. La implementación real usa Firebase, pero al
 * depender de esta interfaz el ViewModel se puede probar con un fake.
 */
interface GastosRepository {

    fun observarGastos(): Flow<List<Gasto>>

    fun observarIngresos(): Flow<List<Ingreso>>

    suspend fun obtenerNombreUsuario(): String?

    suspend fun agregarGasto(gasto: Gasto, reciboUri: Uri?): Result<Gasto>

    suspend fun actualizarGasto(gasto: Gasto, reciboUri: Uri?): Result<Gasto>

    suspend fun agregarIngreso(ingreso: Ingreso): Result<Unit>

    suspend fun actualizarIngreso(ingreso: Ingreso): Result<Unit>

    suspend fun eliminarGasto(gasto: Gasto): Result<Unit>

    suspend fun eliminarIngreso(ingreso: Ingreso): Result<Unit>

    /** Copia los datos antiguos guardados por email a la ruta segura por UID. */
    suspend fun migrarDatosLegacy(): Result<Unit>
}

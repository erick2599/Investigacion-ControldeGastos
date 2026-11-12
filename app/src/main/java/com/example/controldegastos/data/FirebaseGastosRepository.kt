package com.example.controldegastos.data

import android.net.Uri
import android.util.Log
import com.example.controldegastos.domain.Fechas
import com.example.controldegastos.model.Gasto
import com.example.controldegastos.model.Ingreso
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Implementación de Firestore + Storage.
 *
 * Reglas de diseño:
 * - Los documentos viven bajo `usuarios/{uid}` (no bajo el email).
 * - Los listeners se cancelan con `awaitClose`, evitando fugas.
 * - Los recibos se suben a Storage y se guarda la URL de descarga.
 */
class FirebaseGastosRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) : GastosRepository {

    override fun observarGastos(): Flow<List<Gasto>> =
        observarColeccion(
            nombre = "gastos",
            convertir = { doc ->
                doc.toObject(Gasto::class.java)?.copy(id = doc.id)
            }
        ).map { gastos ->
            gastos.sortedByDescending { Fechas.fechaOrdenable(it.fechaMillis, it.fecha) }
        }

    override fun observarIngresos(): Flow<List<Ingreso>> =
        observarColeccion(
            nombre = "ingresos",
            convertir = { doc ->
                doc.toObject(Ingreso::class.java)?.copy(id = doc.id)
            }
        ).map { ingresos ->
            ingresos.sortedByDescending { Fechas.fechaOrdenable(it.fechaMillis, it.fecha) }
        }

    override suspend fun obtenerNombreUsuario(): String? = resultado {
        val referencia = usuarioRef() ?: return@resultado auth.currentUser?.displayName
        val documento = referencia.get().await()
        if (documento.exists()) {
            documento.getString("nombre")?.takeIf { it.isNotBlank() }
                ?: auth.currentUser?.displayName
        } else {
            auth.currentUser?.displayName
        }
    }.getOrNull()

    override suspend fun agregarGasto(gasto: Gasto, reciboUri: Uri?): Result<Gasto> = resultado {
        val usuario = usuarioRef() ?: error("No hay una sesión activa")
        val uid = auth.currentUser?.uid ?: error("No hay una sesión activa")

        var reciboSubido: String? = null
        var gastoFinal = gasto

        try {
            if (reciboUri != null) {
                val reciboRef = storage.reference
                    .child("usuarios/$uid/recibos/${gasto.id}.jpg")
                reciboRef.putFile(reciboUri).await()
                val url = reciboRef.downloadUrl.await().toString()
                reciboSubido = reciboRef.path
                gastoFinal = gasto.copy(
                    rutaFotoRecibo = url,
                    rutaReciboStorage = reciboRef.path
                )
            }

            usuario.collection("gastos")
                .document(gasto.id)
                .set(gastoFinal)
                .await()
            gastoFinal
        } catch (e: Exception) {
            // Si la escritura falla después de subir la foto, no dejamos huérfanos.
            reciboSubido?.let { ruta ->
                runCatching { storage.reference.child(ruta).delete().await() }
            }
            throw e
        }
    }

    override suspend fun agregarIngreso(ingreso: Ingreso): Result<Unit> = resultado {
        val usuario = usuarioRef() ?: error("No hay una sesión activa")
        usuario.collection("ingresos")
            .document(ingreso.id)
            .set(ingreso)
            .await()
    }

    override suspend fun eliminarGasto(gasto: Gasto): Result<Unit> = resultado {
        val usuario = usuarioRef() ?: error("No hay una sesión activa")
        usuario.collection("gastos")
            .document(gasto.id)
            .delete()
            .await()

        gasto.rutaReciboStorage?.takeIf { it.isNotBlank() }?.let { ruta ->
            runCatching { storage.reference.child(ruta).delete().await() }
        }
    }

    /**
     * Migración única desde `usuarios/{email}` a `usuarios/{uid}`.
     *
     * Se copian primero todas las subcolecciones, después el perfil y solo al
     * final se borra la ruta antigua. Si algo falla, el proceso es idempotente
     * y se puede reintentar en el siguiente inicio de sesión.
     */
    override suspend fun migrarDatosLegacy(): Result<Unit> = resultado {
        val usuario = auth.currentUser ?: return@resultado
        val uid = usuario.uid
        val email = usuario.email?.trim() ?: return@resultado
        if (uid == email) return@resultado

        val referenciaNueva = db.collection("usuarios").document(uid)
        val legacy = buscarPerfilLegacy(email) ?: return@resultado
        val referenciaLegacy = legacy.first
        val perfilLegacy = legacy.second

        copiarColeccion(
            origen = referenciaLegacy.collection("gastos"),
            destino = referenciaNueva.collection("gastos")
        )
        copiarColeccion(
            origen = referenciaLegacy.collection("ingresos"),
            destino = referenciaNueva.collection("ingresos")
        )

        // Se combina con lo que ya exista en la ruta nueva. El perfil se escribe
        // al final para que un fallo no marque la migración como completada
        // antes de copiar los movimientos.
        referenciaNueva.set(perfilLegacy.data.orEmpty(), SetOptions.merge()).await()

        eliminarColeccion(referenciaLegacy.collection("gastos"))
        eliminarColeccion(referenciaLegacy.collection("ingresos"))
        referenciaLegacy.delete().await()
    }

    /**
     * La versión antigua usaba el email tal como se escribió, sin normalizar.
     * Se prueban las dos variantes para no perder datos si había mayúsculas.
     */
    private suspend fun buscarPerfilLegacy(
        email: String
    ): Pair<DocumentReference, DocumentSnapshot>? {
        for (candidato in listOf(email, email.lowercase()).distinct()) {
            val referencia = db.collection("usuarios").document(candidato)
            val documento = referencia.get().await()
            if (documento.exists()) return referencia to documento
        }
        return null
    }

    private fun <T> observarColeccion(
        nombre: String,
        convertir: (DocumentSnapshot) -> T?
    ): Flow<List<T>> = callbackFlow {
        val usuario = usuarioRef()
        if (usuario == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val registro = usuario.collection(nombre)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val lista = snapshot?.documents?.mapNotNull(convertir).orEmpty()
                trySend(lista)
            }
        awaitClose { registro.remove() }
    }

    private suspend fun copiarColeccion(
        origen: CollectionReference,
        destino: CollectionReference,
        tamanoLote: Long = 400
    ) {
        var ultimo: DocumentSnapshot? = null
        while (true) {
            var consulta = origen.orderBy(FieldPath.documentId()).limit(tamanoLote)
            if (ultimo != null) consulta = consulta.startAfter(ultimo)

            val documentos = consulta.get().await().documents
            if (documentos.isEmpty()) break

            val lote = db.batch()
            documentos.forEach { documento ->
                lote.set(destino.document(documento.id), documento.data.orEmpty())
            }
            lote.commit().await()

            ultimo = documentos.last()
            if (documentos.size < tamanoLote) break
        }
    }

    private suspend fun eliminarColeccion(
        coleccion: CollectionReference,
        tamanoLote: Long = 400
    ) {
        while (true) {
            val documentos = coleccion
                .orderBy(FieldPath.documentId())
                .limit(tamanoLote)
                .get()
                .await()
                .documents
            if (documentos.isEmpty()) break

            val lote = db.batch()
            documentos.forEach { lote.delete(it.reference) }
            lote.commit().await()
            if (documentos.size < tamanoLote) break
        }
    }

    private fun usuarioRef(): DocumentReference? {
        val uid = auth.currentUser?.uid ?: return null
        return db.collection("usuarios").document(uid)
    }

    private suspend fun <T> resultado(bloque: suspend () -> T): Result<T> =
        try {
            Result.success(bloque())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Error de Firebase", e)
            Result.failure(e)
        }

    private companion object {
        const val TAG = "GastosRepository"
    }
}

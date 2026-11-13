package com.example.controldegastos.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.controldegastos.R
import com.example.controldegastos.databinding.ActivityMainBinding
import com.example.controldegastos.databinding.DialogAgregarGastoBinding
import com.example.controldegastos.databinding.DialogAgregarIngresoBinding
import com.example.controldegastos.domain.Dinero
import com.example.controldegastos.domain.ErrorValidacion
import com.example.controldegastos.domain.Fechas
import com.example.controldegastos.domain.Validador
import com.example.controldegastos.model.Gasto
import com.example.controldegastos.model.Ingreso
import com.example.controldegastos.viewModel.GastosUiState
import com.example.controldegastos.viewModel.GastosViewModel
import com.example.controldegastos.viewModel.TipoMovimiento
import com.example.controldegastos.viewModel.UiEvent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var movimientosAdapter: MovimientosAdapter
    private val auth = FirebaseAuth.getInstance()
    private val viewModel: GastosViewModel by viewModels { GastosViewModel.Factory }

    private var uriReciboSeleccionado: Uri? = null
    private var actualizarTextoRecibo: ((String) -> Unit)? = null

    private val seleccionarFotoGaleria = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            uriReciboSeleccionado = uri
            actualizarTextoRecibo?.invoke(getString(R.string.recibo_cargado))
            Toast.makeText(this, R.string.recibo_cargado, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val usuario = auth.currentUser
        if (usuario == null || !usuario.isEmailVerified) {
            irALogin()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarLista()
        configurarAcciones()
        observarEstado()
    }

    private fun configurarLista() {
        movimientosAdapter = MovimientosAdapter(
            onEditar = ::editarMovimiento,
            onEliminar = ::confirmarEliminacion,
            onReciboClick = ::abrirRecibo
        )
        binding.rvGastos.layoutManager = LinearLayoutManager(this)
        binding.rvGastos.adapter = movimientosAdapter
    }

    private fun configurarAcciones() {
        val tipoInicial = viewModel.uiState.value.tipoSeleccionado
        binding.grupoTipo.check(
            if (tipoInicial == TipoMovimiento.GASTO) {
                R.id.btnFiltroGastos
            } else {
                R.id.btnFiltroIngresos
            }
        )
        binding.grupoTipo.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            viewModel.cambiarTipo(
                if (checkedId == R.id.btnFiltroGastos) {
                    TipoMovimiento.GASTO
                } else {
                    TipoMovimiento.INGRESO
                }
            )
        }

        binding.fabAgregarGasto.setOnClickListener {
            when (viewModel.uiState.value.tipoSeleccionado) {
                TipoMovimiento.GASTO -> mostrarDialogoAgregarGasto()
                TipoMovimiento.INGRESO -> mostrarDialogoAgregarIngreso()
            }
        }

        binding.btnCerrarSesion.setOnClickListener { confirmarCierreSesion() }
    }

    private fun observarEstado() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { estado -> renderizar(estado) }
                }
                launch {
                    viewModel.nombreUsuario.collect { nombre ->
                        binding.txtBienvenida.text = getString(
                            R.string.bienvenida_usuario,
                            nombre ?: getString(R.string.nombre_usuario_predeterminado)
                        )
                    }
                }
                launch {
                    viewModel.eventos.collect { evento -> mostrarEvento(evento) }
                }
            }
        }
    }

    private fun renderizar(estado: GastosUiState) {
        val esGasto = estado.tipoSeleccionado == TipoMovimiento.GASTO
        val movimientos: List<Movimiento> = if (esGasto) {
            estado.gastos.map { Movimiento.GastoItem(it) }
        } else {
            estado.ingresos.map { Movimiento.IngresoItem(it) }
        }

        binding.progressCargando.isVisible = estado.cargando
        binding.txtVacio.isVisible = !estado.cargando && movimientos.isEmpty()
        binding.txtVacio.text = getString(if (esGasto) R.string.sin_gastos else R.string.sin_ingresos)
        binding.lblHistorial.text =
            getString(if (esGasto) R.string.actividades_gastos else R.string.actividades_ingresos)
        binding.fabAgregarGasto.isEnabled = !estado.guardando
        binding.fabAgregarGasto.contentDescription =
            getString(if (esGasto) R.string.agregar_gasto else R.string.agregar_ingreso)

        binding.txtDineroDisponible.text = Dinero.formatear(estado.disponibleCentavos)
        binding.txtDineroDisponible.setTextColor(
            ContextCompat.getColor(
                this,
                if (estado.disponibleCentavos < 0) R.color.rojo_gasto else R.color.verde_primario
            )
        )
        binding.txtPresupuesto.text = Dinero.formatear(estado.presupuestoCentavos)
        binding.txtTotalGastado.text = Dinero.formatear(estado.totalGastadoCentavos)

        movimientosAdapter.submitList(movimientos)
    }

    private fun mostrarEvento(evento: UiEvent) {
        val mensaje = when (evento) {
            UiEvent.GastoGuardado -> R.string.gasto_guardado
            UiEvent.IngresoGuardado -> R.string.ingreso_guardado
            UiEvent.GastoActualizado -> R.string.gasto_actualizado
            UiEvent.IngresoActualizado -> R.string.ingreso_actualizado
            UiEvent.GastoEliminado -> R.string.gasto_eliminado
            UiEvent.IngresoEliminado -> R.string.ingreso_eliminado
            UiEvent.ErrorGuardarGasto -> R.string.error_guardar_gasto
            UiEvent.ErrorGuardarIngreso -> R.string.error_guardar_ingreso
            UiEvent.ErrorActualizarGasto -> R.string.error_actualizar_gasto
            UiEvent.ErrorActualizarIngreso -> R.string.error_actualizar_ingreso
            UiEvent.ErrorEliminarGasto -> R.string.error_eliminar_gasto
            UiEvent.ErrorEliminarIngreso -> R.string.error_eliminar_ingreso
            UiEvent.ErrorCargarDatos -> R.string.error_cargar_datos
        }
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }

    private fun editarMovimiento(movimiento: Movimiento) {
        when (movimiento) {
            is Movimiento.GastoItem -> mostrarDialogoAgregarGasto(movimiento.gasto)
            is Movimiento.IngresoItem -> mostrarDialogoAgregarIngreso(movimiento.ingreso)
        }
    }

    private fun confirmarEliminacion(movimiento: Movimiento) {
        when (movimiento) {
            is Movimiento.GastoItem -> confirmarEliminacion(
                tituloRes = R.string.eliminar_gasto,
                nombre = movimiento.gasto.titulo,
                alConfirmar = { viewModel.eliminarGasto(movimiento.gasto) }
            )

            is Movimiento.IngresoItem -> confirmarEliminacion(
                tituloRes = R.string.eliminar_ingreso,
                nombre = movimiento.ingreso.concepto,
                alConfirmar = { viewModel.eliminarIngreso(movimiento.ingreso) }
            )
        }
    }

    private fun confirmarEliminacion(
        tituloRes: Int,
        nombre: String,
        alConfirmar: () -> Unit
    ) {
        MaterialAlertDialogBuilder(this)
            .setTitle(tituloRes)
            .setMessage(getString(R.string.eliminar_movimiento_mensaje, nombre))
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.eliminar) { _, _ -> alConfirmar() }
            .show()
    }

    private fun confirmarCierreSesion() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.cerrar_sesion)
            .setMessage(R.string.confirmar_cierre_sesion_mensaje)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.cerrar_sesion) { _, _ ->
                auth.signOut()
                irALogin()
            }
            .show()
    }

    private fun irALogin() {
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    private fun mostrarDialogoAgregarGasto(gastoExistente: Gasto? = null) {
        uriReciboSeleccionado = null
        val dialogo = DialogAgregarGastoBinding.inflate(layoutInflater)

        val categorias = resources.getStringArray(R.array.categorias_gasto)
        dialogo.spCategoria.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, categorias)
        )

        val alertDialog = MaterialAlertDialogBuilder(this)
            .setTitle(
                if (gastoExistente == null) {
                    R.string.titulo_dialogo_gasto
                } else {
                    R.string.titulo_dialogo_editar_gasto
                }
            )
            .setView(dialogo.root)
            .setCancelable(false)
            .create()

        dialogo.btnGuardar.setText(
            if (gastoExistente == null) R.string.guardar else R.string.guardar_cambios
        )

        gastoExistente?.let { gasto ->
            dialogo.etTitulo.setText(gasto.titulo)
            dialogo.etMonto.setText(montoEditable(gasto.monto))
            dialogo.spCategoria.setText(gasto.categoriaLaboral, false)
            dialogo.txtReciboSeleccionado.text = if (!gasto.rutaFotoRecibo.isNullOrBlank()) {
                getString(R.string.recibo_actual)
            } else {
                getString(R.string.sin_recibo)
            }
        }

        actualizarTextoRecibo = { texto -> dialogo.txtReciboSeleccionado.text = texto }

        dialogo.etTitulo.doAfterTextChanged { dialogo.tilTitulo.error = null }
        dialogo.etMonto.doAfterTextChanged { dialogo.tilMonto.error = null }
        dialogo.spCategoria.setOnItemClickListener { _, _, _, _ ->
            dialogo.tilCategoria.error = null
        }

        dialogo.btnAdjuntarRecibo.setOnClickListener {
            seleccionarFotoGaleria.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        dialogo.spCategoria.setOnClickListener { dialogo.spCategoria.showDropDown() }

        dialogo.btnCancelar.setOnClickListener {
            actualizarTextoRecibo = null
            uriReciboSeleccionado = null
            alertDialog.dismiss()
        }

        dialogo.btnGuardar.setOnClickListener {
            val titulo = dialogo.etTitulo.text?.toString().orEmpty().trim()
            val montoTexto = dialogo.etMonto.text?.toString().orEmpty().trim()
            val categoria = dialogo.spCategoria.text?.toString().orEmpty().trim()

            val validacion = Validador.validarGasto(titulo, montoTexto, categoria)
            if (!validacion.esValido) {
                mostrarErrorGasto(dialogo, validacion.error)
                return@setOnClickListener
            }

            val monto = Validador.parsearMonto(montoTexto) ?: return@setOnClickListener
            val sello = Fechas.ahora()
            val gasto = Gasto(
                id = gastoExistente?.id ?: UUID.randomUUID().toString(),
                titulo = titulo,
                monto = monto,
                categoriaLaboral = categoria,
                fecha = gastoExistente?.fecha ?: sello.texto,
                fechaMillis = gastoExistente?.fechaMillis ?: sello.millis,
                rutaFotoRecibo = gastoExistente?.rutaFotoRecibo,
                rutaReciboStorage = gastoExistente?.rutaReciboStorage
            )

            if (gastoExistente == null) {
                viewModel.agregarGasto(gasto, uriReciboSeleccionado)
            } else {
                viewModel.actualizarGasto(gasto, uriReciboSeleccionado)
            }
            uriReciboSeleccionado = null
            actualizarTextoRecibo = null
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun mostrarErrorGasto(
        dialogo: DialogAgregarGastoBinding,
        error: ErrorValidacion?
    ) {
        when (error) {
            ErrorValidacion.TITULO_VACIO ->
                dialogo.tilTitulo.error = getString(R.string.error_titulo)
            ErrorValidacion.TITULO_LARGO ->
                dialogo.tilTitulo.error = getString(R.string.error_titulo_largo)
            ErrorValidacion.MONTO_INVALIDO ->
                dialogo.tilMonto.error = getString(R.string.error_monto)
            ErrorValidacion.MONTO_NO_POSITIVO ->
                dialogo.tilMonto.error = getString(R.string.error_monto_positivo)
            ErrorValidacion.MONTO_EXCESIVO ->
                dialogo.tilMonto.error = getString(R.string.error_monto_excesivo)
            ErrorValidacion.CATEGORIA_VACIA ->
                dialogo.tilCategoria.error = getString(R.string.error_categoria)
            else -> Toast.makeText(this, R.string.campos_requeridos, Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarDialogoAgregarIngreso(ingresoExistente: Ingreso? = null) {
        val dialogo = DialogAgregarIngresoBinding.inflate(layoutInflater)

        val categorias = resources.getStringArray(R.array.categorias_ingreso)
        dialogo.spCategoriaIngreso.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, categorias)
        )

        val alertDialog = MaterialAlertDialogBuilder(this)
            .setTitle(
                if (ingresoExistente == null) {
                    R.string.titulo_dialogo_ingreso
                } else {
                    R.string.titulo_dialogo_editar_ingreso
                }
            )
            .setView(dialogo.root)
            .setCancelable(false)
            .create()

        dialogo.btnGuardarIngreso.setText(
            if (ingresoExistente == null) R.string.agregar else R.string.guardar_cambios
        )

        ingresoExistente?.let { ingreso ->
            dialogo.etConceptoIngreso.setText(ingreso.concepto)
            dialogo.etMontoIngreso.setText(montoEditable(ingreso.monto))
            dialogo.spCategoriaIngreso.setText(ingreso.categoria, false)
        }

        dialogo.etConceptoIngreso.doAfterTextChanged {
            dialogo.tilConceptoIngreso.error = null
        }
        dialogo.etMontoIngreso.doAfterTextChanged { dialogo.tilMontoIngreso.error = null }
        dialogo.spCategoriaIngreso.setOnItemClickListener { _, _, _, _ ->
            dialogo.tilCategoriaIngreso.error = null
        }

        dialogo.btnCancelarIngreso.setOnClickListener { alertDialog.dismiss() }

        dialogo.spCategoriaIngreso.setOnClickListener {
            dialogo.spCategoriaIngreso.showDropDown()
        }

        dialogo.btnGuardarIngreso.setOnClickListener {
            val concepto = dialogo.etConceptoIngreso.text?.toString().orEmpty().trim()
            val montoTexto = dialogo.etMontoIngreso.text?.toString().orEmpty().trim()
            val categoria = dialogo.spCategoriaIngreso.text?.toString().orEmpty().trim()

            val validacion = Validador.validarIngreso(concepto, montoTexto, categoria)
            if (!validacion.esValido) {
                mostrarErrorIngreso(dialogo, validacion.error)
                return@setOnClickListener
            }

            val monto = Validador.parsearMonto(montoTexto) ?: return@setOnClickListener
            val sello = Fechas.ahora()
            val ingreso = Ingreso(
                id = ingresoExistente?.id ?: UUID.randomUUID().toString(),
                concepto = concepto,
                monto = monto,
                categoria = categoria,
                fecha = ingresoExistente?.fecha ?: sello.texto,
                fechaMillis = ingresoExistente?.fechaMillis ?: sello.millis
            )

            if (ingresoExistente == null) {
                viewModel.agregarIngreso(ingreso)
            } else {
                viewModel.actualizarIngreso(ingreso)
            }
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun mostrarErrorIngreso(
        dialogo: DialogAgregarIngresoBinding,
        error: ErrorValidacion?
    ) {
        when (error) {
            ErrorValidacion.CONCEPTO_VACIO ->
                dialogo.tilConceptoIngreso.error = getString(R.string.error_concepto)
            ErrorValidacion.MONTO_INVALIDO ->
                dialogo.tilMontoIngreso.error = getString(R.string.error_monto)
            ErrorValidacion.MONTO_NO_POSITIVO ->
                dialogo.tilMontoIngreso.error = getString(R.string.error_monto_positivo)
            ErrorValidacion.MONTO_EXCESIVO ->
                dialogo.tilMontoIngreso.error = getString(R.string.error_monto_excesivo)
            ErrorValidacion.CATEGORIA_VACIA ->
                dialogo.tilCategoriaIngreso.error = getString(R.string.error_categoria)
            else -> Toast.makeText(this, R.string.campos_requeridos, Toast.LENGTH_SHORT).show()
        }
    }

    private fun abrirRecibo(gasto: Gasto) {
        val url = gasto.rutaFotoRecibo
        if (url.isNullOrBlank()) {
            Toast.makeText(this, R.string.recibo_no_disponible, Toast.LENGTH_SHORT).show()
            return
        }

        val uri = runCatching { Uri.parse(url) }.getOrNull()
        if (uri == null || (uri.scheme != "https" && uri.scheme != "http")) {
            Toast.makeText(this, R.string.recibo_no_disponible, Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Intent.ACTION_VIEW, uri)
        runCatching { startActivity(intent) }
            .onFailure {
                Toast.makeText(this, R.string.error_abrir_recibo, Toast.LENGTH_SHORT).show()
            }
    }

    private fun montoEditable(monto: Double): String =
        Dinero.deCentavos(Dinero.aCentavos(monto)).stripTrailingZeros().toPlainString()

    override fun onDestroy() {
        actualizarTextoRecibo = null
        super.onDestroy()
    }
}

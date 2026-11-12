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
import com.example.controldegastos.viewModel.UiEvent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var gastosAdapter: GastosAdapter
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

        if (auth.currentUser == null) {
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
        gastosAdapter = GastosAdapter(
            onEliminar = ::confirmarEliminacion,
            onReciboClick = ::abrirRecibo
        )
        binding.rvGastos.layoutManager = LinearLayoutManager(this)
        binding.rvGastos.adapter = gastosAdapter
    }

    private fun configurarAcciones() {
        binding.fabAgregarGasto.setOnClickListener {
            val opciones = resources.getStringArray(R.array.opciones_registro)
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.menu_registrar_titulo)
                .setItems(opciones) { _, posicion ->
                    when (posicion) {
                        0 -> mostrarDialogoAgregarGasto()
                        1 -> mostrarDialogoAgregarIngreso()
                    }
                }
                .show()
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
        binding.progressCargando.isVisible = estado.cargando
        binding.txtVacio.isVisible = !estado.cargando && estado.gastos.isEmpty()
        binding.fabAgregarGasto.isEnabled = !estado.guardando

        binding.txtDineroDisponible.text = Dinero.formatear(estado.disponibleCentavos)
        binding.txtDineroDisponible.setTextColor(
            ContextCompat.getColor(
                this,
                if (estado.disponibleCentavos < 0) R.color.rojo_gasto else R.color.verde_primario
            )
        )
        binding.txtPresupuesto.text = Dinero.formatear(estado.presupuestoCentavos)
        binding.txtTotalGastado.text = Dinero.formatear(estado.totalGastadoCentavos)

        gastosAdapter.submitList(estado.gastos)
    }

    private fun mostrarEvento(evento: UiEvent) {
        val mensaje = when (evento) {
            UiEvent.GastoGuardado -> R.string.gasto_guardado
            UiEvent.IngresoGuardado -> R.string.ingreso_guardado
            UiEvent.GastoEliminado -> R.string.gasto_eliminado
            UiEvent.ErrorGuardarGasto -> R.string.error_guardar_gasto
            UiEvent.ErrorGuardarIngreso -> R.string.error_guardar_ingreso
            UiEvent.ErrorEliminarGasto -> R.string.error_eliminar_gasto
            UiEvent.ErrorCargarDatos -> R.string.error_cargar_datos
        }
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }

    private fun confirmarEliminacion(gasto: Gasto) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.eliminar_gasto)
            .setMessage(getString(R.string.eliminar_gasto_mensaje, gasto.titulo))
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.eliminar) { _, _ -> viewModel.eliminarGasto(gasto) }
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

    private fun mostrarDialogoAgregarGasto() {
        uriReciboSeleccionado = null
        val dialogo = DialogAgregarGastoBinding.inflate(layoutInflater)

        val categorias = resources.getStringArray(R.array.categorias_gasto)
        dialogo.spCategoria.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, categorias)
        )

        val alertDialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.titulo_dialogo_gasto)
            .setView(dialogo.root)
            .setCancelable(false)
            .create()

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
                id = UUID.randomUUID().toString(),
                titulo = titulo,
                monto = monto,
                categoriaLaboral = categoria,
                fecha = sello.texto,
                fechaMillis = sello.millis,
                rutaFotoRecibo = null,
                rutaReciboStorage = null
            )

            viewModel.agregarGasto(gasto, uriReciboSeleccionado)
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

    private fun mostrarDialogoAgregarIngreso() {
        val dialogo = DialogAgregarIngresoBinding.inflate(layoutInflater)

        val categorias = resources.getStringArray(R.array.categorias_ingreso)
        dialogo.spCategoriaIngreso.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, categorias)
        )

        val alertDialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.titulo_dialogo_ingreso)
            .setView(dialogo.root)
            .setCancelable(false)
            .create()

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
                id = UUID.randomUUID().toString(),
                concepto = concepto,
                monto = monto,
                categoria = categoria,
                fecha = sello.texto,
                fechaMillis = sello.millis
            )

            viewModel.agregarIngreso(ingreso)
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

    override fun onDestroy() {
        actualizarTextoRecibo = null
        super.onDestroy()
    }
}

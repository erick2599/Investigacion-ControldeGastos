package com.example.controldegastos.view

import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.controldegastos.R
import com.example.controldegastos.databinding.ActivityMainBinding
import com.example.controldegastos.databinding.DialogAgregarGastoBinding
import com.example.controldegastos.model.Gasto
import com.example.controldegastos.viewModel.GastosViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: GastosViewModel
    private lateinit var adapter: GastosAdapter

    private var reciboUri: Uri? = null
    private var txtReciboDialogo: TextView? = null

    // Selector de fotos de la galería del sistema
    private val seleccionarFotoGaleria = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            reciboUri = uri
            txtReciboDialogo?.text = getString(R.string.recibo_adjunto)
            Toast.makeText(this, R.string.recibo_importado, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No se seleccionó ninguna imagen", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[GastosViewModel::class.java]

        adapter = GastosAdapter { gasto -> confirmarEliminar(gasto) }
        binding.rvGastos.layoutManager = LinearLayoutManager(this)
        binding.rvGastos.adapter = adapter

        binding.fabAgregarGasto.setOnClickListener { mostrarDialogoAgregarGasto() }

        observarEstado()
    }

    private fun observarEstado() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { estado ->
                    binding.txtPresupuesto.text = formatearMoneda(estado.presupuesto)
                    binding.txtTotalGastado.text = formatearMoneda(estado.totalGastado)
                    binding.txtDineroDisponible.text = formatearMoneda(estado.disponible)

                    binding.txtVacio.isVisible = estado.gastos.isEmpty()
                    binding.rvGastos.isVisible = estado.gastos.isNotEmpty()

                    adapter.submitList(estado.gastos)
                }
            }
        }
    }

    private fun mostrarDialogoAgregarGasto() {
        val dialogBinding = DialogAgregarGastoBinding.inflate(layoutInflater)
        reciboUri = null
        txtReciboDialogo = dialogBinding.txtReciboSeleccionado
        dialogBinding.txtReciboSeleccionado.text = getString(R.string.sin_recibo)

        val categorias = listOf("Transporte", "Materiales", "Viáticos", "Operaciones", "Otros")
        dialogBinding.spCategoria.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, categorias)
        )
        dialogBinding.spCategoria.setOnClickListener { dialogBinding.spCategoria.showDropDown() }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.nuevo_gasto)
            .setView(dialogBinding.root)
            .create()

        dialog.setOnDismissListener {
            txtReciboDialogo = null
            reciboUri = null
        }

        dialogBinding.btnCancelar.setOnClickListener { dialog.dismiss() }

        dialogBinding.btnAdjuntarRecibo.setOnClickListener {
            seleccionarFotoGaleria.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        dialogBinding.btnGuardar.setOnClickListener {
            val titulo = dialogBinding.etTitulo.text?.toString()?.trim().orEmpty()
            val montoTexto = dialogBinding.etMonto.text?.toString()
                ?.trim()
                ?.replace(",", ".")
                .orEmpty()
            val monto = montoTexto.toDoubleOrNull()
            val categoria = dialogBinding.spCategoria.text?.toString()?.trim().orEmpty()

            dialogBinding.tilTitulo.error = null
            dialogBinding.tilMonto.error = null
            dialogBinding.tilCategoria.error = null

            when {
                titulo.isEmpty() -> {
                    dialogBinding.tilTitulo.error = getString(R.string.error_titulo)
                }

                monto == null || monto <= 0 -> {
                    dialogBinding.tilMonto.error = getString(R.string.error_monto)
                }

                categoria.isEmpty() -> {
                    dialogBinding.tilCategoria.error = getString(R.string.error_categoria)
                }

                else -> {
                    viewModel.agregarGasto(
                        Gasto(
                            id = UUID.randomUUID().toString(),
                            titulo = titulo,
                            monto = monto,
                            categoriaLaboral = categoria,
                            fecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                .format(Date()),
                            rutaFotoRecibo = reciboUri?.toString()
                        )
                    )
                    Toast.makeText(this, R.string.gasto_guardado, Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun confirmarEliminar(gasto: Gasto) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.eliminar_gasto)
            .setMessage(getString(R.string.eliminar_gasto_mensaje, gasto.titulo))
            .setPositiveButton(R.string.eliminar) { _, _ ->
                viewModel.eliminarGasto(gasto.id)
                Toast.makeText(this, R.string.gasto_eliminado, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    private fun formatearMoneda(valor: Double): String =
        String.format(Locale.US, "\$%,.2f", valor)
}

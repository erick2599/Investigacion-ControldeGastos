package com.example.controldegastos.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.controldegastos.R
import com.example.controldegastos.model.Gasto
import com.example.controldegastos.model.Ingreso
import com.example.controldegastos.viewModel.GastosViewModel
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var txtBienvenida: TextView
    private lateinit var viewModel: GastosViewModel
    private lateinit var txtDineroDisponible: TextView
    private lateinit var txtPresupuesto: TextView
    private lateinit var txtTotalGastado: TextView
    private lateinit var fabAgregarGasto: FloatingActionButton
    private lateinit var btnCerrarSesion: Button
    private lateinit var cardBalance: MaterialCardView

    private lateinit var rvGastos: RecyclerView
    private lateinit var gastosAdapter: GastosAdapter

    private var uriReciboSeleccionado: Uri? = null
    private var txtReciboReferenciaDialogo: TextView? = null


    // Registrar el selector de fotos de la galería
    private val seleccionarFotoGaleria = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            uriReciboSeleccionado = uri
            txtReciboReferenciaDialogo?.text = "Recibo cargado correctamente"
            Toast.makeText(this, "Foto del recibo seleccionada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicialización de componentes visuales
        txtBienvenida = findViewById(R.id.txtBienvenida)
        txtDineroDisponible = findViewById(R.id.txtDineroDisponible)
        txtPresupuesto = findViewById(R.id.txtPresupuesto)
        txtTotalGastado = findViewById(R.id.txtTotalGastado)
        fabAgregarGasto = findViewById(R.id.fabAgregarGasto)
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion)
        cardBalance = findViewById(R.id.cardBalance)

        // Configuración de la lista de gastos (RecyclerView)
        rvGastos = findViewById(R.id.rvGastos)
        rvGastos.layoutManager = LinearLayoutManager(this)

        // Ajuste en el onCreate de tu MainActivity.kt
        gastosAdapter = GastosAdapter { gastoAEliminar ->
            // Cuando el usuario presione el icono de eliminar o haga clic largo, se borra de Firebase:
            viewModel.eliminarGasto(gastoAEliminar.id)
            Toast.makeText(this, "Gasto eliminado de la nube: ${gastoAEliminar.titulo}", Toast.LENGTH_SHORT).show()
        }
        rvGastos.adapter = gastosAdapter


        // Carga de perfil del usuario logueado
        val currentUser = auth.currentUser
        val emailUsuario = currentUser?.email
        if (emailUsuario != null) {
            db.collection("usuarios").document(emailUsuario).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val nombre = document.getString("nombre") ?: "Usuario"
                        txtBienvenida.text = "¡Bienvenido, $nombre!"
                    } else {
                        val nombreGoogle = currentUser.displayName ?: "Usuario"
                        txtBienvenida.text = "¡Bienvenido, $nombreGoogle!"
                    }
                }
        }

        viewModel = ViewModelProvider(this)[GastosViewModel::class.java]
        actualizarPantallaFinanciera()

        // Evento de clic en la tarjeta de saldo
        cardBalance.setOnClickListener {
            // Puedes usar esto para abrir un historial de ingresos si lo deseas
        }

        // Configuración del botón flotante con menú selectivo
        fabAgregarGasto.setOnClickListener {
            val opciones = arrayOf("Registrar un Gasto (-)", "Registrar un Ingreso (+)")

            MaterialAlertDialogBuilder(this)
                .setTitle("¿Qué deseas registrar?")
                .setItems(opciones) { _, position ->
                    when (position) {
                        0 -> mostrarDialogoAgregarGasto()
                        1 -> mostrarDialogoAgregarIngreso()
                    }
                }
                .show()
        }

        // Cierre seguro de sesión
        btnCerrarSesion.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun mostrarDialogoAgregarGasto() {
        uriReciboSeleccionado = null

        val dialogView = layoutInflater.inflate(R.layout.dialog_agregar_gasto, null)

        val etTitulo = dialogView.findViewById<TextInputEditText>(R.id.etTitulo)
        val etMonto = dialogView.findViewById<TextInputEditText>(R.id.etMonto)
        val spCategoria = dialogView.findViewById<AutoCompleteTextView>(R.id.spCategoria)
        val btnAdjuntar = dialogView.findViewById<Button>(R.id.btnAdjuntarRecibo)
        txtReciboReferenciaDialogo = dialogView.findViewById(R.id.txtReciboSeleccionado)
        val btnCancelar = dialogView.findViewById<Button>(R.id.btnCancelar)
        val btnGuardar = dialogView.findViewById<Button>(R.id.btnGuardar)

        val categorias = arrayOf("Operaciones", "Alimentación", "Transporte", "Servicios", "Otros")
        // Cambia esto tanto en mostrarDialogoAgregarGasto() como en mostrarDialogoAgregarIngreso()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categorias)

        spCategoria.setAdapter(adapter)

        spCategoria.setOnClickListener { spCategoria.showDropDown() }

        val alertDialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnAdjuntar.setOnClickListener {
            seleccionarFotoGaleria.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        btnCancelar.setOnClickListener { alertDialog.dismiss() }

        btnGuardar.setOnClickListener {
            val titulo = etTitulo.text.toString().trim()
            val montoStr = etMonto.text.toString().trim()
            val category = spCategoria.text.toString().trim()

            if (titulo.isNotEmpty() && montoStr.isNotEmpty() && category.isNotEmpty()) {
                val monto = montoStr.toDoubleOrNull()
                if (monto != null) {
                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    val fechaHoraActual = sdf.format(Date())

                    val nuevoGasto = Gasto(
                        id = UUID.randomUUID().toString(),
                        titulo = titulo,
                        monto = monto,
                        categoriaLaboral = category,
                        fecha = fechaHoraActual,
                        rutaFotoRecibo = uriReciboSeleccionado?.toString() ?: ""
                    )

                    viewModel.agregarGasto(nuevoGasto)
                    Toast.makeText(this, "Gasto guardado con éxito", Toast.LENGTH_SHORT).show()
                    alertDialog.dismiss()
                } else {
                    Toast.makeText(this, "Por favor, ingresa un monto válido", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Por favor, completa todos los campos obligatorios", Toast.LENGTH_SHORT).show()
            }
        }

        alertDialog.show()
    }

    private fun mostrarDialogoAgregarIngreso() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_agregar_ingreso, null)

        val etConcepto = dialogView.findViewById<TextInputEditText>(R.id.etConceptoIngreso)
        val etMonto = dialogView.findViewById<TextInputEditText>(R.id.etMontoIngreso)
        val spCategoria = dialogView.findViewById<AutoCompleteTextView>(R.id.spCategoriaIngreso)
        val btnCancelar = dialogView.findViewById<Button>(R.id.btnCancelarIngreso)
        val btnGuardar = dialogView.findViewById<Button>(R.id.btnGuardarIngreso)

        val categorias = arrayOf("Salario", "Bonos", "Remesas", "Otros ingresos")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categorias)
        spCategoria.setAdapter(adapter)

        val alertDialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCancelar.setOnClickListener { alertDialog.dismiss() }

        btnGuardar.setOnClickListener {
            val concepto = etConcepto.text.toString().trim()
            val montoStr = etMonto.text.toString().trim()
            val categoria = spCategoria.text.toString().trim()

            if (concepto.isNotEmpty() && montoStr.isNotEmpty() && categoria.isNotEmpty()) {
                val monto = montoStr.toDoubleOrNull()
                if (monto != null && monto > 0) {
                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    val fechaActual = sdf.format(Date())

                    val nuevoIngreso = Ingreso(
                        id = UUID.randomUUID().toString(),
                        concepto = concepto,
                        monto = monto,
                        categoria = categoria,
                        fecha = fechaActual
                    )

                    viewModel.agregarIngreso(nuevoIngreso)
                    Toast.makeText(this, "Ingreso añadido al presupuesto", Toast.LENGTH_SHORT).show()
                    alertDialog.dismiss()
                } else {
                    Toast.makeText(this, "Por favor, ingresa un monto mayor a cero", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        alertDialog.show()
    }

    private fun actualizarPantallaFinanciera() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Consumo eficiente del uiState unificado del ViewModel
                viewModel.uiState.collect { state ->
                    txtPresupuesto.text = String.format(Locale.US, "$%.2f", state.presupuesto)
                    txtTotalGastado.text = String.format(Locale.US, "$%.2f", state.totalGastado)
                    txtDineroDisponible.text = String.format(Locale.US, "$%.2f", state.disponible)

                    // Actualización del adaptador reactivo
                    gastosAdapter.submitList(state.gastos)
                }
            }
        }
    }
}

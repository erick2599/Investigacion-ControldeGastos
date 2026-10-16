package com.example.controldegastos.view

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.controldegastos.R
import com.example.controldegastos.model.Gasto
import com.example.controldegastos.viewModel.GastosViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.UUID
import kotlinx.coroutines.flow.StateFlow


class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: GastosViewModel
    private lateinit var txtDineroDisponible: TextView
    private lateinit var txtPresupuesto: TextView
    private lateinit var txtTotalGastado: TextView
    private lateinit var fabAgregarGasto: FloatingActionButton

    // Registrar el selector de fotos de la galería del sistema
    private val seleccionarFotoGaleria = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            // El usuario seleccionó la foto de su recibo con éxito
            Toast.makeText(this, "Recibo importado con éxito", Toast.LENGTH_LONG).show()

            // Creamos un gasto de prueba simulando los datos extraídos del recibo
            val nuevoGasto = Gasto(
                id = UUID.randomUUID().toString(),
                titulo = "Recibo Importado (Trabajo)",
                monto = 75.50, // Monto de prueba simulado
                categoriaLaboral = "Operaciones",
                fecha = "15/10/2026",
                rutaFotoRecibo = uri.toString()
            )

            // Lo agregamos al ViewModel para actualizar las tarjetas financieras
            viewModel.agregarGasto(nuevoGasto)
        } else {
            Toast.makeText(this, "No se seleccionó ninguna imagen", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[GastosViewModel::class.java]

        txtDineroDisponible = findViewById(R.id.txtDineroDisponible)
        txtPresupuesto = findViewById(R.id.txtPresupuesto)
        txtTotalGastado = findViewById(R.id.txtTotalGastado)
        fabAgregarGasto = findViewById(R.id.fabAgregarGasto)

        // Escuchar las actualizaciones de los flujos de datos financieros
        actualizarPantallaFinanciera()

        // Al presionar el botón (+), abrimos la galería para elegir la foto del recibo
        fabAgregarGasto.setOnClickListener {
            seleccionarFotoGaleria.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    private fun actualizarPantallaFinanciera() {
        // Usamos el ciclo de vida seguro para lanzar la corrutina
        androidx.lifecycle.lifecycleScope.launchWhenStarted {
            // Accedemos de forma directa al estado de la lista de gastos
            val presupuesto = viewModel.presupuestoInicial.value
            val totalGastado = viewModel.obtenerTotalGastado()
            val disponible = viewModel.obtenerDineroDisponible()

            // Formateamos los textos de manera limpia y sin errores de localización
            txtPresupuesto.text = String.format(java.util.Locale.US, "$%.2f", presupuesto)
            txtTotalGastado.text = String.format(java.util.Locale.US, "$%.2f", totalGastado)
            txtDineroDisponible.text = String.format(java.util.Locale.US, "$%.2f", disponible)
        }
    }


}




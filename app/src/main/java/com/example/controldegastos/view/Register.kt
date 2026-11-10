package com.example.controldegastos.view

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.controldegastos.R
import com.example.controldegastos.model.Usuario
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

class Register : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    // Instancia de Auth
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etApellido = findViewById<EditText>(R.id.etApellido)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etContrasena = findViewById<EditText>(R.id.etContrasena) // Nuevo campo
        val btnRegister = findViewById<Button>(R.id.btnRegistrar)

        btnRegister.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val apellido = etApellido.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val contrasena = etContrasena.text.toString().trim()

            if (nombre.isNotEmpty() && apellido.isNotEmpty() && email.isNotEmpty() && contrasena.isNotEmpty()) {
                if (contrasena.length < 6) {
                    Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // 1. Crear usuario en Firebase Authentication
                auth.createUserWithEmailAndPassword(email, contrasena)
                    .addOnSuccessListener {
                        // 2. Si se crea con éxito, guardamos sus datos en Firestore
                        val nuevoUsuario = Usuario(nombre, apellido, email)
                        db.collection("usuarios").document(email)
                            .set(nuevoUsuario)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Usuario registrado con éxito", Toast.LENGTH_LONG).show()
                                finish()
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error en registro: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            } else {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

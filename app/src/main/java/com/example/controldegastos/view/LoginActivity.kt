package com.example.controldegastos.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.controldegastos.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    // Instancia de View Binding (se inicializa de forma tardía)
    private lateinit var binding: ActivityLoginBinding

    // Instancia de Firebase Authentication
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Configurar View Binding para inflar la vista
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. Inicializar Firebase Auth
        mAuth = FirebaseAuth.getInstance()

        // 3. Comportamiento del botón "Iniciar Sesión"
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUsuario(email, password)
            } else {
                Toast.makeText(this, "Por favor, ingresa correo y contraseña", Toast.LENGTH_SHORT).show()
            }
        }

        // 4. Comportamiento del botón "Crear Cuenta Nueva"
        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                registrarUsuario(email, password)
            } else {
                Toast.makeText(this, "Llena los campos para crear tu cuenta", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Método para validar credenciales en Firebase e iniciar sesión
    private fun loginUsuario(email: String, password: String) {
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "¡Inicio de sesión exitoso!", Toast.LENGTH_SHORT).show()

                    // Navegamos hacia la pantalla principal
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish() // Cierra el Login de la pila de pantallas
                } else {
                    val error = task.exception?.message ?: "Error desconocido"
                    Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show();
                }
            }
    }

    // Método para dar de alta un nuevo correo en Firebase Authentication
    private fun registrarUsuario(email: String, password: String) {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "¡Usuario creado con éxito! Ya puedes iniciar sesión.", Toast.LENGTH_LONG).show()
                } else {
                    val error = task.exception?.message ?: "Error desconocido"
                    Toast.makeText(this, "Error al registrar: $error", Toast.LENGTH_LONG).show()
                }
            }
    }
}

package com.example.controldegastos.view

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView // IMPORT CORREGIDO
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.controldegastos.R
import com.google.android.material.textfield.TextInputEditText
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button
    private lateinit var btnGoogleLogin: Button
    private lateinit var tvOlvideContrasena: TextView // DECLARACIÓN DE VARIABLE CORREGIDA

    private val auth = FirebaseAuth.getInstance()
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin)
        tvOlvideContrasena = findViewById(R.id.tvOlvideContrasena) // ASIGNACIÓN CORRECTA DENTRO DE ONCREATE

        // Configuración de Google Sign-In (Asegúrate de cambiar este String por tu código real .apps.googleusercontent.com)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("project-74174685981://googleusercontent.com")
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Login Tradicional con Firebase
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        Toast.makeText(this, "¡Bienvenido de nuevo!", Toast.LENGTH_SHORT).show()
                        irAMainActivity()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Credenciales incorrectas: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            } else {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        // Login con Google
        btnGoogleLogin.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            launcherGoogle.launch(signInIntent)
        }

        btnRegister.setOnClickListener {
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        }

        // BLOQUE CORREGIDO: Ahora está seguro dentro de onCreate
        tvOlvideContrasena.setOnClickListener {
            val email = etEmail.text.toString().trim()

            if (email.isNotEmpty()) {
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Se ha enviado un correo para restablecer tu contraseña.", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al enviar el correo: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            } else {
                Toast.makeText(this, "Por favor, ingresa tu correo electrónico primero en el campo de texto", Toast.LENGTH_SHORT).show()
            }
        }
    } // AQUÍ CIERRA CORRECTAMENTE ONCREATE

    // Receptor del resultado de la pantalla de Google
    private val launcherGoogle = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val cuenta = task.getResult(ApiException::class.java)
                cuenta?.idToken?.let { token ->
                    val credencial = GoogleAuthProvider.getCredential(token, null)
                    auth.signInWithCredential(credencial)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Inicio de sesión con Google exitoso", Toast.LENGTH_SHORT).show()
                            irAMainActivity()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error Firebase: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Error Google: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun irAMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        // Estas banderas aseguran que MainActivity se abra de forma limpia como pantalla principal
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

}

@file:Suppress("DEPRECATION")

package com.example.controldegastos.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.controldegastos.R
import com.example.controldegastos.databinding.ActivityLoginBinding
import com.example.controldegastos.domain.Validador
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth = FirebaseAuth.getInstance()
    private var googleSignInClient: GoogleSignInClient? = null

    private val launcherGoogle = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        if (resultado.resultCode != RESULT_OK) return@registerForActivityResult

        lifecycleScope.launch {
            try {
                val cuenta = GoogleSignIn.getSignedInAccountFromIntent(resultado.data).await()
                val token = cuenta.idToken
                    ?: error(getString(R.string.google_error, "token vacío"))
                auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                Toast.makeText(this@LoginActivity, R.string.google_exitoso, Toast.LENGTH_SHORT).show()
                irAMainActivity()
            } catch (e: Exception) {
                Toast.makeText(
                    this@LoginActivity,
                    getString(R.string.google_error, e.localizedMessage.orEmpty()),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sesión recordada: si Firebase ya tiene un usuario, no mostramos el login.
        if (auth.currentUser != null) {
            irAMainActivity()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarGoogle()
        configurarEventos()
    }

    private fun configurarGoogle() {
        // El ID de cliente web se configura en res/values/strings.xml. Si está
        // vacío, ocultamos el botón en lugar de fallar con un token inválido.
        val webClientId = getString(R.string.google_web_client_id)
        if (webClientId.isBlank()) {
            binding.btnGoogleLogin.isVisible = false
            return
        }

        val opciones = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, opciones)

        binding.btnGoogleLogin.isVisible = true
        binding.btnGoogleLogin.setOnClickListener {
            googleSignInClient?.let { cliente ->
                launcherGoogle.launch(cliente.signInIntent)
            }
        }
    }

    private fun configurarEventos() {
        binding.etEmail.doAfterTextChanged { binding.tilEmail.error = null }
        binding.etPassword.doAfterTextChanged { binding.tilPassword.error = null }

        binding.btnLogin.setOnClickListener { iniciarSesionConCorreo() }

        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
        }

        binding.tvOlvideContrasena.setOnClickListener { enviarCorreoRestablecimiento() }
    }

    private fun iniciarSesionConCorreo() {
        val email = binding.etEmail.text?.toString().orEmpty().trim()
        val contrasena = binding.etPassword.text?.toString().orEmpty()

        if (!validarCredenciales(email, contrasena)) return

        setCargando(true)
        lifecycleScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, contrasena).await()
                Toast.makeText(this@LoginActivity, R.string.bienvenido, Toast.LENGTH_SHORT).show()
                irAMainActivity()
            } catch (e: FirebaseAuthException) {
                binding.tilPassword.error = mensajeDeError(e)
            } catch (e: Exception) {
                binding.tilPassword.error = getString(
                    R.string.error_iniciar_sesion,
                    e.localizedMessage.orEmpty()
                )
            } finally {
                setCargando(false)
            }
        }
    }

    private fun enviarCorreoRestablecimiento() {
        val email = binding.etEmail.text?.toString().orEmpty().trim()
        if (!Validador.esEmailValido(email)) {
            binding.tilEmail.error = getString(R.string.error_correo)
            return
        }

        setCargando(true)
        lifecycleScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                Toast.makeText(
                    this@LoginActivity,
                    R.string.restablecer_enviado,
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: FirebaseAuthException) {
                Toast.makeText(this@LoginActivity, mensajeDeError(e), Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@LoginActivity,
                    getString(R.string.error_restablecer, e.localizedMessage.orEmpty()),
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                setCargando(false)
            }
        }
    }

    private fun validarCredenciales(email: String, contrasena: String): Boolean {
        if (email.isBlank()) {
            binding.tilEmail.error = getString(R.string.error_correo)
            return false
        }
        if (!Validador.esEmailValido(email)) {
            binding.tilEmail.error = getString(R.string.error_correo)
            return false
        }
        if (!Validador.validarContrasena(contrasena).esValido) {
            binding.tilPassword.error = getString(R.string.error_contrasena)
            return false
        }
        return true
    }

    private fun mensajeDeError(error: FirebaseAuthException): String = when (error.errorCode) {
        "ERROR_INVALID_CREDENTIALS",
        "ERROR_INVALID_EMAIL",
        "ERROR_USER_NOT_FOUND",
        "ERROR_WRONG_PASSWORD" -> getString(R.string.error_credenciales)
        "ERROR_NETWORK_REQUEST_FAILED" -> getString(R.string.error_sin_conexion)
        "ERROR_TOO_MANY_REQUESTS" -> getString(R.string.error_demasiados_intentos)
        else -> getString(R.string.error_firebase, error.localizedMessage.orEmpty())
    }

    private fun setCargando(cargando: Boolean) {
        binding.progressLogin.isVisible = cargando
        binding.etEmail.isEnabled = !cargando
        binding.etPassword.isEnabled = !cargando
        binding.btnLogin.isEnabled = !cargando
        binding.btnRegister.isEnabled = !cargando
        binding.btnGoogleLogin.isEnabled = !cargando
        binding.tvOlvideContrasena.isEnabled = !cargando
    }

    private fun irAMainActivity() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }
}

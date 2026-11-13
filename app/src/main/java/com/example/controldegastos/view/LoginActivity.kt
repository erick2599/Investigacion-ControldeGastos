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
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth = FirebaseAuth.getInstance()
    private var googleSignInClient: GoogleSignInClient? = null
    private var comprobandoVerificacion = false
    private var navegando = false

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
                continuarSiEstaVerificado()
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
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarGoogle()
        configurarEventos()
    }

    override fun onStart() {
        super.onStart()
        val usuario = auth.currentUser
        if (usuario == null) {
            mostrarLogin()
        } else {
            manejarSesionExistente()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!::binding.isInitialized) return

        // Al volver del navegador tras abrir el enlace del correo, comprobamos
        // la verificación sin que el usuario tenga que pulsar nada.
        val usuario = auth.currentUser
        if (usuario != null) {
            if (usuario.isEmailVerified) {
                irAMainActivity()
            } else {
                if (!binding.panelVerificacion.isVisible) {
                    mostrarPanelVerificacion(usuario.email.orEmpty())
                }
                comprobarVerificacion(manual = false)
            }
        } else if (!binding.panelLogin.isVisible) {
            mostrarLogin()
        }
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

        binding.btnYaVerifique.setOnClickListener {
            comprobarVerificacion(manual = true)
        }
        binding.btnReenviarVerificacion.setOnClickListener { reenviarVerificacion() }
        binding.btnCerrarSesionVerificacion.setOnClickListener { cerrarSesionPendiente() }
    }

    private fun iniciarSesionConCorreo() {
        val email = binding.etEmail.text?.toString().orEmpty().trim()
        val contrasena = binding.etPassword.text?.toString().orEmpty()

        if (!validarCredenciales(email, contrasena)) return

        setCargando(true)
        lifecycleScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, contrasena).await()
                continuarSiEstaVerificado()
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

    /**
     * Solo deja pasar a la pantalla principal si el correo está verificado.
     * Si no, muestra el panel para verificar o reenviar el correo.
     */
    private suspend fun continuarSiEstaVerificado() {
        val usuario = auth.currentUser
        if (usuario == null) {
            mostrarLogin()
            return
        }

        runCatching { usuario.reload().await() }

        if (usuario.isEmailVerified) {
            // Forzamos un token nuevo para que las reglas del servidor vean
            // el correo ya verificado en la primera lectura.
            runCatching { usuario.getIdToken(true).await() }
            Toast.makeText(this@LoginActivity, R.string.bienvenido, Toast.LENGTH_SHORT).show()
            irAMainActivity()
        } else {
            mostrarPanelVerificacion(usuario.email.orEmpty())
            Toast.makeText(
                this@LoginActivity,
                R.string.cuenta_sin_verificar,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun manejarSesionExistente() {
        val usuario = auth.currentUser ?: run {
            mostrarLogin()
            return
        }

        if (usuario.isEmailVerified) {
            irAMainActivity()
            return
        }

        mostrarPanelVerificacion(usuario.email.orEmpty())
        // La sesión guardada puede tener el estado desactualizado: comprobamos
        // en segundo plano y, si ya está verificado, continuamos solos.
        comprobarVerificacion(manual = false)
    }

    private fun comprobarVerificacion(manual: Boolean) {
        if (comprobandoVerificacion) return
        val usuario = auth.currentUser ?: run {
            mostrarLogin()
            return
        }

        comprobandoVerificacion = true
        if (manual) setCargandoVerificacion(true)

        lifecycleScope.launch {
            var mensajeError: String? = null
            try {
                usuario.reload().await()
                if (usuario.isEmailVerified) {
                    runCatching { usuario.getIdToken(true).await() }
                    Toast.makeText(
                        this@LoginActivity,
                        R.string.correo_verificado,
                        Toast.LENGTH_SHORT
                    ).show()
                    irAMainActivity()
                    return@launch
                }
            } catch (e: FirebaseAuthInvalidUserException) {
                auth.signOut()
                mostrarLogin()
                return@launch
            } catch (e: Exception) {
                mensajeError = e.localizedMessage.orEmpty()
            } finally {
                comprobandoVerificacion = false
                if (manual) setCargandoVerificacion(false)
            }

            if (manual) {
                val mensaje = if (mensajeError == null) {
                    getString(R.string.correo_no_verificado)
                } else {
                    getString(R.string.error_verificar, mensajeError)
                }
                Toast.makeText(this@LoginActivity, mensaje, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun reenviarVerificacion() {
        val usuario = auth.currentUser ?: run {
            mostrarLogin()
            return
        }

        setCargandoVerificacion(true)
        lifecycleScope.launch {
            try {
                usuario.sendEmailVerification().await()
                Toast.makeText(
                    this@LoginActivity,
                    R.string.reenviar_enviado,
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: FirebaseAuthException) {
                Toast.makeText(
                    this@LoginActivity,
                    getString(R.string.error_reenviar, mensajeDeError(e)),
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@LoginActivity,
                    getString(R.string.error_reenviar, e.localizedMessage.orEmpty()),
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                setCargandoVerificacion(false)
            }
        }
    }

    private fun cerrarSesionPendiente() {
        auth.signOut()
        binding.etPassword.text?.clear()
        mostrarLogin()
    }

    private fun mostrarLogin() {
        binding.panelLogin.isVisible = true
        binding.panelVerificacion.isVisible = false
        binding.progressVerificacion.isVisible = false
        setCargando(false)
    }

    private fun mostrarPanelVerificacion(email: String) {
        binding.panelLogin.isVisible = false
        binding.panelVerificacion.isVisible = true
        binding.txtVerificacionMensaje.text = getString(
            R.string.verificacion_mensaje,
            email.ifBlank { getString(R.string.tu_correo) }
        )
        setCargandoVerificacion(false)
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

    private fun setCargandoVerificacion(cargando: Boolean) {
        binding.progressVerificacion.isVisible = cargando
        binding.btnYaVerifique.isEnabled = !cargando
        binding.btnReenviarVerificacion.isEnabled = !cargando
        binding.btnCerrarSesionVerificacion.isEnabled = !cargando
    }

    private fun irAMainActivity() {
        if (navegando) return
        navegando = true

        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }
}

package com.example.controldegastos.view

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.controldegastos.R
import com.example.controldegastos.databinding.ActivityRegisterBinding
import com.example.controldegastos.domain.Validador
import com.example.controldegastos.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class Register : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etNombre.doAfterTextChanged { binding.tilNombre.error = null }
        binding.etApellido.doAfterTextChanged { binding.tilApellido.error = null }
        binding.etEmail.doAfterTextChanged { binding.tilEmail.error = null }
        binding.etContrasena.doAfterTextChanged { binding.tilContrasena.error = null }
        binding.etConfirmarContrasena.doAfterTextChanged {
            binding.tilConfirmarContrasena.error = null
        }

        binding.btnRegistrar.setOnClickListener { registrarUsuario() }
    }

    private fun registrarUsuario() {
        val nombre = binding.etNombre.text?.toString().orEmpty().trim()
        val apellido = binding.etApellido.text?.toString().orEmpty().trim()
        val email = binding.etEmail.text?.toString().orEmpty().trim()
        val contrasena = binding.etContrasena.text?.toString().orEmpty()
        val confirmacion = binding.etConfirmarContrasena.text?.toString().orEmpty()

        if (!validarFormulario(nombre, apellido, email, contrasena, confirmacion)) return

        setCargando(true)
        lifecycleScope.launch {
            try {
                val resultado = auth.createUserWithEmailAndPassword(email, contrasena).await()
                val usuario = resultado.user ?: error("Firebase no devolvió el usuario")

                try {
                    db.collection("usuarios").document(usuario.uid)
                        .set(Usuario(nombre = nombre, apellido = apellido, email = email))
                        .await()

                    runCatching {
                        usuario.updateProfile(
                            UserProfileChangeRequest.Builder()
                                .setDisplayName("$nombre $apellido".trim())
                                .build()
                        ).await()
                    }
                } catch (e: Exception) {
                    // Si no se pudo guardar el perfil, deshacemos la cuenta para
                    // que el usuario no quede autenticado sin datos.
                    runCatching { usuario.delete().await() }
                        .onFailure { runCatching { auth.signOut() } }
                    Toast.makeText(this@Register, R.string.error_guardar_perfil, Toast.LENGTH_LONG)
                        .show()
                    return@launch
                }

                // La cuenta queda creada, pero no se podrá usar hasta que el
                // correo esté verificado. LoginActivity muestra el aviso.
                try {
                    usuario.sendEmailVerification().await()
                    Toast.makeText(this@Register, R.string.registro_exitoso, Toast.LENGTH_LONG)
                        .show()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@Register,
                        R.string.error_enviar_verificacion,
                        Toast.LENGTH_LONG
                    ).show()
                }
                finish()
            } catch (e: FirebaseAuthException) {
                mostrarErrorDeRegistro(e)
            } catch (e: Exception) {
                Toast.makeText(
                    this@Register,
                    getString(R.string.error_registro, e.localizedMessage.orEmpty()),
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                setCargando(false)
            }
        }
    }

    private fun validarFormulario(
        nombre: String,
        apellido: String,
        email: String,
        contrasena: String,
        confirmacion: String
    ): Boolean {
        if (nombre.isBlank()) {
            binding.tilNombre.error = getString(R.string.error_nombre)
            return false
        }
        if (apellido.isBlank()) {
            binding.tilApellido.error = getString(R.string.error_apellido)
            return false
        }
        if (!Validador.esEmailValido(email)) {
            binding.tilEmail.error = getString(R.string.error_correo)
            return false
        }
        if (!Validador.validarContrasena(contrasena).esValido) {
            binding.tilContrasena.error = getString(R.string.error_contrasena_corta)
            return false
        }
        if (!Validador.validarConfirmacion(contrasena, confirmacion).esValido) {
            binding.tilConfirmarContrasena.error =
                getString(R.string.error_contrasenas_no_coinciden)
            return false
        }
        return true
    }

    private fun mostrarErrorDeRegistro(error: FirebaseAuthException) {
        val mensaje = when (error.errorCode) {
            "ERROR_EMAIL_ALREADY_IN_USE" -> getString(R.string.error_email_en_uso)
            "ERROR_WEAK_PASSWORD" -> getString(R.string.error_contrasena_debil)
            "ERROR_INVALID_EMAIL" -> getString(R.string.error_correo)
            "ERROR_NETWORK_REQUEST_FAILED" -> getString(R.string.error_sin_conexion)
            else -> getString(R.string.error_registro, error.localizedMessage.orEmpty())
        }
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
    }

    private fun setCargando(cargando: Boolean) {
        binding.progressRegistro.isVisible = cargando
        binding.btnRegistrar.isEnabled = !cargando
        binding.etNombre.isEnabled = !cargando
        binding.etApellido.isEnabled = !cargando
        binding.etEmail.isEnabled = !cargando
        binding.etContrasena.isEnabled = !cargando
        binding.etConfirmarContrasena.isEnabled = !cargando
    }
}

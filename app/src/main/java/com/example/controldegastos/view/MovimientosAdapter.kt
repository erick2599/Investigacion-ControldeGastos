package com.example.controldegastos.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.controldegastos.R
import com.example.controldegastos.databinding.ItemMovimientoBinding
import com.example.controldegastos.domain.Dinero
import com.example.controldegastos.model.Gasto
import com.example.controldegastos.model.Ingreso

/** Elemento que puede mostrarse en el historial principal. */
sealed interface Movimiento {
    val id: String

    data class GastoItem(val gasto: Gasto) : Movimiento {
        override val id: String get() = gasto.id
    }

    data class IngresoItem(val ingreso: Ingreso) : Movimiento {
        override val id: String get() = ingreso.id
    }
}

class MovimientosAdapter(
    private val onEditar: (Movimiento) -> Unit,
    private val onEliminar: (Movimiento) -> Unit,
    private val onReciboClick: (Gasto) -> Unit
) : ListAdapter<Movimiento, MovimientosAdapter.MovimientoViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovimientoViewHolder {
        val binding = ItemMovimientoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MovimientoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MovimientoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MovimientoViewHolder(
        private val binding: ItemMovimientoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(movimiento: Movimiento) {
            val contexto = binding.root.context

            when (movimiento) {
                is Movimiento.GastoItem -> {
                    val gasto = movimiento.gasto
                    binding.txtTituloMovimiento.text = gasto.titulo
                    binding.txtCategoriaMovimiento.text = gasto.categoriaLaboral
                    binding.txtFechaMovimiento.text = gasto.fecha
                    binding.txtMontoMovimiento.text =
                        Dinero.formatearGasto(Dinero.aCentavos(gasto.monto))
                    binding.txtMontoMovimiento.setTextColor(
                        ContextCompat.getColor(contexto, R.color.rojo_gasto)
                    )

                    val urlRecibo = gasto.rutaFotoRecibo
                    val tieneRecibo = !urlRecibo.isNullOrBlank()
                    binding.imgRecibo.isVisible = tieneRecibo
                    if (!urlRecibo.isNullOrBlank()) {
                        binding.imgRecibo.load(urlRecibo) {
                            placeholder(android.R.drawable.ic_menu_camera)
                            error(android.R.drawable.ic_menu_camera)
                            crossfade(true)
                        }
                    } else {
                        binding.imgRecibo.setImageDrawable(null)
                    }
                    binding.imgRecibo.setOnClickListener {
                        if (tieneRecibo) onReciboClick(gasto)
                    }
                }

                is Movimiento.IngresoItem -> {
                    val ingreso = movimiento.ingreso
                    binding.txtTituloMovimiento.text = ingreso.concepto
                    binding.txtCategoriaMovimiento.text = ingreso.categoria
                    binding.txtFechaMovimiento.text = ingreso.fecha
                    binding.txtMontoMovimiento.text = contexto.getString(
                        R.string.formato_ingreso,
                        Dinero.formatear(Dinero.aCentavos(ingreso.monto))
                    )
                    binding.txtMontoMovimiento.setTextColor(
                        ContextCompat.getColor(contexto, R.color.verde_primario)
                    )

                    binding.imgRecibo.isVisible = false
                    binding.imgRecibo.setImageDrawable(null)
                    binding.imgRecibo.setOnClickListener(null)
                }
            }

            binding.btnEditarMovimiento.setOnClickListener { onEditar(movimiento) }
            binding.btnEliminarMovimiento.setOnClickListener { onEliminar(movimiento) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Movimiento>() {
            override fun areItemsTheSame(oldItem: Movimiento, newItem: Movimiento): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Movimiento, newItem: Movimiento): Boolean =
                oldItem == newItem
        }
    }
}

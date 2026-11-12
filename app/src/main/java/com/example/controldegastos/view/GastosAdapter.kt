package com.example.controldegastos.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.controldegastos.databinding.ItemGastoBinding
import com.example.controldegastos.domain.Dinero
import com.example.controldegastos.model.Gasto

class GastosAdapter(
    private val onEliminar: (Gasto) -> Unit,
    private val onReciboClick: (Gasto) -> Unit
) : ListAdapter<Gasto, GastosAdapter.GastoViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GastoViewHolder {
        val binding = ItemGastoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GastoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GastoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GastoViewHolder(
        private val binding: ItemGastoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(gasto: Gasto) {
            binding.txtTituloGasto.text = gasto.titulo
            binding.txtCategoriaGasto.text = gasto.categoriaLaboral
            binding.txtFechaGasto.text = gasto.fecha
            binding.txtMontoGasto.text = Dinero.formatearGasto(Dinero.aCentavos(gasto.monto))

            val urlRecibo = gasto.rutaFotoRecibo
            val tieneRecibo = !urlRecibo.isNullOrBlank()

            binding.imgRecibo.isVisible = tieneRecibo
            if (tieneRecibo) {
                binding.imgRecibo.load(urlRecibo) {
                    placeholder(android.R.drawable.ic_menu_camera)
                    error(android.R.drawable.ic_menu_camera)
                    crossfade(true)
                }
            }

            binding.imgRecibo.setOnClickListener {
                if (tieneRecibo) onReciboClick(gasto)
            }
            binding.btnEliminarGasto.setOnClickListener { onEliminar(gasto) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Gasto>() {
            override fun areItemsTheSame(oldItem: Gasto, newItem: Gasto): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Gasto, newItem: Gasto): Boolean =
                oldItem == newItem
        }
    }
}

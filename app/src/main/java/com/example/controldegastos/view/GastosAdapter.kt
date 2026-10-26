package com.example.controldegastos.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.controldegastos.databinding.ItemGastoBinding
import com.example.controldegastos.model.Gasto
import java.util.Locale

class GastosAdapter(
    private val onEliminar: (Gasto) -> Unit
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
            binding.txtMontoGasto.text = "-" + String.format(Locale.US, "\$%,.2f", gasto.monto)
            binding.imgRecibo.isVisible = gasto.rutaFotoRecibo != null

            binding.btnEliminarGasto.setOnClickListener { onEliminar(gasto) }
            binding.root.setOnLongClickListener {
                onEliminar(gasto)
                true
            }
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

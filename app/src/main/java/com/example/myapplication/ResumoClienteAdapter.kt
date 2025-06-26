package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class ResumoClienteAdapter(private var clientes: List<ResumoClienteItem>) :
    RecyclerView.Adapter<ResumoClienteAdapter.ResumoClienteViewHolder>() {

    private val decimalFormat = DecimalFormat("R$ #,##0.00", DecimalFormatSymbols(Locale("pt", "BR")))

    class ResumoClienteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nomeCliente: TextView = itemView.findViewById(R.id.textViewNomeClienteResumo)
        val totalGasto: TextView = itemView.findViewById(R.id.textViewTotalGastoCliente)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResumoClienteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_resumo_cliente, parent, false)
        return ResumoClienteViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResumoClienteViewHolder, position: Int) {
        val cliente = clientes[position]
        holder.nomeCliente.text = cliente.nomeCliente
        holder.totalGasto.text = decimalFormat.format(cliente.totalCompras) // Acessando 'totalCompras'
    }

    override fun getItemCount(): Int = clientes.size

    fun updateClientes(newClientes: List<ResumoClienteItem>) {
        clientes = newClientes
        notifyDataSetChanged()
    }
}
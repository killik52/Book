package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class ResumoArtigoAdapter(private var artigos: List<ResumoArtigoItem>) :
    RecyclerView.Adapter<ResumoArtigoAdapter.ResumoArtigoViewHolder>() {

    private val decimalFormat = DecimalFormat("R$ #,##0.00", DecimalFormatSymbols(Locale("pt", "BR")))

    class ResumoArtigoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nomeArtigo: TextView = itemView.findViewById(R.id.textViewNomeArtigoResumo)
        val quantidadeVendida: TextView = itemView.findViewById(R.id.textViewQuantidadeVendidaArtigo)
        val totalVendido: TextView = itemView.findViewById(R.id.textViewTotalVendidoArtigo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResumoArtigoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_resumo_artigo, parent, false)
        return ResumoArtigoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResumoArtigoViewHolder, position: Int) {
        val artigo = artigos[position]
        holder.nomeArtigo.text = artigo.nomeArtigo
        holder.quantidadeVendida.text = "Qtd: ${artigo.quantidadeVendida}" // Acessando 'quantidadeVendida'
        holder.totalVendido.text = decimalFormat.format(artigo.valorTotalVendido) // Acessando 'valorTotalVendido'
    }

    override fun getItemCount(): Int = artigos.size

    fun updateArtigos(newArtigos: List<ResumoArtigoItem>) {
        artigos = newArtigos
        notifyDataSetChanged()
    }
}
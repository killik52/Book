package com.example.myapplication

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

// 1. Classe adaptadora para o RecyclerView que exibe uma lista de faturas resumidas com suporte a cliques
class FaturaResumidaAdapter(
    private val context: Context,
    private val onItemClick: (FaturaResumidaItem) -> Unit,
    private val onItemLongClick: (FaturaResumidaItem) -> Unit
) : ListAdapter<FaturaResumidaItem, FaturaResumidaAdapter.FaturaViewHolder>(FaturaDiffCallback()) {

    // 2. Lista mutável que armazena os itens de fatura resumida
    private val faturas: MutableList<FaturaResumidaItem> = mutableListOf()

    // 4. Método chamado para criar uma nova view para cada item da lista
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaturaViewHolder {
        // 5. Infla o layout do item da fatura
        val view = LayoutInflater.from(context).inflate(R.layout.item_fatura, parent, false)
        // 6. Retorna uma nova instância do ViewHolder com a view inflada
        return FaturaViewHolder(view)
    }

    // 7. Método chamado para vincular os dados da fatura à view
    override fun onBindViewHolder(holder: FaturaViewHolder, position: Int) {
        // 8. Obtém a fatura na posição especificada
        val fatura = getItem(position)
        "Vinculando fatura na posição $position: Número: ${fatura.numeroFatura}, Cliente: ${fatura.cliente}, Enviada: ${fatura.foiEnviada}".logDebug("FaturaResumidaAdapter")
        
        // 9. Define o número da fatura
        holder.faturaNumero.text = fatura.numeroFatura
        // 10. Define o nome do cliente
        holder.faturaCliente.text = "Cliente: ${fatura.cliente}"
        // 11. Define a data da fatura
        holder.faturaData.text = "Data: ${fatura.data}"
        // 12. Define o valor do saldo devedor formatado usando extension
        holder.faturaValor.text = fatura.saldoDevedor.toCurrencyString()

        // Configura a visibilidade do status "Enviado"
        if (fatura.foiEnviada) {
            holder.faturaStatusEnviado.show()
        } else {
            holder.faturaStatusEnviado.hide()
        }

        // 13. Configura o listener para clique simples no item
        holder.itemView.setOnClickListener {
            "Clique simples na fatura ID: ${fatura.id}".logDebug("FaturaResumidaAdapter")
            // 14. Chama a função de callback para clique simples
            onItemClick(fatura)
        }

        // 15. Configura o listener para clique longo no item
        holder.itemView.setOnLongClickListener {
            "Clique longo na fatura ID: ${fatura.id}".logDebug("FaturaResumidaAdapter")
            // 16. Chama a função de callback para clique longo
            onItemLongClick(fatura)
            true
        }
    }

    // 17. Método para atualizar a lista de faturas
    fun updateFaturas(newFaturas: List<FaturaResumidaItem>) {
        submitList(newFaturas)
    }

    // 18. Classe ViewHolder que armazena as referências aos componentes da view
    class FaturaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // 23. Referência ao TextView para o número da fatura
        val faturaNumero: TextView = itemView.findViewById(R.id.faturaNumero)
        // 24. Referência ao TextView para o nome do cliente
        val faturaCliente: TextView = itemView.findViewById(R.id.faturaCliente)
        // 25. Referência ao TextView para a data da fatura
        val faturaData: TextView = itemView.findViewById(R.id.faturaData)
        // 26. Referência ao TextView para o valor do saldo devedor
        val faturaValor: TextView = itemView.findViewById(R.id.faturaValor)
        // Nova referência para o status "Enviado"
        val faturaStatusEnviado: TextView = itemView.findViewById(R.id.faturaStatusEnviado)
    }

    class FaturaDiffCallback : DiffUtil.ItemCallback<FaturaResumidaItem>() {
        override fun areItemsTheSame(oldItem: FaturaResumidaItem, newItem: FaturaResumidaItem): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: FaturaResumidaItem, newItem: FaturaResumidaItem): Boolean {
            return oldItem == newItem
        }
    }
}
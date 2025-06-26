package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import database.entities.FaturaLixeira

class FaturaLixeiraRoomAdapter(
    private val onRestoreClick: (FaturaLixeira) -> Unit,
    private val onDeleteClick: (FaturaLixeira) -> Unit
) : ListAdapter<FaturaLixeira, FaturaLixeiraRoomAdapter.FaturaViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaturaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fatura_lixeira_room, parent, false)
        return FaturaViewHolder(view)
    }

    override fun onBindViewHolder(holder: FaturaViewHolder, position: Int) {
        val fatura = getItem(position)
        holder.numeroFaturaTextView.text = fatura.numeroFatura
        holder.clienteTextView.text = fatura.cliente
        holder.dataTextView.text = fatura.data
        holder.restoreButton.setOnClickListener { onRestoreClick(fatura) }
        holder.deleteButton.setOnClickListener { onDeleteClick(fatura) }
    }

    class FaturaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val numeroFaturaTextView: TextView = itemView.findViewById(R.id.numeroFaturaTextView)
        val clienteTextView: TextView = itemView.findViewById(R.id.clienteTextView)
        val dataTextView: TextView = itemView.findViewById(R.id.dataTextView)
        val restoreButton: Button = itemView.findViewById(R.id.restoreButton)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FaturaLixeira>() {
            override fun areItemsTheSame(oldItem: FaturaLixeira, newItem: FaturaLixeira): Boolean {
                return oldItem.id == newItem.id
            }
            override fun areContentsTheSame(oldItem: FaturaLixeira, newItem: FaturaLixeira): Boolean {
                return oldItem == newItem
            }
        }
    }
} 
package com.example.myapplication

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

// 1. Classe adaptadora para o RecyclerView que exibe uma lista de fotos
class FotoAdapter(
    private val context: Context,
    private val fotos: MutableList<String>,
    private val onPhotoClick: (String) -> Unit,
    private val onPhotoLongClick: (Int) -> Unit
) : RecyclerView.Adapter<FotoAdapter.FotoViewHolder>() {

    // 2. Método chamado para criar uma nova view para cada item da lista
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FotoViewHolder {
        // 3. Infla o layout do item da foto
        val view = LayoutInflater.from(context).inflate(R.layout.item_foto, parent, false)
        // 4. Retorna uma nova instância do ViewHolder com a view inflada
        return FotoViewHolder(view)
    }

    // 5. Método chamado para vincular o caminho da foto à view
    override fun onBindViewHolder(holder: FotoViewHolder, position: Int) {
        // 6. Obtém o caminho da foto na posição especificada
        val fotoPath = fotos[position]
        
        // 7. Carrega a imagem usando o ImageLoader com Coil
        holder.fotoImageView.loadLocalImage(
            filePath = fotoPath,
            placeholder = android.R.drawable.ic_menu_report_image,
            error = android.R.drawable.ic_menu_report_image
        )
        
        "Imagem carregada: $fotoPath".logDebug("FotoAdapter")

        // 8. Configura o listener para clique simples na imagem
        holder.fotoImageView.setOnClickListener {
            // 9. Chama a função de callback para clique simples
            onPhotoClick(fotoPath)
        }

        // 10. Configura o listener para clique longo na imagem
        holder.fotoImageView.setOnLongClickListener {
            // 11. Chama a função de callback para clique longo
            onPhotoLongClick(position)
            true
        }
    }

    // 12. Método que retorna o número total de fotos na lista
    override fun getItemCount(): Int = fotos.size

    // 13. Classe ViewHolder que armazena a referência ao componente da view
    class FotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // 14. Referência ao ImageView que exibe a foto
        val fotoImageView: ImageView = itemView.findViewById(R.id.fotoImageView)
    }
}
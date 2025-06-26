package com.example.myapplication

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.ActivityLixeiraRoomBinding
import database.entities.FaturaLixeira
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LixeiraActivityRoom : AppCompatActivity() {
    private lateinit var binding: ActivityLixeiraRoomBinding
    private val viewModel: LixeiraViewModel by viewModels()
    private lateinit var adapter: FaturaLixeiraRoomAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLixeiraRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = FaturaLixeiraRoomAdapter(
            onRestoreClick = { fatura ->
                viewModel.restaurarFatura(fatura)
                Toast.makeText(this, "Fatura restaurada!", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = { fatura ->
                viewModel.excluirFaturaPermanentemente(fatura)
                Toast.makeText(this, "Fatura excluída permanentemente!", Toast.LENGTH_SHORT).show()
            }
        )
        binding.faturasLixeiraRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.faturasLixeiraRecyclerView.adapter = adapter

        lifecycleScope.launch {
            viewModel.faturasLixeira.collectLatest { faturas ->
                adapter.submitList(faturas)
            }
        }
    }
} 
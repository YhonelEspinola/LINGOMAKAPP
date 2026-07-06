package com.lingomak.lingomakapp.ui.alertas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.lingomak.lingomakapp.databinding.FragmentAlertasBinding

class AlertasFragment : Fragment() {

    private var _binding: FragmentAlertasBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AlertasViewModel by viewModels()

    private lateinit var adapter: AlertasAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlertasBinding.inflate(inflater, container, false)

        configurarRecyclerView()
        observarViewModel()

        viewModel.listarAlertas()

        return binding.root
    }

    private fun configurarRecyclerView() {
        adapter = AlertasAdapter(emptyList())

        binding.rvAlertas.layoutManager =
            LinearLayoutManager(requireContext())

        binding.rvAlertas.adapter = adapter
    }

    private fun observarViewModel() {

        viewModel.listaAlertas.observe(viewLifecycleOwner) { lista ->

            adapter.actualizarLista(lista)

            binding.tvAlertasCriticas.text =
                lista.count { alerta ->
                    alerta.prioridad == "ALTA"
                }.toString()

            binding.tvAlertasAdvertencias.text =
                lista.count { alerta ->
                    alerta.prioridad == "MEDIA"
                }.toString()

            binding.tvAlertasTotal.text =
                lista.size.toString()

            binding.tvSinAlertas.visibility =
                if (lista.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.mensajeError.observe(viewLifecycleOwner) { mensaje ->
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

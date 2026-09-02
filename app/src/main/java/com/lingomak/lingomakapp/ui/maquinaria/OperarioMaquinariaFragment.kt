package com.lingomak.lingomakapp.ui.maquinaria

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.MaquinariaModel
import com.lingomak.lingomakapp.databinding.FragmentOperarioMaquinariaBinding

class OperarioMaquinariaFragment : Fragment() {

    private var _binding: FragmentOperarioMaquinariaBinding? = null
    private val binding get() = _binding!!

    private val maquinariaViewModel: MaquinariaViewModel by viewModels()

    private lateinit var adapter: OperarioMaquinariaAdapter

    private var listaOperativas: List<MaquinariaModel> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOperarioMaquinariaBinding.inflate(inflater, container, false)

        configurarRecyclerView()
        configurarBuscador()
        observarViewModel()

        maquinariaViewModel.listarMaquinarias()

        return binding.root
    }

    private fun configurarRecyclerView() {
        adapter = OperarioMaquinariaAdapter(
            listaMaquinarias = emptyList(),
            onRegistrarUsoClick = { maquinaria ->
                abrirRegistrarUso(maquinaria)
            }
        )

        binding.rvMaquinariaOperario.layoutManager =
            LinearLayoutManager(requireContext())

        binding.rvMaquinariaOperario.adapter = adapter
    }

    private fun configurarBuscador() {
        binding.etBuscarMaquinaria.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarMaquinaria(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observarViewModel() {
        maquinariaViewModel.listaMaquinarias.observe(viewLifecycleOwner) { lista ->


            listaOperativas = lista.filter { maquinaria ->
                maquinaria.estado == "OPERATIVA"
            }

            adapter.actualizarLista(listaOperativas)

            binding.tvSinMaquinariaOperativa.visibility =
                if (listaOperativas.isEmpty()) View.VISIBLE else View.GONE
        }

        maquinariaViewModel.mensajeError.observe(viewLifecycleOwner) { mensaje ->
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        }
    }

    private fun filtrarMaquinaria(texto: String) {
        val filtro = texto.lowercase().trim()

        val listaFiltrada =
            if (filtro.isEmpty()) {
                listaOperativas
            } else {
                listaOperativas.filter { maquinaria ->
                    maquinaria.nombre.lowercase().contains(filtro) ||
                            maquinaria.codigoMaquinaria.lowercase().contains(filtro) ||
                            maquinaria.tipo.lowercase().contains(filtro) ||
                            maquinaria.marca.lowercase().contains(filtro)
                }
            }

        adapter.actualizarLista(listaFiltrada)

        binding.tvSinMaquinariaOperativa.visibility =
            if (listaFiltrada.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun abrirRegistrarUso(maquinaria: MaquinariaModel) {
        val fragment = RegistrarUsoMaquinariaFragment()

        val bundle = Bundle().apply {
            putString("uidMaquinaria", maquinaria.uid)
            putString("codigoMaquinaria", maquinaria.codigoMaquinaria)
            putString("nombreMaquinaria", maquinaria.nombre)
            putString("tipoMaquinaria", maquinaria.tipo)
            putDouble("horometroActual", maquinaria.horometroActual)
            putDouble("horometroUltimoMantenimiento", maquinaria.horometroUltimoMantenimiento)
            putDouble("intervaloMantenimientoHoras", maquinaria.intervaloMantenimientoHoras)
        }

        fragment.arguments = bundle

        parentFragmentManager.beginTransaction()
            .replace(R.id.containerOperario, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
package com.lingomak.lingomakapp.ui.movimientos

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.databinding.FragmentMovimientosGlobalBinding

class MovimientosOpFragment : Fragment() {

    private var _binding: FragmentMovimientosGlobalBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MovimientosOpViewModel by viewModels()
    private lateinit var adapter: MovimientosGlobalAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMovimientosGlobalBinding.inflate(inflater, container, false)
        
        setupUI()
        observarViewModel()
        
        viewModel.sincronizarDatos()
        
        return binding.root
    }

    private fun setupUI() {
        // En el modo operario, quizás no necesitemos filtrar por entradas ya que solo registran salidas
        // pero pueden ver el historial general.
        
        adapter = MovimientosGlobalAdapter(emptyList()) { pair ->
            // Ver detalle
            val fragment = DetalleMovimientoFragment()
            val bundle = Bundle().apply { putString("movimientoUid", pair.first.uid) }
            fragment.arguments = bundle
            
            parentFragmentManager.beginTransaction()
                .replace(R.id.containerOperario, fragment)
                .addToBackStack(null)
                .commit()
        }

        binding.rvMovimientos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMovimientos.adapter = adapter

        // Implementar Scroll Infinito (Paginación)
        binding.rvMovimientos.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                // Si estamos cerca del final de la lista, cargamos más
                if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0) {
                    viewModel.cargarSiguienteLote()
                }
            }
        })

        binding.selectorFechas.onRangoSeleccionado = { inicio, fin, etiqueta ->
            viewModel.setRangoFechas(inicio, fin)
            binding.root.findViewById<TextView>(R.id.tvFiltroActual)?.text = etiqueta
        }
        binding.selectorFechas.dispararSeleccionActual()

        binding.etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.filtrarPorTexto(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Botón Registrar (En Operario solo Salidas)
        binding.btnRegistrar.text = "REGISTRAR SALIDA"
        binding.btnRegistrar.setOnClickListener {
            // Abrir el escáner que luego llevará a RegistrarSalidaOpFragment
            val fragment = EscaneoQRFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.containerOperario, fragment)
                .addToBackStack(null)
                .commit()
        }

        // Quitar estadísticas y centrar botón de registro
        binding.btnEstadisticas.visibility = View.GONE
        binding.layoutBotones.weightSum = 1f
    }

    private fun observarViewModel() {
        viewModel.movimientosFiltrados.observe(viewLifecycleOwner) { lista ->
            adapter.actualizarLista(lista)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
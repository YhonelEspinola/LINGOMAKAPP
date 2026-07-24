package com.lingomak.lingomakapp.ui.repuestos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.databinding.FragmentMovimientosBinding
import com.lingomak.lingomakapp.ui.dashboard.DashboardAdminActivity
import com.lingomak.lingomakapp.utils.CsvExporter
import java.text.SimpleDateFormat
import java.util.*

class HistorialRepuestoFragment : Fragment() {

    private var _binding: FragmentMovimientosBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistorialRepuestoViewModel by viewModels()
    private lateinit var adapter: HistorialRepuestoAdapter

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
            val movements = viewModel.historialFiltrado.value ?: emptyList()
            val dataToExport = movements.map { it to nombreRepuesto }
            val csvContent = buildCsvContent(dataToExport)
            CsvExporter.saveCsvToUri(requireContext(), it, csvContent)
        }
    }

    private fun buildCsvContent(data: List<Pair<com.lingomak.lingomakapp.data.model.MovimientoModel, String>>): String {
        val header = "Fecha,Tipo,Nombre del Repuesto,Cantidad,Destino,Registrado por,Orden Mantenimiento,Máquina,Observación"
        val rows = data.map { (mov, nombre) ->
            val fecha = mov.fecha?.let { d -> SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(d) } ?: ""
            val destino = when(mov.destinoSalida) {
                "CONSUMO_INTERNO" -> "Consumo Interno"
                "DISTRIBUCION_EXTERNA" -> "Distribución Externa"
                else -> mov.destinoSalida
            }
            "${fecha},${CsvExporter.escapeCsv(mov.tipo)},${CsvExporter.escapeCsv(nombre)},${mov.cantidad},${CsvExporter.escapeCsv(destino)},${CsvExporter.escapeCsv(mov.registradoPor)},${CsvExporter.escapeCsv(mov.ordenMantenimientoUid ?: "")},${CsvExporter.escapeCsv(mov.maquinariaUid ?: "")},${CsvExporter.escapeCsv(mov.observacion)}"
        }
        return header + "\n" + rows.joinToString("\n")
    }

    private var repuestoUid: String = ""
    private var nombreRepuesto: String = ""
    private var stockActual: Int = 0

    private var origen = ""
    private var tituloAlerta = ""
    private var mensajeAlerta = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMovimientosBinding.inflate(inflater, container, false)

        origen = arguments?.getString("origen") ?: ""
        tituloAlerta = arguments?.getString("tituloAlerta") ?: ""
        mensajeAlerta = arguments?.getString("mensajeAlerta") ?: ""

        repuestoUid = arguments?.getString("repuestoUid") ?: ""
        nombreRepuesto = arguments?.getString("nombreRepuesto") ?: ""
        stockActual = arguments?.getInt("stockActual") ?: 0

        viewModel.setRepuestoUid(repuestoUid)
        
        setupUI()
        observarViewModel()
        validarAccesoAdmin()
        
        return binding.root
    }

    private fun validarAccesoAdmin() {
        val isAdmin = requireActivity() is DashboardAdminActivity
        binding.btnExportarCsv.visibility = if (isAdmin) View.VISIBLE else View.GONE
    }

    private fun setupUI() {
        binding.tvTituloRepuesto.text = "Historial - $nombreRepuesto"
        
        adapter = HistorialRepuestoAdapter(emptyList())
        binding.rvMovimientos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMovimientos.adapter = adapter

        // Paginación del historial por repuesto
        binding.rvMovimientos.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                if ((layoutManager.childCount + layoutManager.findFirstVisibleItemPosition()) >= layoutManager.itemCount) {
                    viewModel.cargarSiguienteLote()
                }
            }
        })

        binding.selectorFechas.onRangoSeleccionado = { inicio, fin, etiqueta ->
            viewModel.setRangoFechas(inicio, fin)
            binding.tvTituloRepuesto.text = etiqueta
        }
        binding.selectorFechas.dispararSeleccionActual()

        binding.btnExportarCsv.setOnClickListener {
            val movements = viewModel.historialFiltrado.value ?: emptyList()
            if (movements.isNotEmpty()) {
                val dataToExport = movements.map { it to nombreRepuesto }
                CsvExporter.showExportDialog(requireContext(), "Historial_${nombreRepuesto.replace(" ", "_")}.csv", buildCsvContent(dataToExport), createDocumentLauncher)
            } else {
                Toast.makeText(requireContext(), "No hay movimientos para exportar", Toast.LENGTH_SHORT).show()
            }
        }

        mostrarBannerAlerta()
    }


    private fun observarViewModel() {
        viewModel.historialFiltrado.observe(viewLifecycleOwner) { lista ->
            adapter.actualizarLista(lista)
        }

        viewModel.resumen.observe(viewLifecycleOwner) { resumen ->
            binding.tvTotalEntradas.text = resumen.totalEntradas.toString()
            binding.tvTotalSalidas.text = resumen.totalSalidas.toString()
            binding.tvConsumoPromedio.text = resumen.balance.toString()
            
            val colorBalance = if (resumen.balance >= 0) R.color.success else R.color.danger
            binding.tvConsumoPromedio.setTextColor(ContextCompat.getColor(requireContext(), colorBalance))

            binding.tvLabelEntradas.text = "Entradas${resumen.periodoLabel}"
            binding.tvLabelSalidas.text = "Salidas${resumen.periodoLabel}"
            binding.tvLabelPromedio.text = "Balance${resumen.periodoLabel}"
        }
    }

    private fun mostrarBannerAlerta() {
        if (origen == "ALERTA") {
            binding.cardAlerta.visibility = View.VISIBLE
            binding.tvTipoAlerta.text = tituloAlerta
            binding.tvMensajeAlerta.text = mensajeAlerta
        } else {
            binding.cardAlerta.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

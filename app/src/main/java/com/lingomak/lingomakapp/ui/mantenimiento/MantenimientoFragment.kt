package com.lingomak.lingomakapp.ui.mantenimiento

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
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.core.content.ContextCompat
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import com.lingomak.lingomakapp.databinding.FragmentMantenimientoBinding


class MantenimientoFragment : Fragment() {

    private var _binding: FragmentMantenimientoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MantenimientoViewModel by viewModels()

    private var listaCompleta = listOf<MantenimientoModel>()

    private var filtroTipo = "TODOS"
    private var filtroEstado = "TODOS"

    private lateinit var adapter: MantenimientoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentMantenimientoBinding.inflate(inflater, container, false)

        configurarRecyclerView()
        observarViewModel()
        configurarEventos()
        configurarFiltros()
        configurarBusqueda()
        viewModel.listarMantenimientos()

        return binding.root
    }

    private fun configurarRecyclerView() {

        adapter = MantenimientoAdapter(
            listaMantenimientos = emptyList(),

            onMantenimientoClick = { mantenimiento ->
                abrirDetalleMantenimiento(mantenimiento)
            },

            onEditarClick = { mantenimiento ->
                abrirEditarMantenimiento(mantenimiento)
            },

            onCambiarEstadoClick = { mantenimiento ->
                mostrarDialogoCambiarEstado(mantenimiento)
            },
            onFinalizarClick = { mantenimiento ->
                abrirFinalizarMantenimiento(mantenimiento)
            },
            onCancelarClick = { mantenimiento ->
                mostrarDialogoCancelarMantenimiento(mantenimiento)
            }
        )

        binding.rvMantenimientos.layoutManager =
            LinearLayoutManager(requireContext())

        binding.rvMantenimientos.adapter = adapter
    }

    private fun observarViewModel(){
        viewModel.listaMantenimientos.observe(viewLifecycleOwner){lista ->
            listaCompleta = lista

            aplicarFiltros()
        }
        viewModel.mensajeError.observe(viewLifecycleOwner){ mensaje ->
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        }
    }


    private fun abrirDetalleMantenimiento(mantenimiento : MantenimientoModel){
        val fragment = DetalleMantenimientoFragment()

        val bundle = Bundle().apply {
            putString("uid", mantenimiento.uid)
            putString("uidMaquinaria", mantenimiento.uidMaquinaria)
            putString("codigoMantenimiento", mantenimiento.codigoMantenimiento)
            putString("tipoMantenimiento", mantenimiento.tipoMantenimiento)
            putString("nombreMaquinaria", mantenimiento.nombreMaquinaria)
            putString("codigoMaquinaria", mantenimiento.codigoMaquinaria)
            putString("tipoMaquinaria", mantenimiento.tipoMaquinaria)
            putString("descripcion", mantenimiento.descripcion)
            putString("fechaProgramada", mantenimiento.fechaProgramada)
            putString("responsable", mantenimiento.responsable)
            putInt("horometroProgramado", mantenimiento.horometroProgramado)
            putString("estado", mantenimiento.estado)
            putString("prioridad", mantenimiento.prioridad)
            putDouble("costoEstimado", mantenimiento.costoEstimado)
            putString("observaciones", mantenimiento.observaciones)
        }

        fragment.arguments = bundle

        requireActivity().supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainerAdmin, fragment)
            .addToBackStack(null)
            .commit()

    }
    private fun configurarEventos() {

        binding.fabAgregarMantenimiento.setOnClickListener {

            val fragment = ProgramarMantenimientoFragment()

            requireActivity().supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainerAdmin,
                    fragment
                )
                .addToBackStack(null)
                .commit()
        }
    }

    private fun configurarFiltros(){
        binding.toggleTipoMantenimiento.addOnButtonCheckedListener {  _, checkedId, isChecked ->
            if(isChecked){
                filtroTipo = when(checkedId){
                    binding.btnTipoPreventivo.id -> "PREVENTIVO"
                    binding.btnTipoCorrectivo.id -> "CORRECTIVO"
                    binding.btnTipoPredictivo.id -> "PREDICTIVO"
                    else -> "TODOS"
                }
                aplicarFiltros()
            }
        }

        binding.toggleEstadoMantenimiento.addOnButtonCheckedListener { _, checkedId, isChecked ->

            if (isChecked) {
                filtroEstado = when (checkedId) {
                    binding.btnEstadoPendiente.id -> "PENDIENTE"
                    binding.btnEstadoProceso.id -> "EN_PROCESO"
                    binding.btnEstadoFinalizado.id -> "FINALIZADO"
                    binding.btnEstadoVencido.id -> "VENCIDO"
                    else -> "TODOS"
                }

                aplicarFiltros()
            }
        }


    }

    private fun aplicarFiltros(){
        val textoBusqueda = binding.etBuscarMantenimiento.text
            .toString()
            .trim()
            .lowercase()

        val listaFiltrada = listaCompleta.filter { mantenimiento ->
            val coincideBusqueda =
                mantenimiento.nombreMaquinaria.lowercase().contains(textoBusqueda) ||
                        mantenimiento.codigoMantenimiento.lowercase().contains(textoBusqueda) ||
                        mantenimiento.descripcion.lowercase().contains(textoBusqueda)

            val coincideTipo =
                filtroTipo == "TODOS" ||
                        mantenimiento.tipoMantenimiento == filtroTipo

            val coincideEstado =
                filtroEstado == "TODOS" ||
                        mantenimiento.estado == filtroEstado

            coincideBusqueda && coincideTipo && coincideEstado
        }

        adapter.actualizarLista(listaFiltrada)
        actualizarResumen(listaFiltrada)
    }

    private fun configurarBusqueda(){
        binding.etBuscarMantenimiento.addTextChangedListener(object  : TextWatcher{
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                aplicarFiltros()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun actualizarResumen(lista: List<MantenimientoModel>) {

        binding.tvTotalMantenimientos.text = lista.size.toString()

        binding.tvPendientesMantenimiento.text =
            lista.count { it.estado == "PENDIENTE" }.toString()

        binding.tvVencidosMantenimiento.text =
            lista.count { it.estado == "VENCIDO" }.toString()

        binding.tvFinalizadosMantenimiento.text =
            lista.count { it.estado == "FINALIZADO" }.toString()
    }

    private fun mostrarDialogoCambiarEstado(mantenimiento : MantenimientoModel){
        if (mantenimiento.estado != "PENDIENTE") {

            Toast.makeText(
                requireContext(),
                "Solo los mantenimientos pendientes pueden iniciarse",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Iniciar mantenimiento")
            .setMessage(
                "¿Desea iniciar este mantenimiento?\n\n" +
                        "La maquinaria pasará al estado EN PROCESO."
            )

            .setPositiveButton("Iniciar") { dialog, _ ->

                if (mantenimiento.uidMaquinaria.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "No se encontró la maquinaria relacionada",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                viewModel.iniciarMantenimiento(
                    uidMantenimiento = mantenimiento.uid,
                    uidMaquinaria = mantenimiento.uidMaquinaria,
                    onSuccess = {
                        Toast.makeText(
                            requireContext(),
                            "Mantenimiento iniciado correctamente",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )

                dialog.dismiss()
            }

            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }

            .show()

    }

    private fun abrirEditarMantenimiento(
        mantenimiento: MantenimientoModel
    ) {

        val fragment = EditarMantenimientoFragment()

        val bundle = Bundle().apply {

            putString("uid", mantenimiento.uid)
            putString("codigoMantenimiento", mantenimiento.codigoMantenimiento)

            putString("uidMaquinaria", mantenimiento.uidMaquinaria)
            putString("codigoMaquinaria", mantenimiento.codigoMaquinaria)
            putString("nombreMaquinaria", mantenimiento.nombreMaquinaria)
            putString("tipoMaquinaria", mantenimiento.tipoMaquinaria)

            putString("tipoMantenimiento", mantenimiento.tipoMantenimiento)
            putString("descripcion", mantenimiento.descripcion)
            putString("fechaProgramada", mantenimiento.fechaProgramada)

            putString("responsable", mantenimiento.responsable)
            putString("estado", mantenimiento.estado)
            putString("prioridad", mantenimiento.prioridad)

            putInt(
                "horometroProgramado",
                mantenimiento.horometroProgramado
            )

            putDouble(
                "costoEstimado",
                mantenimiento.costoEstimado
            )

            putString(
                "observaciones",
                mantenimiento.observaciones
            )

            putString(
                "fechaRegistro",
                mantenimiento.fechaRegistro
            )

            putString(
                "registradoPor",
                mantenimiento.registradoPor
            )
        }

        fragment.arguments = bundle

        requireActivity().supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragmentContainerAdmin,
                fragment
            )
            .addToBackStack(null)
            .commit()
    }

    private fun mostrarDialogoCancelarMantenimiento(mantenimiento: MantenimientoModel) {
        if (
            mantenimiento.estado == "EN_PROCESO" ||
            mantenimiento.estado == "FINALIZADO" ||
            mantenimiento.estado == "CANCELADO"
        ) {
            Toast.makeText(
                requireContext(),
                "No se puede cancelar un mantenimiento en estado ${mantenimiento.estado}",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Cancelar mantenimiento")
            .setMessage("¿Seguro que deseas cancelar este mantenimiento?")
            .setPositiveButton("Sí, cancelar") { dialog, _ ->

                viewModel.cambiarEstadoMantenimiento(
                    uid = mantenimiento.uid,
                    nuevoEstado = "CANCELADO",
                    onSuccess = {
                        Toast.makeText(
                            requireContext(),
                            "Mantenimiento cancelado correctamente",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )

                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun abrirFinalizarMantenimiento(mantenimiento : MantenimientoModel){
        if(mantenimiento.estado != "EN_PROCESO"){
            Toast.makeText(requireContext(), "Solo se puede finalizar un mantenimiento en proceso", Toast.LENGTH_SHORT).show()
            return
        }

        val fragment = FinalizarMantenimientoFragment()

        val bundle = Bundle().apply {
            putString("uid", mantenimiento.uid)
            putString("uidMaquinaria", mantenimiento.uidMaquinaria)
            putString("codigoMantenimiento", mantenimiento.codigoMantenimiento)
            putString("nombreMaquinaria", mantenimiento.nombreMaquinaria)
            putString("descripcion", mantenimiento.descripcion)
            putString("estado", mantenimiento.estado)
        }

        fragment.arguments = bundle
        requireActivity().supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainerAdmin, fragment)
            .addToBackStack(null)
            .commit()

    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}

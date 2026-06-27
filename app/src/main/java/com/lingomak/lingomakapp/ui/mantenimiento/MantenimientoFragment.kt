package com.lingomak.lingomakapp.ui.mantenimiento

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.lingomak.lingomakapp.data.model.MantenimientoModel
import com.lingomak.lingomakapp.databinding.FragmentMantenimientoBinding
import com.lingomak.lingomakapp.R


class MantenimientoFragment : Fragment() {

    private var _binding: FragmentMantenimientoBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: MantenimientoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentMantenimientoBinding.inflate(inflater, container, false)

        configurarRecyclerView()

        cargarDatosPrueba()

        configurarEventos()

        return binding.root
    }

    private fun configurarRecyclerView() {

        adapter = MantenimientoAdapter(
            emptyList()
        ) { mantenimiento ->

            val fragment = DetalleMantenimientoFragment()

            val bundle = Bundle().apply {

                putString(
                    "codigoMantenimiento",
                    mantenimiento.codigoMantenimiento
                )

                putString(
                    "tipoMantenimiento",
                    mantenimiento.tipoMantenimiento
                )

                putString(
                    "nombreMaquinaria",
                    mantenimiento.nombreMaquinaria
                )

                putString(
                    "descripcion",
                    mantenimiento.descripcion
                )

                putString(
                    "fechaProgramada",
                    mantenimiento.fechaProgramada
                )

                putString(
                    "responsable",
                    mantenimiento.responsable
                )

                putInt(
                    "horometroProgramado",
                    mantenimiento.horometroProgramado
                )

                putString(
                    "estado",
                    mantenimiento.estado
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

        binding.rvMantenimientos.layoutManager =
            LinearLayoutManager(requireContext())

        binding.rvMantenimientos.adapter = adapter
    }

    private fun cargarDatosPrueba() {

        val lista = listOf(

            MantenimientoModel(
                uid = "1",
                codigoMantenimiento = "MAN-0001",
                nombreMaquinaria = "Excavadora CAT 320",
                tipoMantenimiento = "PREVENTIVO",
                descripcion = "Cambio de aceite y filtros",
                fechaProgramada = "15/07/2026",
                estado = "PENDIENTE",
                responsable = "Luis García",
                horometroProgramado = 3500
            ),

            MantenimientoModel(
                uid = "2",
                codigoMantenimiento = "MAN-0002",
                nombreMaquinaria = "Volquete Volvo FMX",
                tipoMantenimiento = "CORRECTIVO",
                descripcion = "Reparación sistema hidráulico",
                fechaProgramada = "12/07/2026",
                estado = "EN_PROCESO",
                responsable = "Carlos Ruiz",
                horometroProgramado = 4200
            ),

            MantenimientoModel(
                uid = "3",
                codigoMantenimiento = "MAN-0003",
                nombreMaquinaria = "Cargador Frontal CAT",
                tipoMantenimiento = "PREDICTIVO",
                descripcion = "Inspección por vibración",
                fechaProgramada = "08/07/2026",
                estado = "FINALIZADO",
                responsable = "Miguel Torres",
                horometroProgramado = 2800
            ),

            MantenimientoModel(
                uid = "4",
                codigoMantenimiento = "MAN-0004",
                nombreMaquinaria = "Retroexcavadora JCB",
                tipoMantenimiento = "PREVENTIVO",
                descripcion = "Mantenimiento general",
                fechaProgramada = "05/07/2026",
                estado = "VENCIDO",
                responsable = "Juan Pérez",
                horometroProgramado = 5100
            )
        )

        adapter.actualizarLista(lista)


        binding.tvTotalMantenimientos.text = lista.size.toString()

        binding.tvPendientesMantenimiento.text =
            lista.count { it.estado == "PENDIENTE" }.toString()

        binding.tvVencidosMantenimiento.text =
            lista.count { it.estado == "VENCIDO" }.toString()

        binding.tvFinalizadosMantenimiento.text =
            lista.count { it.estado == "FINALIZADO" }.toString()
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

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}
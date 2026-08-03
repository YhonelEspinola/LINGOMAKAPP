package com.lingomak.lingomakapp.ui.usuarios

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.lingomak.lingomakapp.data.model.UserModel
import com.lingomak.lingomakapp.databinding.FragmentUsuariosBinding
import com.lingomak.lingomakapp.utils.Constants
import androidx.appcompat.app.AlertDialog

class UsuariosFragment : Fragment() {

    private var _binding: FragmentUsuariosBinding? = null

    private val binding get() = _binding!!

    private val viewModel: UsuariosViewModel by viewModels()

    private lateinit var adapter: UsuariosAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {


        _binding = FragmentUsuariosBinding.inflate(inflater, container, false)


        configurarRecyclerView()

        observarViewModel()

        viewModel.listarUsuarios()

        configurarEventos()

        return binding.root
    }

    private fun configurarRecyclerView() {


        adapter = UsuariosAdapter(
            listaUsuarios = emptyList(),

            onEditarClick = { usuario ->
                val fragment = EditarUsuarioFragment()

                val bundle = Bundle()

                bundle.putString("uid", usuario.uid)
                bundle.putString("nombre", usuario.nombre)
                bundle.putString("correo", usuario.correo)
                bundle.putString("rol", usuario.rol)

                fragment.arguments = bundle

                parentFragmentManager.beginTransaction()
                    .replace(
                        com.lingomak.lingomakapp.R.id.fragmentContainerAdmin,
                        fragment
                    )
                    .addToBackStack(null)
                    .commit()

            },

            onCambiarEstadoClick = { usuario ->
                mostrarDialogoCambiarEstado(usuario)
            }
        )


        binding.rvUsuarios.layoutManager = LinearLayoutManager(requireContext())


        binding.rvUsuarios.adapter = adapter
    }

    private fun observarViewModel() {


        viewModel.usuarios.observe(viewLifecycleOwner) { listaUsuarios ->
            adapter.actualizarLista(listaUsuarios)
        }

        viewModel.error.observe(viewLifecycleOwner) { mensaje ->
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        }
    }

    private fun configurarEventos(){
        // Toggle Filtros Colapsable
        binding.btnToggleFiltros.setOnClickListener {
            val currentlyVisible = binding.layoutFiltrosExpandible.visibility == View.VISIBLE
            val nextVisibility = if (currentlyVisible) View.GONE else View.VISIBLE
            binding.layoutFiltrosExpandible.visibility = nextVisibility

            // Animación del chevron
            binding.ivChevronFiltros.animate()
                .rotation(if (currentlyVisible) 0f else 180f)
                .setDuration(200)
                .start()
        }

        // Filtro por Rol
        binding.chipGroupRol.setOnCheckedStateChangeListener { _, checkedIds ->
            val selection = when (checkedIds.firstOrNull()) {
                com.lingomak.lingomakapp.R.id.chipRolAdmin -> "ADMIN"
                com.lingomak.lingomakapp.R.id.chipRolOperario -> "OPERARIO"
                else -> "TODOS"
            }
            viewModel.filtrarPorRol(selection)
        }

        // Filtro por Estado
        binding.chipGroupEstado.setOnCheckedStateChangeListener { _, checkedIds ->
            val selection = when (checkedIds.firstOrNull()) {
                com.lingomak.lingomakapp.R.id.chipEstadoActivo -> com.lingomak.lingomakapp.utils.Constants.ESTADO_ACTIVO
                com.lingomak.lingomakapp.R.id.chipEstadoInactivo -> com.lingomak.lingomakapp.utils.Constants.ESTADO_INACTIVO
                else -> "TODOS"
            }
            viewModel.filtrarPorEstado(selection)
        }

        // Búsqueda
        binding.etBuscarUsuarios.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.buscarUsuario(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        binding.fabAgregarUsuario.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    com.lingomak.lingomakapp.R.id.fragmentContainerAdmin,
                    AgregarUsuarioFragment()
                )
                .addToBackStack(null)
                .commit()
        }
    }

    private fun mostrarDialogoCambiarEstado(usuario : UserModel){
            val nuevoEstado =
                if( usuario.estado == Constants.ESTADO_ACTIVO){
                    Constants.ESTADO_INACTIVO
                }else{
                    Constants.ESTADO_ACTIVO
                }

        val mensaje =
            if(nuevoEstado == Constants.ESTADO_INACTIVO){
                "Deseas desactivar a ${usuario.nombre}"
            }else {
                "Deseas activar a ${usuario.nombre}"
            }

        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar acción")
            .setMessage(mensaje)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aceptar") { _, _ ->
                viewModel.cambiarEstadoUsuario(usuario.uid, nuevoEstado)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}
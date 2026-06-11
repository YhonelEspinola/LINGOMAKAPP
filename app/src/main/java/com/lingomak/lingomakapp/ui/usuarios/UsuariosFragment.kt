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
                        (requireView().parent as ViewGroup).id,
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
        binding.fabAgregarUsuario.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    (requireView().parent as ViewGroup).id,
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
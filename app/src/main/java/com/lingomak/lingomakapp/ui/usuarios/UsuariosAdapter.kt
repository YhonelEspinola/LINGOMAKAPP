package com.lingomak.lingomakapp.ui.usuarios

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.lingomak.lingomakapp.data.model.UserModel
import com.lingomak.lingomakapp.databinding.ItemUsuarioBinding

class UsuariosAdapter(
    private var listaUsuarios: List<UserModel>,
    private val onEditarClick: (UserModel) -> Unit,
    private val onCambiarEstadoClick: (UserModel) -> Unit
) : RecyclerView.Adapter<UsuariosAdapter.UsuarioViewHolder>() {

    inner class UsuarioViewHolder(
        private val binding: ItemUsuarioBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(usuario: UserModel) {
            binding.tvNombre.text = usuario.nombre
            binding.tvCorreo.text = usuario.correo
            binding.tvRol.text = usuario.rol
            
            // Si está ACTIVO no se muestra texto, si está INACTIVO se muestra en rojo
            if (usuario.estado.equals("ACTIVO", ignoreCase = true)) {
                binding.tvEstado.visibility = android.view.View.GONE
                binding.ivCambiarEstado.setImageResource(com.lingomak.lingomakapp.R.drawable.ic_person_off)
                binding.ivCambiarEstado.setColorFilter(ContextCompat.getColor(binding.root.context, com.lingomak.lingomakapp.R.color.danger))
            } else {
                binding.tvEstado.visibility = android.view.View.VISIBLE
                binding.tvEstado.text = "INACTIVO"
                binding.tvEstado.setTextColor(ContextCompat.getColor(binding.root.context, com.lingomak.lingomakapp.R.color.danger))
                binding.ivCambiarEstado.setImageResource(com.lingomak.lingomakapp.R.drawable.person)
                binding.ivCambiarEstado.setColorFilter(ContextCompat.getColor(binding.root.context, com.lingomak.lingomakapp.R.color.success))
            }

            binding.ivEditar.setOnClickListener {
                onEditarClick(usuario)
            }


            binding.ivCambiarEstado.setOnClickListener {
                onCambiarEstadoClick(usuario)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {

        val binding = ItemUsuarioBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return UsuarioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {

        holder.bind(listaUsuarios[position])
    }

    override fun getItemCount(): Int {

        return listaUsuarios.size
    }

    fun actualizarLista(nuevaLista: List<UserModel>) {
        listaUsuarios = nuevaLista
        notifyDataSetChanged()
    }


}
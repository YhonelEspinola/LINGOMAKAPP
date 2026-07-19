package com.lingomak.lingomakapp.ui.mantenimiento

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.RepuestoModel

class RepuestoDropdownAdapter(
    private val context: Context,
    private val allRepuestos: List<RepuestoModel>
) : BaseAdapter(), Filterable {

    private var filteredRepuestos: List<RepuestoModel> = allRepuestos

    override fun getCount(): Int = filteredRepuestos.size

    override fun getItem(position: Int): RepuestoModel = filteredRepuestos[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_repuesto_dropdown, parent, false)

        val repuesto = getItem(position)
        
        view.findViewById<TextView>(R.id.tvNombreRepuesto).text = repuesto.nombre
        view.findViewById<TextView>(R.id.tvCodigoRepuesto).text = repuesto.codigoInterno
        view.findViewById<TextView>(R.id.tvStockRepuesto).text = "Stock: ${repuesto.stockActual}"

        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                val query = constraint?.toString()?.lowercase()?.trim() ?: ""

                val filtered = if (query.isEmpty()) {
                    allRepuestos
                } else {
                    allRepuestos.filter {
                        it.nombre.lowercase().contains(query) || 
                        it.codigoInterno.lowercase().contains(query)
                    }
                }

                results.values = filtered
                results.count = filtered.size
                return results
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredRepuestos = results?.values as? List<RepuestoModel> ?: emptyList()
                notifyDataSetChanged()
            }

            override fun convertResultToString(resultValue: Any?): CharSequence {
                return (resultValue as? RepuestoModel)?.nombre ?: ""
            }
        }
    }
}

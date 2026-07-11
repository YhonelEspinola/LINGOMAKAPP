package com.lingomak.lingomakapp.ui.repuestos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.lingomak.lingomakapp.utils.ImageOptimizer
import com.lingomak.lingomakapp.R
import com.lingomak.lingomakapp.data.model.RepuestoModel
import com.lingomak.lingomakapp.databinding.FragmentDetalleRepuestoBinding
import java.io.FileOutputStream
import java.io.IOException

/**
 * Fragment que muestra el detalle completo y estático (no editable)
 * de un repuesto: datos generales, imagen, código QR y acciones
 * (Editar, Activar/Inactivar, Volver).
 */
class DetalleRepuestoFragment : Fragment() {

    private var _binding: FragmentDetalleRepuestoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventarioViewModel by activityViewModels()

    private var uidRepuesto: String = ""
    private var repuestoActual: RepuestoModel? = null

    private var origen = ""
    private var tituloAlerta = ""
    private var mensajeAlerta = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetalleRepuestoBinding.inflate(inflater, container, false)
        uidRepuesto = arguments?.getString("uid") ?: ""

        origen = arguments?.getString("origen") ?: ""
        tituloAlerta = arguments?.getString("tituloAlerta") ?: ""
        mensajeAlerta = arguments?.getString("mensajeAlerta") ?: ""

        observarViewModel()
        viewModel.obtenerRepuestoPorUid(uidRepuesto)
        configurarEventos()
        return binding.root
    }

    private fun observarViewModel() {
        viewModel.repuestoSeleccionado.observe(viewLifecycleOwner) { repuesto ->
            if (repuesto != null) {
                repuestoActual = repuesto
                pintarDatos(repuesto)
            }
        }
        viewModel.error.observe(viewLifecycleOwner) { mensaje ->
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        }
    }

    private fun pintarDatos(repuesto: RepuestoModel) {
        binding.tvNombre.text = repuesto.nombre
        binding.tvCodigoInterno.text = repuesto.codigoInterno
        binding.tvMarca.text = repuesto.marca
        binding.tvCategoria.text = repuesto.categoria
        binding.tvDescripcion.text = repuesto.descripcion.ifEmpty { "Sin descripción" }
        binding.tvStockActual.text = repuesto.stockActual.toString()
        binding.tvStockMinimo.text = repuesto.stockMinimo.toString()
        binding.tvStockMaximo.text = repuesto.stockMaximo.toString()
        binding.tvUbicacionAlmacen.text = repuesto.ubicacionAlmacen.ifEmpty { "No especificada" }
        binding.tvEstado.text = repuesto.estado

        val colorEstado = if (repuesto.estado == "ACTIVO") R.color.success else R.color.danger
        binding.tvEstado.setTextColor(requireContext().getColor(colorEstado))

        if (repuesto.imagenUrl.isNotEmpty()) {
            val optimizedUrl = ImageOptimizer.getOptimizedUrl(repuesto.imagenUrl, "1024x1024")
            Glide.with(this)
                .load(optimizedUrl)
                // Cargamos la original como miniatura para que aparezca INSTANTÁNEAMENTE 
                // mientras se descarga la de 1024px
                .thumbnail(Glide.with(this).load(repuesto.imagenUrl).override(300))
                .error(Glide.with(this).load(repuesto.imagenUrl))
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(binding.ivImagenRepuesto)
        } else {
            binding.ivImagenRepuesto.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        if (repuesto.codigoQR.isNotEmpty()) {
            Glide.with(this).load(repuesto.codigoQR).placeholder(android.R.drawable.ic_menu_gallery).error(android.R.drawable.ic_menu_gallery).into(binding.ivCodigoQR)
        } else {
            binding.ivCodigoQR.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        if (origen == "ALERTA") {

            binding.cardAlerta.visibility = View.VISIBLE

            binding.tvTipoAlerta.text = tituloAlerta

            binding.tvMensajeAlerta.text = mensajeAlerta

        } else {

            binding.cardAlerta.visibility = View.GONE
        }

    }

    private fun configurarEventos() {
        // Ocultar botones de edición para Operadores
        if (requireActivity() is com.lingomak.lingomakapp.ui.dashboard.DashboardOperarioActivity) {
            binding.btnEditar.visibility = View.GONE
            binding.btnCambiarEstado.visibility = View.GONE
            binding.btnVerMovimientos.visibility = View.GONE
        }

        binding.ivBotonRegresar.setOnClickListener { parentFragmentManager.popBackStack() }
        
        binding.btnImprimirQR.setOnClickListener {
            imprimirQR()
        }

        binding.btnVerMovimientos.setOnClickListener {
            val fragment = MovimientosFragment()
            val bundle = Bundle().apply {
                putString("repuestoUid", uidRepuesto)
                putString("nombreRepuesto", repuestoActual?.nombre)
                putInt("stockActual", repuestoActual?.stockActual ?: 0)
            }
            fragment.arguments = bundle
            parentFragmentManager.beginTransaction().replace((requireView().parent as ViewGroup).id, fragment).addToBackStack(null).commit()
        }

        binding.btnEditar.setOnClickListener {
            val fragment = EditarRepuestoFragment()
            val bundle = Bundle()
            bundle.putString("uid", uidRepuesto)
            fragment.arguments = bundle
            parentFragmentManager.beginTransaction().replace((requireView().parent as ViewGroup).id, fragment).addToBackStack(null).commit()
        }

        binding.btnCambiarEstado.setOnClickListener {
            repuestoActual?.let { mostrarDialogoCambiarEstado(it) }
        }

        binding.btnVolver.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun imprimirQR() {
        val drawable = binding.ivCodigoQR.drawable
        if (drawable is BitmapDrawable && repuestoActual != null) {
            val bitmap = drawable.bitmap
            val printManager = requireContext().getSystemService(Context.PRINT_SERVICE) as PrintManager
            val jobName = "${getString(R.string.app_name)} - QR ${repuestoActual?.codigoInterno}"
            
            printManager.print(jobName, object : PrintDocumentAdapter() {
                override fun onLayout(oldAttributes: PrintAttributes?, newAttributes: PrintAttributes, cancellationSignal: CancellationSignal?, callback: LayoutResultCallback, extras: Bundle?) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback.onLayoutCancelled()
                        return
                    }
                    val info = PrintDocumentInfo.Builder("qr_label.pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(1)
                        .build()
                    callback.onLayoutFinished(info, true)
                }

                override fun onWrite(pages: Array<out PageRange>?, destination: ParcelFileDescriptor, cancellationSignal: CancellationSignal?, callback: WriteResultCallback) {
                    val pdfDocument = PdfDocument()
                    // Tamaño estándar de etiqueta pequeña (200 x 250 puntos)
                    val pageInfo = PdfDocument.PageInfo.Builder(200, 250, 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas
                    val paint = Paint()

                    // Dibujar fondo blanco
                    paint.color = Color.WHITE
                    canvas.drawRect(0f, 0f, 200f, 250f, paint)

                    // Dibujar el QR escalado
                    paint.isFilterBitmap = true
                    val qrRect = android.graphics.Rect(20, 10, 180, 170)
                    canvas.drawBitmap(bitmap, null, qrRect, paint)

                    // Dibujar textos
                    paint.color = Color.BLACK
                    paint.textSize = 12f
                    paint.isFakeBoldText = true
                    canvas.drawText(repuestoActual?.nombre ?: "", 20f, 190f, paint)
                    
                    paint.textSize = 10f
                    paint.isFakeBoldText = false
                    canvas.drawText("Cod: ${repuestoActual?.codigoInterno}", 20f, 210f, paint)
                    canvas.drawText("Cat: ${repuestoActual?.categoria}", 20f, 225f, paint)

                    pdfDocument.finishPage(page)

                    try {
                        pdfDocument.writeTo(FileOutputStream(destination.fileDescriptor))
                    } catch (e: IOException) {
                        callback.onWriteFailed(e.toString())
                        return
                    } finally {
                        pdfDocument.close()
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                }
            }, null)
        } else {
            Toast.makeText(requireContext(), "QR no disponible para imprimir", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarDialogoCambiarEstado(repuesto: RepuestoModel) {
        val nuevoEstado = if (repuesto.estado == "ACTIVO") "INACTIVO" else "ACTIVO"
        val mensaje = if (nuevoEstado == "INACTIVO") "Deseas inactivar ${repuesto.nombre}" else "Deseas activar ${repuesto.nombre}"
        AlertDialog.Builder(requireContext()).setTitle("Confirmar acción").setMessage(mensaje).setNegativeButton("Cancelar", null).setPositiveButton("Aceptar") { _, _ ->
            viewModel.cambiarEstadoRepuesto(repuesto.uid, nuevoEstado)
            viewModel.obtenerRepuestoPorUid(repuesto.uid)
        }.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

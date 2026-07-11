package com.lingomak.lingomakapp.utils

object ImageOptimizer {

    /**
     * Obtiene la URL de la miniatura que está en la misma carpeta que el original.
     * Estructura: folder/filename_size.webp
     */
    fun getOptimizedUrl(originalUrl: String, size: String = "512x512"): String {
        if (originalUrl.isEmpty() || !originalUrl.contains("firebasestorage")) return originalUrl

        return try {
            // Ejemplo: .../maquinarias%2Ffoto.jpg?alt=media
            val baseUrl = originalUrl.substringBefore("?")
            
            // Quitamos la extensión (.jpg o .png)
            val pathWithoutExt = baseUrl.substringBeforeLast(".")
            
            // Añadimos el sufijo del tamaño y la nueva extensión .webp
            // Resultado: .../maquinarias%2Ffoto_512x512.webp?alt=media
            "$pathWithoutExt" + "_" + size + ".webp?alt=media"
        } catch (e: Exception) {
            originalUrl
        }
    }
}
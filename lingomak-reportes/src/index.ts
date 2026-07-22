import {onCall, HttpsError} from "firebase-functions/v2/https";
import {defineSecret} from "firebase-functions/params";
import {setGlobalOptions} from "firebase-functions";
import * as logger from "firebase-functions/logger";

// Configuración global
setGlobalOptions({
  region: "us-central1",
  maxInstances: 10,
});

const groqApiKey = defineSecret("GROQ_API_KEY");

export const generarReporteMantenimiento = onCall({
  secrets: [groqApiKey],
  enforceAppCheck: true,
}, async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Usuario no autenticado.");
  }

  const {
    tipoMantenimiento, descripcion, observaciones,
    fechaRealizada, horometroReal, costoReal,
    nombreMaquinaria, insumosStr,
  } = request.data;

  // Prompt actualizado para solicitar JSON estructurado
  const prompt = `
    Eres un experto senior en mantenimiento de maquinaria pesada.
    Genera un reporte técnico profesional basado en estos datos:
    Maquinaria: ${nombreMaquinaria} (${tipoMantenimiento})
    Fecha: ${fechaRealizada} | Horómetro: ${horometroReal} h | Costo: S/ ${costoReal}
    Descripción: ${descripcion} | Observaciones: ${observaciones}
    Insumos: ${insumosStr}

    RESPONDE EXCLUSIVAMENTE EN FORMATO JSON siguiendo este esquema exacto:
    {
      "sintoma": "descripción técnica del estado inicial o falla",
      "causa": "deducción de la causa raíz basada en uso y datos",
      "acciones": "detalle del proceso técnico e insumos aplicados",
      "resultado": "estado operativo final y recomendaciones"
    }
    No incluyas markdown, no incluyas texto fuera del JSON.
  `;

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 15000);

  try {
    const groqUrl = "https://api.groq.com/openai/v1/chat/completions";
    const response = await fetch(groqUrl, {
      method: "POST",
      signal: controller.signal,
      headers: {
        "Authorization": `Bearer ${groqApiKey.value()}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        model: "llama-3.1-8b-instant",
        messages: [
          {
            role: "system",
            content: "Eres un asistente técnico que solo responde con objetos JSON válidos."
          },
          {role: "user", content: prompt},
        ],
        temperature: 0.1, // Reducimos temperatura para asegurar formato JSON
        max_tokens: 1500,
        response_format: { "type": "json_object" } // Groq soporta modo JSON
      }),
    });

    clearTimeout(timeoutId);

    if (!response.ok) {
      const errorData = await response.json();
      logger.error("Error de Groq API", errorData);
      throw new HttpsError("unavailable", "El motor de IA no responde.");
    }

    const json: any = await response.json();
    const content = json.choices[0]?.message?.content;

    if (!content) throw new HttpsError("internal", "IA sin contenido.");

    // Parseamos para validar que sea JSON antes de devolverlo
    const parsedContent = JSON.parse(content);
    return { reporte: parsedContent };

  } catch (error: any) {
    clearTimeout(timeoutId);
    if (error.name === "AbortError") {
      throw new HttpsError("deadline-exceeded", "La IA tardó demasiado.");
    }
    logger.error("Error en generarReporteMantenimiento", error);
    if (error instanceof HttpsError) throw error;
    throw new HttpsError("internal", "Error al procesar el reporte estructurado.");
  }
});

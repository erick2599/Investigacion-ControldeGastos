package com.example.gastos.metrics

import android.os.SystemClock
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MetricasRendimiento {

    private const val ARCHIVO_CSV = "resultados_benchmark.csv"
    private val formatoFecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    data class Medicion(
        val tecnologia: String,     
        val operacion: String,      
        val volumen: Int,           
        val repeticion: Int,        
        val esCalentamiento: Boolean,
        val tiempoMs: Double,       
        val memoriaAntesKb: Long,   
        val memoriaDespuesKb: Long, 
        val exitosa: Boolean,
        val nota: String = ""
    )

    inline fun medirTiempo(bloque: () -> Unit): Double {
        val inicio = SystemClock.elapsedRealtimeNanos()
        bloque()
        val fin = SystemClock.elapsedRealtimeNanos()
        return (fin - inicio) / 1_000_000.0 
    }

    fun memoriaActualKb(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024
    }

    fun ejecutarYMedir(
        tecnologia: String,
        operacion: String,
        volumen: Int,
        repeticion: Int,
        esCalentamiento: Boolean = false,
        nota: String = "",
        operacionFuncional: () -> Unit
    ): Medicion {
        val memoriaAntes = memoriaActualKb()
        return try {
            val tiempo = medirTiempo(operacionFuncional)
            val memoriaDespues = memoriaActualKb()
            Medicion(
                tecnologia, operacion, volumen, repeticion, esCalentamiento,
                tiempo, memoriaAntes, memoriaDespues, true, nota
            )
        } catch (e: Exception) {
            Medicion(
                tecnologia, operacion, volumen, repeticion, esCalentamiento,
                -1.0, memoriaAntes, memoriaActualKb(), false,
                "Error: ${e.message}"
            )
        }
    }

    fun guardarCSV(directorioSalida: File, mediciones: List<Medicion>) {
        val archivo = File(directorioSalida, ARCHIVO_CSV)
        val esNuevo = !archivo.exists()
        FileWriter(archivo, true).use { writer ->
            if (esNuevo) {
                writer.append("fecha_hora,tecnologia,operacion,volumen,repeticion,")
                writer.append("es_calentamiento,tiempo_ms,memoria_antes_kb,memoria_despues_kb,")
                writer.append("exitosa,nota\n")
            }
            mediciones.forEach { m ->
                writer.append("${formatoFecha.format(Date())},")
                writer.append("${m.tecnologia},${m.operacion},${m.volumen},${m.repeticion},")
                writer.append("${m.esCalentamiento},${"%.3f".format(m.tiempoMs)},")
                writer.append("${m.memoriaAntesKb},${m.memoriaDespuesKb},")
                writer.append("${m.exitosa},\"${m.nota}\"\n")
            }
        }
    }

    fun resumen(mediciones: List<Medicion>): Map<String, Double> {
        val tiempos = mediciones.filter { it.exitosa }.map { it.tiempoMs }.sorted()
        if (tiempos.isEmpty()) return emptyMap()
        val n = tiempos.size
        val media = tiempos.average()
        val mediana = if (n % 2 == 0)
            (tiempos[n / 2 - 1] + tiempos[n / 2]) / 2.0
        else tiempos[n / 2]
        val desviacion = kotlin.math.sqrt(tiempos.map { (it - media) * (it - media) }.sum() / n)
        return mapOf(
            "n" to n.toDouble(),
            "media" to media,
            "mediana" to mediana,
            "desviacion" to desviacion,
            "min" to tiempos.first(),
            "max" to tiempos.last(),
            "p90" to percentil(tiempos, 90.0),
            "p95" to percentil(tiempos, 95.0)
        )
    }

    private fun percentil(ordenados: List<Double>, p: Double): Double {
        if (ordenados.isEmpty()) return 0.0
        val idx = (p / 100.0 * (ordenados.size - 1)).toInt()
        return ordenados[idx]
    }
}

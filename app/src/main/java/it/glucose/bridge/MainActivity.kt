package it.glucose.bridge

import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

class MainActivity : ComponentActivity() {

    // URL della cartella dove stanno batch.php / preview.php (senza slash finale)
    private val BASE_URL = "https://TUO_DOMINIO/PERCORSO_WEBAPP"

    private val http = OkHttpClient()

    private lateinit var status: TextView

    private val permissions = setOf(
        HealthPermission.getWritePermission(BloodGlucoseRecord::class)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        status = TextView(this).apply {
            textSize = 16f
            text = "Avvio..."
            setPadding(32, 32, 32, 32)
        }
        setContentView(status)

        val uri: Uri? = intent?.data
        val token = uri?.getQueryParameter("token")

        lifecycleScope.launch {
            try {
                val sdkStatus = HealthConnectClient.getSdkStatus(this@MainActivity)
                if (sdkStatus != HealthConnectClient.SDK_AVAILABLE) {
                    status.text = "Health Connect non disponibile (status=$sdkStatus)."
                    return@launch
                }

                val client = HealthConnectClient.getOrCreate(this@MainActivity)

                val granted = client.permissionController.getGrantedPermissions()
                if (!granted.containsAll(permissions)) {
                    status.text =
                        "Permessi mancanti. Vai in Health Connect → App con accesso → Glucose Bridge → abilita Scrittura Glicemia."
                    return@launch
                }

                if (token.isNullOrBlank()) {
                    status.text = "Apri l’app dal pulsante Import della webapp (preview.php)."
                    return@launch
                }

                status.text = "Scarico batch..."
                val batch = withContext(Dispatchers.IO) { fetchBatch(token) }

                val items = batch.optJSONArray("items")
                if (items == null || items.length() == 0) {
                    status.text = "Batch vuoto o non valido."
                    return@launch
                }

                val records = ArrayList<BloodGlucoseRecord>(items.length())
                for (i in 0 until items.length()) {
                    val obj = items.getJSONObject(i)
                    val iso = obj.optString("datetime_iso", "")
                    val mmol = obj.optDouble("value_mmol", -1.0)
                    if (iso.isBlank() || mmol <= 0) continue

                    val t = parseToInstant(iso)

                    // Firma corretta per connect-client 1.1.0-alpha10:
                    // BloodGlucoseRecord(double, String?, String?, String?, Instant, ZoneOffset?, Metadata)
                    records.add(
                        BloodGlucoseRecord(
                            mmol,
                            null, // specimenSource
                            null, // mealType
                            null, // relationToMeal
                            t,
                            ZoneOffset.UTC,
                            Metadata()
                        )
                    )
                }

                if (records.isEmpty()) {
                    status.text = "Nessun record importabile."
                    return@launch
                }

                status.text = "Scrivo su Health Connect (${records.size})..."
                client.insertRecords(records)

                status.text = "Import completato: ${records.size} record."
            } catch (e: Exception) {
                status.text = "Errore: ${e.javaClass.simpleName}: ${e.message}"
            }
        }
    }

    private fun fetchBatch(token: String): JSONObject {
        val url = "${BASE_URL.trimEnd('/')}/batch.php?token=$token"
        val req = Request.Builder().url(url).get().build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}")
            val body = resp.body?.string() ?: throw RuntimeException("Risposta vuota")
            return JSONObject(body)
        }
    }

    private fun parseToInstant(iso: String): Instant {
        return try {
            OffsetDateTime.parse(iso).toInstant()
        } catch (_: Exception) {
            Instant.parse(iso)
        }
    }
}

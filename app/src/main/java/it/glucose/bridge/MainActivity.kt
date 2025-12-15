package it.glucose.bridge

import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.metadata.Metadata
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

class MainActivity : ComponentActivity() {

    // METTI QUI l’URL della cartella dove stanno batch.php / preview.php (senza slash finale)
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

        try {
            val sdkStatus = HealthConnectClient.getSdkStatus(this)
            if (sdkStatus != HealthConnectClient.SDK_AVAILABLE) {
                status.text = "Health Connect non disponibile (status=$sdkStatus)."
                return
            }

            val client = HealthConnectClient.getOrCreate(this)

            val granted = client.permissionController.getGrantedPermissions()
            if (!granted.containsAll(permissions)) {
                status.text =
                    "Permessi mancanti. Vai in Health Connect → App con accesso → Glucose Bridge → abilita Scrittura Glicemia."
                return
            }

            if (token.isNullOrBlank()) {
                status.text = "Apri l’app dal pulsante Import della webapp (preview.php)."
                return
            }

            status.text = "Scarico batch..."

            Thread {
                try {
                    val batch = fetchBatch(token)
                    val items = batch.optJSONArray("items")
                    if (items == null || items.length() == 0) {
                        runOnUiThread { status.text = "Batch vuoto o non valido." }
                        return@Thread
                    }

                    val records = ArrayList<BloodGlucoseRecord>(items.length())
                    for (i in 0 until items.length()) {
                        val obj = items.getJSONObject(i)
                        val iso = obj.optString("datetime_iso", "")
                        val mmol = obj.optDouble("value_mmol", -1.0)
                        if (iso.isBlank() || mmol <= 0) continue

                        val t = parseToInstant(iso)
                        records.add(
                            BloodGlucoseRecord(
                                time = t,
                                zoneOffset = ZoneOffset.UTC,
                                level = BloodGlucoseRecord.BloodGlucose(mmolPerL = mmol),
                                specimenSource = BloodGlucoseRecord.SpecimenSource.CAPILLARY_BLOOD,
                                relationToMeal = BloodGlucoseRecord.RelationToMeal.UNKNOWN,
                                metadata = Metadata()
                            )
                        )
                    }

                    if (records.isEmpty()) {
                        runOnUiThread { status.text = "Nessun record importabile." }
                        return@Thread
                    }

                    runOnUiThread { status.text = "Scrivo su Health Connect (${records.size})..." }
                    client.insertRecords(records)
                    runOnUiThread { status.text = "Import completato: ${records.size} record." }

                } catch (e: Exception) {
                    runOnUiThread { status.text = "Errore: ${e.javaClass.simpleName}: ${e.message}" }
                }
            }.start()

        } catch (e: Exception) {
            status.text = "Errore iniziale: ${e.javaClass.simpleName}: ${e.message}"
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

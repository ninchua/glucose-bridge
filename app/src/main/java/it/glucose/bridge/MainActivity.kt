package it.glucose.bridge

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset

class MainActivity : ComponentActivity() {

    private lateinit var hc: HealthConnectClient

    private val permissions = setOf(
        HealthPermission.getWritePermission(BloodGlucoseRecord::class)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // UI minima senza XML (niente R.*)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }
        val txtStatus = TextView(this).apply { text = "Apri Health Connect e abilita permessi." }
        val edt = EditText(this).apply { hint = "mmol/L (es. 6.1)" }
        val btn = Button(this).apply { text = "Inserisci su Health Connect" }

        root.addView(txtStatus)
        root.addView(edt)
        root.addView(btn)
        setContentView(root)

        hc = HealthConnectClient.getOrCreate(this)

        btn.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val granted = hc.permissionController.getGrantedPermissions()
                    if (!granted.containsAll(permissions)) {
                        txtStatus.text =
                            "Permessi mancanti: Impostazioni → Health Connect → App con accesso → Glucose Bridge → abilita Scrittura Glicemia."
                        return@launch
                    }

                    val mmol = edt.text.toString().trim().replace(",", ".").toDoubleOrNull()
                    if (mmol == null) {
                        txtStatus.text = "Valore non valido. Esempio: 6.1"
                        return@launch
                    }

                    val now = Instant.now()

                    // Costruttore coerente con i tuoi errori (time e level obbligatori)
                    val record = BloodGlucoseRecord(
                        time = now,
                        zoneOffset = ZoneOffset.UTC,
                        level = BloodGlucoseRecord.BloodGlucose(mmolPerL = mmol),
                        specimenSource = 0,   // UNKNOWN
                        mealType = 0,         // UNKNOWN
                        relationToMeal = 0,   // UNKNOWN
                        metadata = Metadata()
                    )

                    hc.insertRecords(listOf(record))
                    txtStatus.text = "Inserito: $mmol mmol/L"
                } catch (e: Throwable) {
                    txtStatus.text = "Errore: ${e::class.java.simpleName}: ${e.message}"
                }
            }
        }
    }
}

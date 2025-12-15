package it.glucose.bridge

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
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

    private val requestPermissions =
        registerForActivityResult(HealthConnectClient.createRequestPermissionResultContract()) { granted ->
            val ok = granted.containsAll(permissions)
            findViewById<TextView>(R.id.txtStatus).text =
                if (ok) "Permessi OK" else "Permessi NON concessi"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        hc = HealthConnectClient.getOrCreate(this)

        val btnPerms = findViewById<Button>(R.id.btnPermissions)
        val btnInsert = findViewById<Button>(R.id.btnInsert)
        val txtStatus = findViewById<TextView>(R.id.txtStatus)
        val edtMmol = findViewById<EditText>(R.id.edtMmol)

        btnPerms.setOnClickListener {
            requestPermissions.launch(permissions)
        }

        btnInsert.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val granted = hc.permissionController.getGrantedPermissions()
                    if (!granted.containsAll(permissions)) {
                        txtStatus.text = "Mancano permessi: premi 'Permessi' prima"
                        return@launch
                    }

                    val mmol = edtMmol.text.toString().trim().replace(",", ".").toDoubleOrNull()
                    if (mmol == null) {
                        txtStatus.text = "Valore non valido. Esempio: 6.1"
                        return@launch
                    }

                    val now = Instant.now()

                    val record = BloodGlucoseRecord(
                        levelMillimolesPerLiter = mmol,
                        specimenSource = null,
                        mealType = null,
                        relationToMeal = null,
                        time = now,
                        zoneOffset = ZoneOffset.UTC,
                        metadata = Metadata()
                    )

                    hc.insertRecords(listOf(record))
                    txtStatus.text = "Inserito: $mmol mmol/L"
                } catch (e: Throwable) {
                    txtStatus.text = "Errore: ${e.message ?: e::class.java.simpleName}"
                }
            }
        }
    }
}

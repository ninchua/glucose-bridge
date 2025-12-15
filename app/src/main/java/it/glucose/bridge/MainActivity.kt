package it.glucose.bridge

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.BloodGlucose
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset

class MainActivity : ComponentActivity() {

    private lateinit var hc: HealthConnectClient

    private val permissions = setOf(
        HealthPermission.getWritePermission(BloodGlucoseRecord::class)
    )

    private val requestPermissionsLauncher =
        registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
            val ok = granted.containsAll(permissions)
            status?.text = if (ok) "Permessi OK (ora dovresti vedermi in Health Connect)" else "Permessi NON concessi"
        }

    private var status: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        val txtStatus = TextView(this).apply { text = "1) Premi 'Richiedi permessi'." }
        status = txtStatus

        val btnPerms = Button(this).apply { text = "Richiedi permessi Health Connect" }
        val edt = EditText(this).apply { hint = "mmol/L (es. 6.1)" }
        val btnInsert = Button(this).apply { text = "Inserisci glicemia" }

        root.addView(txtStatus)
        root.addView(btnPerms)
        root.addView(edt)
        root.addView(btnInsert)
        setContentView(root)

        // Verifica disponibilità
        val sdkStatus = HealthConnectClient.getSdkStatus(this)
        if (sdkStatus != HealthConnectClient.SDK_AVAILABLE) {
            txtStatus.text = "Health Connect non disponibile (status=$sdkStatus)."
            return
        }

        hc = HealthConnectClient.getOrCreate(this)

        btnPerms.setOnClickListener {
            requestPermissionsLauncher.launch(permissions)
        }

        btnInsert.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val granted = hc.permissionController.getGrantedPermissions()
                    if (!granted.containsAll(permissions)) {
                        txtStatus.text = "Permessi mancanti: premi prima 'Richiedi permessi'."
                        return@launch
                    }

                    val mmol = edt.text.toString().trim().replace(",", ".").toDoubleOrNull()
                    if (mmol == null) {
                        txtStatus.text = "Valore non valido. Esempio: 6.1"
                        return@launch
                    }

                    val record = BloodGlucoseRecord(
                        time = Instant.now(),
                        zoneOffset = ZoneOffset.UTC,
                        level = BloodGlucose.millimolesPerLiter(mmol),
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

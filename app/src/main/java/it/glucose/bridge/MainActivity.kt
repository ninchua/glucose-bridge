package it.glucose.bridge

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.health.connect.InsertRecordsResponse
import android.health.connect.datatypes.BloodGlucoseRecord
import android.health.connect.datatypes.Metadata
import android.health.connect.datatypes.units.BloodGlucose
import android.os.OutcomeReceiver
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var hcm: HealthConnectManager

    private lateinit var status: TextView
    private lateinit var edtMmol: EditText

    private val requestWriteGlucosePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            status.text = if (granted) {
                "Permesso concesso. Ora puoi inserire un valore."
            } else {
                "Permesso NON concesso. Premi 'Apri permessi Health Connect' e abilita Scrittura → Glicemia."
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hcm = getSystemService(HealthConnectManager::class.java)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        status = TextView(this).apply {
            textSize = 15f
            text = "1) Premi 'Richiedi permesso'\n2) Se serve: 'Apri permessi Health Connect'\n3) Inserisci mmol/L e premi 'Inserisci'"
        }

        val btnReq = Button(this).apply { text = "Richiedi permesso WRITE_BLOOD_GLUCOSE" }
        val btnOpen = Button(this).apply { text = "Apri permessi Health Connect (questa app)" }
        edtMmol = EditText(this).apply { hint = "mmol/L (es. 6.1)" }
        val btnInsert = Button(this).apply { text = "Inserisci glicemia" }

        root.addView(status)
        root.addView(btnReq)
        root.addView(btnOpen)
        root.addView(edtMmol)
        root.addView(btnInsert)
        setContentView(root)

        // Se Health Connect framework è disponibile, qui non dovrebbe essere “unavailable”
        val availability = hcm.healthConnectAvailabilityStatus
        status.append("\n\nHealthConnect availabilityStatus=$availability")

        btnReq.setOnClickListener {
            requestWriteGlucosePermission.launch("android.permission.health.WRITE_BLOOD_GLUCOSE")
        }

        btnOpen.setOnClickListener {
            // Apre la UI per gestire i permessi Health Connect (con packageName filtra sulla tua app)
            val i = Intent(HealthConnectManager.ACTION_MANAGE_HEALTH_PERMISSIONS).apply {
                putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
            }
            startActivity(i)
        }

        btnInsert.setOnClickListener {
            insert()
        }
    }

    private fun insert() {
        val mmol = edtMmol.text.toString().trim().replace(",", ".").toDoubleOrNull()
        if (mmol == null) {
            status.text = "Valore non valido. Esempio: 6.1"
            return
        }

        // Costruttore/Builder framework: richiede BloodGlucose unit e campi relazione/meal/specimen (anche UNKNOWN)
        val record = BloodGlucoseRecord.Builder(
            Metadata(),
            Instant.now(),
            /* specimenSource */ BloodGlucoseRecord.SPECIMEN_SOURCE_UNKNOWN,
            /* level */ BloodGlucose.millimolesPerLiter(mmol),
            /* relationToMeal */ BloodGlucoseRecord.RELATION_TO_MEAL_UNKNOWN
        ).setMealType(BloodGlucoseRecord.MEAL_TYPE_UNKNOWN)
         .setZoneOffset(ZoneOffset.UTC)
         .build()

        status.text = "Inserimento in corso..."

        hcm.insertRecords(
            listOf(record),
            executor,
            object : OutcomeReceiver<InsertRecordsResponse, HealthConnectException> {
                override fun onResult(result: InsertRecordsResponse) {
                    runOnUiThread { status.text = "Inserito: $mmol mmol/L" }
                }

                override fun onError(error: HealthConnectException) {
                    runOnUiThread { status.text = "Errore Health Connect: ${error.message}" }
                }
            }
        )
    }
}

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
import android.os.OutcomeReceiver
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private lateinit var hcm: HealthConnectManager
    private val executor = Executors.newSingleThreadExecutor()

    private lateinit var status: TextView
    private lateinit var edtMmol: EditText

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            status.text = if (granted) {
                "Permesso concesso. Ora puoi inserire la glicemia."
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
            text = "1) Richiedi permesso\n2) Inserisci mmol/L\n3) Premi Inserisci"
        }

        val btnPerm = Button(this).apply { text = "Richiedi permesso WRITE_BLOOD_GLUCOSE" }
        val btnOpenPerms = Button(this).apply { text = "Apri permessi Health Connect" }

        edtMmol = EditText(this).apply { hint = "mmol/L (es. 6.1)" }

        val btnInsert = Button(this).apply { text = "Inserisci glicemia" }

        root.addView(status)
        root.addView(btnPerm)
        root.addView(btnOpenPerms)
        root.addView(edtMmol)
        root.addView(btnInsert)
        setContentView(root)

        btnPerm.setOnClickListener {
            requestPermission.launch("android.permission.health.WRITE_BLOOD_GLUCOSE")
        }

        btnOpenPerms.setOnClickListener {
            startActivity(
                Intent(HealthConnectManager.ACTION_MANAGE_HEALTH_PERMISSIONS).apply {
                    putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
                }
            )
        }

        btnInsert.setOnClickListener { insertGlucose() }
    }

    private fun insertGlucose() {
        val mmol = edtMmol.text.toString()
            .trim()
            .replace(",", ".")
            .toDoubleOrNull()

        if (mmol == null) {
            status.text = "Valore non valido"
            return
        }

        // Conversione mmol/L -> mg/dL (Int)
        val mgdl = (mmol * 18.0182).roundToInt()

        val now = Instant.now()
        val offset = ZoneOffset.systemDefault().rules.getOffset(now)

        val meta = Metadata.Builder().build()

        // Firma che il tuo compilatore sta imponendo:
        // Builder(metadata, time, level:Int, p3:Int, p4:Int, p5:Int)
        val record = BloodGlucoseRecord.Builder(
            meta,
            now,
            mgdl,
            0, // specimenSource UNKNOWN
            0, // relationToMeal UNKNOWN
            0  // mealType UNKNOWN
        )
            .setZoneOffset(offset)
            .build()

        status.text = "Inserimento… ($mmol mmol/L = $mgdl mg/dL)"

        hcm.insertRecords(
            listOf(record),
            executor,
            object : OutcomeReceiver<InsertRecordsResponse, HealthConnectException> {
                override fun onResult(result: InsertRecordsResponse) {
                    runOnUiThread { status.text = "Inserito: $mmol mmol/L ($mgdl mg/dL)" }
                }

                override fun onError(error: HealthConnectException) {
                    runOnUiThread { status.text = "Errore: ${error.message}" }
                }
            }
        )
    }
}

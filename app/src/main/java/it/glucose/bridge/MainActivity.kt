package it.glucose.bridge

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodGlucoseRecord

class MainActivity : ComponentActivity() {

    private val permissions = setOf(
        HealthPermission.getWritePermission(BloodGlucoseRecord::class)
    )

    private lateinit var status: TextView

    private val requestPermissionsLauncher =
        registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
            val ok = granted.containsAll(permissions)
            status.text =
                "Callback permessi:\n" +
                "- granted size=${granted.size}\n" +
                "- ok=$ok\n" +
                "- granted:\n${granted.joinToString("\n")}"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        status = TextView(this).apply { textSize = 15f }
        val btnPerms = Button(this).apply { text = "Richiedi permessi Health Connect" }

        root.addView(status)
        root.addView(btnPerms)
        setContentView(root)

        val sdkStatus = HealthConnectClient.getSdkStatus(this)
        status.text =
            "Health Connect sdkStatus=$sdkStatus\n" +
            "Premi il pulsante per richiedere i permessi."

        btnPerms.setOnClickListener {
            requestPermissionsLauncher.launch(permissions)
        }
    }
}

package it.glucose.bridge

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity

class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this).apply {
            setPadding(40, 40, 40, 40)
            textSize = 16f
            text =
                "Glucose Bridge usa Health Connect solo per scrivere le misurazioni di glicemia " +
                "che importi manualmente o dalla tua webapp."
        }
        setContentView(tv)
    }
}

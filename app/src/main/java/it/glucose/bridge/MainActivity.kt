package it.glucose.bridge

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // MVP: just confirms deep link works.
        // Next step: fetch batch.php?token=... from your server and write to Health Connect.
        val uri: Uri? = intent?.data
        val token = uri?.getQueryParameter("token")

        // TODO: implement import
        finish()
    }
}

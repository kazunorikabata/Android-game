package com.kabata.kanko

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kabata.kanko.data.VisitRepo
import com.kabata.kanko.ui.KankoApp
import com.kabata.kanko.ui.KankoTheme
import org.osmdroid.config.Configuration
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = File(filesDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid")
        }
        VisitRepo.init(applicationContext)
        setContent { KankoTheme { KankoApp() } }
    }
}

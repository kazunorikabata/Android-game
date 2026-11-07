package com.kabata.sanpo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kabata.sanpo.data.CastleRepo
import com.kabata.sanpo.ui.CastleApp
import com.kabata.sanpo.ui.CastleTheme
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
        CastleRepo.init(applicationContext)
        com.kabata.sanpo.quiz.QuizRepo.init(applicationContext)
        setContent {
            CastleTheme { CastleApp() }
        }
    }
}

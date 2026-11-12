package com.kabata.eigo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kabata.eigo.ui.EigoApp
import com.kabata.eigo.ui.EigoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Progress.init(applicationContext)
        setContent { EigoTheme { EigoApp() } }
    }
}

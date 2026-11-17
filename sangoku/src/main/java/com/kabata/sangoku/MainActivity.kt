package com.kabata.sangoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kabata.sangoku.ui.GameHolder
import com.kabata.sangoku.ui.SangokuApp
import com.kabata.sangoku.ui.SangokuTheme
import java.io.File

class MainActivity : ComponentActivity() {
    private lateinit var holder: GameHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        holder = GameHolder(File(filesDir, "save.json"))
        setContent { SangokuTheme { SangokuApp(holder) } }
    }

    override fun onStop() {
        super.onStop()
        holder.save()
    }
}

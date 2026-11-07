package com.kabata.sanpo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.kabata.sanpo.ui.SanpoApp
import com.kabata.sanpo.ui.SanpoTheme
import com.kabata.sanpo.walk.WalkTracker
import org.osmdroid.config.Configuration
import java.io.File

class MainActivity : ComponentActivity() {
    private val vm: SanpoViewModel by viewModels()
    private var permissionGranted by mutableStateOf(false)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            permissionGranted = granted
            if (granted) vm.startListening()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = File(filesDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid")
        }
        WalkTracker.init(applicationContext)
        com.kabata.sanpo.meisho.MeishoRepo.init(applicationContext)
        permissionGranted = hasPermission()
        if (!permissionGranted) requestPermission()

        setContent {
            SanpoTheme {
                SanpoApp(
                    vm = vm,
                    permissionGranted = permissionGranted,
                    onRequestPermission = ::requestPermission,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        permissionGranted = hasPermission()
        if (permissionGranted) vm.startListening()
    }

    override fun onPause() {
        super.onPause()
        vm.stopListening()
    }

    private fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }
}

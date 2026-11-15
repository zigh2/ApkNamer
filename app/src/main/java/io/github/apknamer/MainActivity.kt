package io.github.apknamer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import io.github.apknamer.data.storage.StoragePermission
import io.github.apknamer.ui.MainScreen
import io.github.apknamer.ui.MainViewModel
import io.github.apknamer.ui.theme.ApkNamerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    private val legacyPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            viewModel.refreshPermission()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ApkNamerTheme {
                MainScreen(
                    viewModel = viewModel,
                    onRequestPermission = { StoragePermission.request(this, legacyPermission) },
                )
            }
        }
    }
}

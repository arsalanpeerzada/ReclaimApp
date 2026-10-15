package io.github.arsalanpeerzada.reclaim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.arsalanpeerzada.reclaim.ui.ReclaimRoot
import io.github.arsalanpeerzada.reclaim.ui.theme.ReclaimTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as ReclaimApp).container
        setContent {
            ReclaimTheme { ReclaimRoot(container) }
        }
    }
}

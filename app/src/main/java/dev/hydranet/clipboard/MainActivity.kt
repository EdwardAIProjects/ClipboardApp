package dev.hydranet.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.hydranet.clipboard.ui.theme.ClipboardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (copySharedContent(intent)) {
            finish()
            return
        }

        setContent {
            ClipboardTheme {
                ClipboardIdleScreen()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (copySharedContent(intent)) {
            finish()
        }
    }

    private fun copySharedContent(intent: Intent): Boolean {
        val clipData = intent.toClipboardClipData() ?: return false
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(clipData)
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
        return true
    }

    private fun Intent.toClipboardClipData(): ClipData? {
        val sharedText = getCharSequenceExtra(Intent.EXTRA_TEXT)
        if (!sharedText.isNullOrBlank()) {
            return ClipData.newPlainText(getString(R.string.clipboard_label), sharedText)
        }

        if (action == Intent.ACTION_SEND_MULTIPLE) {
            val sharedUris = getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            if (!sharedUris.isNullOrEmpty()) {
                val firstUri = sharedUris.first()
                return ClipData.newUri(contentResolver, getString(R.string.clipboard_label), firstUri)
                    .also { clipboardClipData ->
                        sharedUris.drop(1).forEach { uri ->
                            clipboardClipData.addItem(contentResolver, ClipData.Item(uri))
                        }
                    }
            }
        }

        val sharedUri = getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        if (sharedUri != null) {
            return ClipData.newUri(contentResolver, getString(R.string.clipboard_label), sharedUri)
        }

        val incomingClipData = clipData
        if (incomingClipData != null && incomingClipData.itemCount > 0) {
            return ClipData(
                getString(R.string.clipboard_label),
                incomingClipData.description.filterMimeTypes("*/*") ?: fallbackMimeTypes(type),
                incomingClipData.getItemAt(0)
            ).also { clipboardClipData ->
                for (index in 1 until incomingClipData.itemCount) {
                    clipboardClipData.addItem(incomingClipData.getItemAt(index))
                }
            }
        }

        return null
    }

    private fun fallbackMimeTypes(intentType: String?): Array<String> {
        return arrayOf(intentType ?: "text/plain")
    }
}

@Composable
private fun ClipboardIdleScreen() {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.idle_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.idle_description),
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ClipboardIdleScreenPreview() {
    ClipboardTheme {
        ClipboardIdleScreen()
    }
}

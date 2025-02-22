package com.example.notes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/* TODO
after delete, hit boxes are wack
recompose NotesUI when deleting file your on
add a grayed out effect or change the text of "Save" when no changes have been made. Since last save. Or when just loaded.

close and open new keyboard when clicking new input, with other input focused
make me able to edit text behind keyboard. When text is just not long enough to be scrollable and keyboard is in the way
want a undo/redo feature (in case I accidentally delete something)

laggy list and slow to open dir on battery saving mode. Which means it's not at all optimized.
check recompositions
*/

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions()
        val fileManager = FileManager(this)

        setContent {
            // AUTOSAVE
            LaunchedEffect(fileManager.currentFile.content.value) {
                this.launch {
                    delay(3000)
                    fileManager.autoSave()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                val showDir = remember { mutableStateOf(false) }
                Notes(fileManager) {
                    showDir.value = true
                    fileManager.updateFiles()
                }
                if (showDir.value) {
                    Directory(
                        fileManager,
                        closeDir = {
                            showDir.value = false
                        }
                    )
                }
            }
        }
    }

    private fun requestPermissions() {
        if (Environment.isExternalStorageManager()) {
            return
        }

        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).also {
            it.data = Uri.parse("package:${packageName}")
        }
        startActivity(intent)
    }
}
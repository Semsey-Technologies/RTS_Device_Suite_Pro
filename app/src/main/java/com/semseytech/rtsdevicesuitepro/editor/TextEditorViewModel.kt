package com.semseytech.rtsdevicesuitepro.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class TextEditorViewModel : ViewModel() {
    var content by mutableStateOf("")
    var filePath by mutableStateOf<String?>(null)
    var isDirty by mutableStateOf(false)
    var isLoading by mutableStateOf(false)

    fun loadFile(path: String) {
        filePath = path
        isLoading = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(path)
                if (file.exists()) {
                    val text = file.readText()
                    withContext(Dispatchers.Main) {
                        content = text
                        isDirty = false
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    fun saveFile(onSuccess: () -> Unit = {}) {
        val path = filePath ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                File(path).writeText(content)
                withContext(Dispatchers.Main) {
                    isDirty = false
                    onSuccess()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun onContentChange(newContent: String) {
        content = newContent
        isDirty = true
    }
}

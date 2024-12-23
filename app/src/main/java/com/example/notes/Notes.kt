package com.example.notes

import android.content.ClipData
import android.content.ClipDescription
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Notes(
    getFiles: () -> MutableList<FileManager.CustomFile>,
    saveFile: (name: String, path: String, content: String) -> Boolean,
    saveFileOverride: (name: String, path: String, content: String) -> Unit,
    readFile: (file: File) -> String,
    saveFolder: (name: String, path: String) -> Unit,
    moveFile: (sourceFilePaths: String, targetFile: FileManager.CustomFile) -> Unit,
    deleteFiles: (list: List<FileManager.CustomFile>) -> Unit,
    rootFolderName: String,
    rootPath: String,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val textFieldFocused = remember { mutableStateOf(false) }

    val title = remember { mutableStateOf("") }
    val path = remember { mutableStateOf("") }
    val text = remember { mutableStateOf("") }

    val showDirMenu = remember { mutableStateOf(false) }
    val showSaveFileDialog = remember { mutableStateOf(false) }
    val showSaveFolderDialog = remember { mutableStateOf(false) }
    val showSaveFileOverrideDialog = remember { mutableStateOf(false) }

    val files = remember { mutableStateListOf<FileManager.CustomFile>() }
    val previousFiles = remember { mutableListOf<FileManager.CustomFile>() }

    LaunchedEffect(files.size == 0) {
        if (files.size == 0) {
            getFiles().forEach {
                if (it.file.exists())
                    files.add(it)
            }
            files.forEach { file ->
                val previousFile = previousFiles.find { prevFile -> prevFile.file.path == file.file.path }
                file.hidden = previousFile?.hidden ?: false
            }

            previousFiles.clear()
        }
    }

    LaunchedEffect(text.value) {
        this.launch {
            delay(3000)
            if (text.value != "") {
                saveFile("tmpfileforautosave", "", text.value)
            }
        }
    }

    if (showSaveFileDialog.value) {
        DialogSaveFile(
            confirm = { name: String ->
                if (name.isNotEmpty()) {
                    if (!saveFile(name, path.value, text.value)) {
                        showSaveFileOverrideDialog.value = true
                    }

                    showSaveFileDialog.value = false
                    files.forEach { previousFiles.add(it) }
                    files.clear()
                }
            },
            cancel = {
                showSaveFileDialog.value = false
            },
            presetFileName = title.value,
        )
    }

    if (showSaveFolderDialog.value) {
        DialogSaveFolder(
            confirm = { folderName: String ->
                saveFolder(folderName, path.value)
                files.forEach { previousFiles.add(it) }
                files.clear()
                showSaveFolderDialog.value = false
            },
            cancel = {
                showSaveFolderDialog.value = false
            },
        )
    }

    if (showSaveFileOverrideDialog.value) {
        DialogOverride(
            confirm = {
                saveFileOverride(title.value, path.value, text.value)
                files.forEach { previousFiles.add(it) }
                files.clear()
                showSaveFileOverrideDialog.value = false
            },
            cancel = {
                showSaveFileDialog.value = false
                showSaveFileOverrideDialog.value = false
            },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val focusManager = LocalFocusManager.current
        val focusRequester = remember { FocusRequester() }

        val customTextSelectionColors = TextSelectionColors(
            handleColor = Color.Gray,
            backgroundColor = Color.DarkGray
        )

        CompositionLocalProvider(
            LocalTextSelectionColors provides customTextSelectionColors
        ) {
            BasicTextField(
                modifier = Modifier
                    .padding(bottom = 90.dp)
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        if (it.isFocused) {
                            textFieldFocused.value = true
                            showDirMenu.value = false
                        }
                    }
                    .background(Color.Black),
                value = text.value,
                onValueChange = { it: String ->
                    text.value = it
                },
                cursorBrush = Brush.verticalGradient(
                    0.00f to Color.White,
                    0.15f to Color.White,
                    0.15f to Color.White,
                    0.75f to Color.White,
                    0.75f to Color.White,
                    1.00f to Color.White,
                ),
                enabled = !showDirMenu.value,
                textStyle = TextStyle(
                    textAlign = TextAlign.Start,
                    fontFamily = Typography.bodyLarge.fontFamily,
                    fontSize = Typography.bodyLarge.fontSize,
                    lineHeight = Typography.bodyLarge.lineHeight,
                    letterSpacing = Typography.bodyLarge.letterSpacing,
                    color = Color.White,
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
                    imeAction = ImeAction.Unspecified,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        textFieldFocused.value = false
                    }
                ),
                singleLine = false,
                maxLines = Int.MAX_VALUE,
                visualTransformation = VisualTransformation.None,
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 20.dp, top = 20.dp, end = 20.dp)
                    ) {
                        if (text.value.isEmpty()) {
                            Text(
                                modifier = Modifier,
                                text = "Write something",
                                textAlign = TextAlign.Left,
                                fontFamily = FontFamily(Font(R.font.roboto_italic)),
                                fontSize = Typography.bodyLarge.fontSize,
                                fontWeight = Typography.bodyLarge.fontWeight,
                                lineHeight = Typography.bodyLarge.lineHeight,
                                color = Color.Gray
                            )
                        } else {
                            innerTextField()
                        }
                    }
                },
                onTextLayout = {},
                interactionSource = interactionSource,
                minLines = 1,
            )

            BasicTextField(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 50.dp)
                    .fillMaxWidth()
                    .height(70.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        if (it.isFocused) {
                            textFieldFocused.value = true
                            showDirMenu.value = false
                        }
                    }
                    .background(Color.Black),
                value = title.value,
                onValueChange = { it: String ->
                    title.value = it
                },
                cursorBrush = Brush.verticalGradient(
                    0.00f to Color.White,
                    0.15f to Color.White,
                    0.15f to Color.White,
                    0.75f to Color.White,
                    0.75f to Color.White,
                    1.00f to Color.White,
                ),
                enabled = !showDirMenu.value,
                textStyle = TextStyle(
                    textAlign = TextAlign.Start,
                    fontFamily = Typography.titleMedium.fontFamily,
                    fontSize = Typography.titleMedium.fontSize,
                    lineHeight = Typography.titleMedium.lineHeight,
                    letterSpacing = Typography.titleMedium.letterSpacing,
                    color = Color.White,
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        textFieldFocused.value = false
                    }
                ),
                singleLine = false,
                maxLines = 1,
                visualTransformation = VisualTransformation.None,
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 20.dp, end = 20.dp)
                    ) {
                        if (title.value.isEmpty()) {
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterStart),
                                text = "Title",
                                textAlign = TextAlign.Left,
                                fontFamily = FontFamily(roboto["italic"]!!),
                                fontSize = Typography.titleMedium.fontSize,
                                fontWeight = Typography.titleMedium.fontWeight,
                                lineHeight = Typography.titleMedium.lineHeight,
                                color = Color.Gray
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                            ) {
                                innerTextField()
                            }
                        }
                    }
                },
                onTextLayout = {},
                interactionSource = interactionSource,
                minLines = 1,
            )
        }

        Text(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-60).dp, y = (-11).dp)
                .clickable {
                    focusManager.clearFocus()
                    textFieldFocused.value = false

                    if (text.value.isNotEmpty()) {
                        showSaveFileDialog.value = true
                    }
                },
            text = "Save",
            color = if (text.value.isEmpty()) Color.LightGray else Color.White,
            fontFamily = Typography.bodyLarge.fontFamily,
            fontSize = Typography.bodyLarge.fontSize,
            fontWeight = Typography.bodyLarge.fontWeight,
            lineHeight = Typography.bodyLarge.lineHeight,
        )

        if (!showDirMenu.value) {
            Icon(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(50.dp)
                    .clickable {
                        showDirMenu.value = !showDirMenu.value
                    },
                painter = painterResource(R.drawable.burger_menu),
                contentDescription = null,
                tint = Color.White
            )
        }

        val hiddenItems = remember { mutableListOf<String>() }

        if (showDirMenu.value) {
            val selectedItems = remember { mutableStateListOf<String>() }

            Box(
                modifier = Modifier
                    .width(200.dp)
                    .fillMaxHeight()
                    .align(Alignment.BottomEnd)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.DarkGray)
                    .clickable(interactionSource = interactionSource, indication = null) {}
                    .dragAndDropTarget(
                        shouldStartDragAndDrop = { event ->
                            event.mimeTypes().contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
                        },
                        target = object: DragAndDropTarget {
                            override fun onDrop(event: DragAndDropEvent): Boolean {
                                val draggedFilePath = event.toAndroidDragEvent().clipData?.getItemAt(0)?.text.toString()
                                moveFile(
                                    draggedFilePath, FileManager.CustomFile(
                                        file = File(
                                            rootPath,
                                            ""
                                        ),
                                        children = null,
                                        indent = 1,
                                        hidden = true,
                                    )
                                )
                                selectedItems.clear()
                                files.forEach { previousFiles.add(it) }
                                files.clear()
                                return true
                            }
                        }
                    )
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(start = 5.dp),
                    text = rootFolderName,
                    color = Color.White,
                    fontFamily = Typography.titleLarge.fontFamily,
                    fontSize = Typography.titleLarge.fontSize,
                    fontWeight = Typography.titleLarge.fontWeight,
                    lineHeight = Typography.titleLarge.lineHeight,
                )

                val autoSaveFile = files.find { it.file.nameWithoutExtension == "tmpfileforautosave" }
                if (autoSaveFile != null) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 60.dp)
                            .fillMaxWidth()
                            .height(50.dp)
                            .clickable {
                                val file = files.find { it.file.nameWithoutExtension == title.value }
                                if (text.value != "") {
                                    if (file == null || readFile(file.file) != text.value) {
                                        showSaveFileDialog.value = true
                                    }
                                }
                                if (text.value == "" || !showSaveFileDialog.value) {
                                    text.value = readFile(autoSaveFile.file)
                                    title.value = "tmpfileforautosave"
                                    path.value = autoSaveFile.file.path
                                        .replace(rootPath, "")
                                        .replace(autoSaveFile.file.name, "")
                                    showDirMenu.value = false
                                }
                            },
                    ) {
                        Icon(
                            modifier = Modifier
                                .padding(start = 5.dp, top = 10.dp, bottom = 10.dp),
                            painter = painterResource(R.drawable.file),
                            contentDescription = null,
                            tint = Color.Black,
                        )

                        Text(
                            modifier = Modifier
                                .padding(start = 5.dp, top = 10.dp, bottom = 10.dp),
                            text = "Auto save",
                            color = Color.White,
                            fontFamily = Typography.bodyLarge.fontFamily,
                            fontSize = Typography.bodyLarge.fontSize,
                            fontWeight = Typography.bodyLarge.fontWeight,
                            lineHeight = Typography.bodyLarge.lineHeight,
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(top = 120.dp, bottom = 60.dp)
                ) {
                    items(files) { file ->
                        if (!file.hidden && file.file.nameWithoutExtension != "tmpfileforautosave") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .dragAndDropSource {
                                        detectTapGestures(
                                            onTap = {
                                                if (selectedItems.size > 0) {
                                                    if (selectedItems.contains(file.file.path)) selectedItems.remove(file.file.path)
                                                    else selectedItems.add(file.file.path)
                                                } else if (file.file.isFile) {
                                                    if (text.value != "") {
                                                        val match = files.find { it.file.nameWithoutExtension == title.value }
                                                        if (match == null || readFile(match.file) != text.value) {
                                                            showSaveFileDialog.value = true
                                                        }
                                                    }
                                                    if (text.value == "" || !showSaveFileDialog.value) {
                                                        text.value = readFile(file.file)
                                                        title.value = file.file.nameWithoutExtension
                                                        showDirMenu.value = false
                                                        path.value = file.file.path
                                                            .replace(rootPath, "")
                                                            .replace(file.file.name, "")
                                                    }
                                                } else if (file.file.isDirectory) {
                                                    if (hiddenItems.find { it == file.file.path } != null) {
                                                        hiddenItems.remove(file.file.path)
                                                        file.file.listFiles()?.forEach { i ->
                                                            val f = files.find { j -> i.path == j.file.path }
                                                            if (f != null)
                                                                f.hidden = false
                                                        }
                                                    }
                                                    else {
                                                        hiddenItems.add(file.file.path)
                                                        file.children?.forEach { it.hidden = true }
                                                    }

                                                    files.forEach { previousFiles.add(it) }
                                                    files.clear()
                                                } else {
                                                    files.forEach { previousFiles.add(it) }
                                                    files.clear()
                                                }
                                            },
                                            onLongPress = {
                                                if (selectedItems.find { it == file.file.path } != null) {
                                                    var result = ""
                                                    selectedItems.forEachIndexed { index, path ->
                                                        result += if (index != selectedItems.size - 1) path + "_-middle-_" else path
                                                    }

                                                    startTransfer(
                                                        transferData = DragAndDropTransferData(
                                                            clipData = ClipData.newPlainText(file.file.name, result)
                                                        )
                                                    )
                                                } else {
                                                    selectedItems.add(file.file.path)
                                                }
                                            }
                                        )
                                    }
                                    .dragAndDropTarget(
                                        shouldStartDragAndDrop = { event ->
                                            event.mimeTypes().contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
                                        },
                                        target = object: DragAndDropTarget {
                                            override fun onDrop(event: DragAndDropEvent): Boolean {
                                                moveFile(event.toAndroidDragEvent().clipData?.getItemAt(0)?.text.toString(), file)
                                                selectedItems.clear()
                                                files.forEach { previousFiles.add(it) }
                                                files.clear()

                                                return true
                                            }
                                        }
                                    )
                                    .background(if (selectedItems.find { it == file.file.path } != null) Color.Gray else Color.Transparent)

                            ) {
                                Icon(
                                    modifier = Modifier
                                        .padding(start = (5 * file.indent).dp, top = 10.dp, bottom = 10.dp),
                                    painter = painterResource(if (file.file.isFile) R.drawable.file else if (hiddenItems.find { it == file.file.path } == null) R.drawable.folder_open else R.drawable.folder),
                                    contentDescription = null,
                                    tint = Color.White,
                                )

                                Text(
                                    modifier = Modifier
                                        .padding(start = 5.dp, top = 10.dp, bottom = 10.dp),
                                    text = if (file.file.isFile) file.file.nameWithoutExtension else file.file.name,
                                    color = Color.White,
                                    fontFamily = Typography.bodyLarge.fontFamily,
                                    fontSize = Typography.bodyLarge.fontSize,
                                    fontWeight = Typography.bodyLarge.fontWeight,
                                    lineHeight = Typography.bodyLarge.lineHeight,
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (selectedItems.size != 0) {
                        Icon(
                            modifier = Modifier
                                .size(50.dp)
                                .clickable {
                                    selectedItems.forEach {
                                        val file = files.find { file -> file.file.path == it }
                                        if (file != null) {
                                            val list = mutableListOf<FileManager.CustomFile>()

                                            selectedItems.forEach { path ->
                                                val f = files.find { f -> f.file.path == path }
                                                if (f != null) {
                                                    list.add(f)
                                                }
                                            }

                                            deleteFiles(list)
                                            files.forEach { previousFiles.add(it) }
                                            files.clear()
                                        }
                                    }
                                    selectedItems.clear()
                                },
                            painter = painterResource(R.drawable.bin),
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                    else {
                        Icon(
                            modifier = Modifier
                                .size(50.dp)
                                .clickable {
                                    text.value = ""
                                    title.value = ""
                                    path.value = ""
                                    showDirMenu.value = !showDirMenu.value
                                },
                            painter = painterResource(R.drawable.plus),
                            contentDescription = null,
                            tint = Color.White,
                        )

                        Icon(
                            modifier = Modifier
                                .size(50.dp)
                                .clickable {
                                    showDirMenu.value = !showDirMenu.value
                                    showSaveFolderDialog.value = true
                                },
                            painter = painterResource(R.drawable.folder),
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }

                    Icon(
                        modifier = Modifier
                            .size(50.dp)
                            .clickable {
                                showDirMenu.value = !showDirMenu.value
                            },
                        painter = painterResource(R.drawable.burger_menu),
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    }
}
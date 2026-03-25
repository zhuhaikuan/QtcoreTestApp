/*
 * Copyright (C) 2025 Lenovo
 * All Rights Reserved.
 * Lenovo Confidential Restricted.
 */
package com.lenovo.quantum.test

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import com.lenovo.quantum.test.client.AudioStream
import com.lenovo.quantum.test.client.FileAttachment
import com.lenovo.quantum.test.components.MediaSurface
import qtcoretestapp.composeapp.generated.resources.Res
import qtcoretestapp.composeapp.generated.resources.q_and_a_send
import qtcoretestapp.composeapp.generated.resources.q_and_a_text_box_hint

enum class InputSelector {
    NONE
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserInput(
    onAddSample: (AudioStream) -> Unit,
    onMessageSent: (String) -> Unit,
    modifier: Modifier = Modifier,
    resetScroll: () -> Unit = {},
    onSelectFile: () -> Unit,
    selectedFile: FileAttachment?,
    isRunning : Boolean,
    label: String? = null,
    displayMic: Boolean,
    sendMessageEnabled: Boolean = false
) {
    var currentInputSelector by rememberSaveable { mutableStateOf(InputSelector.NONE) }
    val dismissKeyboard = { currentInputSelector = InputSelector.NONE }

    var textState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    // Used to decide if the keyboard should be shown
    var textFieldFocusState by remember { mutableStateOf(false) }

    Surface(tonalElevation = 2.dp, contentColor = MaterialTheme.colorScheme.secondary) {
        Column(modifier = modifier) {
            UserInputText(
                onAddSample = onAddSample,
                textFieldValue = textState,
                onTextChanged = { textState = it },
                // Only show the keyboard if there's no input selector and text field has focus
                keyboardShown = currentInputSelector == InputSelector.NONE && textFieldFocusState,
                // Close extended selector if text field receives focus
                onTextFieldFocused = { focused ->
                    if (focused) {
                        currentInputSelector = InputSelector.NONE
                        resetScroll()
                    }
                    textFieldFocusState = focused
                },
                focusState = textFieldFocusState,
                sendMessageEnabled = textState.text.isNotBlank() || sendMessageEnabled,
                onMessageSent = {
                    onMessageSent(textState.text)
                    // Reset text field and close keyboard
                    textState = TextFieldValue()
                    // Move scroll to bottom
                    resetScroll()
                    dismissKeyboard()
                },
                onSelectFile = onSelectFile,
                selectedFile = selectedFile,
                isRunning = isRunning,
                label = label,
                displayMic = displayMic,
            )
        }
    }
    DisposableEffect(Unit) {
        SetTextGlobal = { newText: String ->
            textState = TextFieldValue(newText)
        }
        onDispose { SetTextGlobal = { _ -> } }
    }
}

private var SetTextGlobal: (String) -> Unit = { _ -> }


fun setText(newText: String) {
    SetTextGlobal(newText)
}

val KeyboardShownKey = SemanticsPropertyKey<Boolean>("KeyboardShownKey")
var SemanticsPropertyReceiver.keyboardShownProperty by KeyboardShownKey

@OptIn(ExperimentalAnimationApi::class)
@ExperimentalFoundationApi
@Composable
private fun UserInputText(
    onAddSample: (AudioStream) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    onTextChanged: (TextFieldValue) -> Unit,
    textFieldValue: TextFieldValue,
    keyboardShown: Boolean,
    onTextFieldFocused: (Boolean) -> Unit,
    focusState: Boolean,
    sendMessageEnabled: Boolean,
    onMessageSent: () -> Unit,
    onSelectFile: () -> Unit,
    selectedFile: FileAttachment?,
    isRunning : Boolean,
    label: String? = null,
    displayMic: Boolean,
) {
    val swipeOffset = remember { mutableStateOf(0f) }

    Surface(tonalElevation = 2.dp, contentColor = MaterialTheme.colorScheme.secondary) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (selectedFile != null) {
                MediaSurface(
                    MediaItem(selectedFile.uri, selectedFile.fileName, selectedFile.mimeType)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp)
                    .height(64.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (displayMic) {
                    Microphone(
                        onAddSample = onAddSample,
                        modifier = Modifier.align(Alignment.CenterVertically),
                        onTextChanged = { setText(it.text) },
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                else {
                    IconButton(
                        modifier = Modifier
                            .align(Alignment.CenterVertically),
                        onClick = {
                            onSelectFile()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "",
                            modifier = Modifier
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .weight(5f)
                ) {
                    UserInputTextField(
                        textFieldValue,
                        onTextChanged,
                        onTextFieldFocused,
                        keyboardType,
                        focusState,
                        onMessageSent,
                        Modifier.semantics {
                            contentDescription = ""
                            keyboardShownProperty = keyboardShown
                        },
                        label
                    )
                }

                if (!displayMic) {
                    // Send button
                    val border = if (!sendMessageEnabled) {
                        BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    } else {
                        null
                    }
                    Spacer(modifier = Modifier.weight(1f))

                    val disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

                    val buttonColors = ButtonDefaults.buttonColors(
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = disabledContentColor
                    )
                    val buttonText = if (isRunning) "Stop" else stringResource(Res.string.q_and_a_send)

                    Button(
                        modifier = Modifier
                            .align(Alignment.CenterVertically),
                        enabled = sendMessageEnabled || isRunning,
                        onClick = onMessageSent,
                        colors = buttonColors,
                        border = border,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = buttonText,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.UserInputTextField(
    textFieldValue: TextFieldValue,
    onTextChanged: (TextFieldValue) -> Unit,
    onTextFieldFocused: (Boolean) -> Unit,
    keyboardType: KeyboardType,
    focusState: Boolean,
    onMessageSent: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    var lastFocusState by remember { mutableStateOf(false) }
    BasicTextField(
        value = textFieldValue,
        onValueChange = { onTextChanged(it) },
        modifier = modifier
            .padding(start = 32.dp)
            .align(Alignment.CenterStart)
            .onFocusChanged { state ->
                if (lastFocusState != state.isFocused) {
                    onTextFieldFocused(state.isFocused)
                }
                lastFocusState = state.isFocused
            }
            .onPreviewKeyEvent { keyEvent ->
               if (keyEvent.type == KeyEventType.KeyUp
                       && keyEvent.key == Key.Enter
                       && textFieldValue.text.isNotBlank()
               ) {
                   onMessageSent()
                   true
               } else {
                   false
               }
           },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Send
        ),
        maxLines = 1,
        cursorBrush = SolidColor(LocalContentColor.current),
        textStyle = LocalTextStyle.current.copy(color = LocalContentColor.current)
    )

    val disableContentColor =
        MaterialTheme.colorScheme.onSurfaceVariant
    if (textFieldValue.text.isEmpty() && !focusState) {
        Text(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp),
            text = label?: stringResource(Res.string.q_and_a_text_box_hint),
            style = MaterialTheme.typography.bodyLarge.copy(color = disableContentColor)
        )
    }
}
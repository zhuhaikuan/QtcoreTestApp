package com.lenovo.quantum.test.shell.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    text: String,
    onTextChange: (String) -> Unit,
    onSaveClick: () -> Unit) {
    TextAreaWithSaveButton(text, onTextChange, onSaveClick)
}

@Composable
fun TextAreaWithSaveButton(
    text: String,
    onTextChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 400.dp
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.widthIn(max = maxWidth)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                label = { Text("System Instructions") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp, max = 100.dp),
                maxLines = 3,
                singleLine = false,
                textStyle = LocalTextStyle.current.copy(lineHeight = 20.sp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onSaveClick,
            modifier = Modifier.widthIn(max = maxWidth)
        ) {
            Icon(Icons.Default.Save, contentDescription = "Save")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save")
        }
    }
}
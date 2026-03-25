package com.lenovo.quantum.test.shell.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenovo.quantum.test.client.FileAttachment
import com.lenovo.quantum.test.shell.models.ChatMessage
import com.lenovo.quantum.test.shell.models.ThreadPreview
import com.lenovo.quantum.test.shell.models.ToolInfo
import com.lenovo.quantum.test.shell.models.ToolStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ThreadsPane(
    width: Dp,
    threads: List<ThreadPreview>,
    selectedThreadId: String?,
    onNewThread: () -> Unit,
    onRefresh: () -> Unit,
    onSelect: (String) -> Unit,
) {
    Surface(Modifier.width(width).fillMaxHeight()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Threads",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, contentDescription = "Refresh") }
            }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = onNewThread,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("New thread")
            }

            Spacer(Modifier.height(20.dp))
            LazyColumn(Modifier.fillMaxSize()) {
                items(threads, key = { it.id }) { item ->
                    ThreadRowItem(
                        item = item,
                        selected = item.id == selectedThreadId,
                        onClick = { onSelect(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ThreadRowItem(item: ThreadPreview, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface


    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal)
            )
        }
    }
}

@Composable
fun MessagesContainer(
    messages : List<ChatMessage>
) {
    val chips = listOf<String>(
//        "what is this",
//        "What's the weather in San D...",
//        "hello",
    )
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                chips.forEach {
                    SuggestionChip(text = it)
                }
            }

            MessagesPane(messages)
        }
    }
}

@Composable
private fun SuggestionChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ArrowOutward, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.bodySmall)
        }
    }
}

fun String.trimAndEllipsize(maxLength: Int): String {
    val trimmedString = this.trim() // Trim leading/trailing whitespace

    return if (trimmedString.length > maxLength) {
        // If the trimmed string is longer than maxLength,
        // truncate it and append "..."
        trimmedString.substring(0, maxLength - 3) + "..."
    } else {
        // Otherwise, return the trimmed string as is
        trimmedString
    }
}

@Composable
fun ChatComposer(
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
    attachedFile : FileAttachment? = null,
    onSelectFile: () -> Unit = {}
) {
    var text by remember { mutableStateOf("") }


    Column {
        Row {
            Spacer(Modifier.weight(1f))
            if (attachedFile != null) {
                Box(
                    modifier = Modifier
                        .padding(8.dp) // Outer padding for the container
                        .border(
                            border = BorderStroke(2.dp, Color.DarkGray), // Border width and color
                            shape = RoundedCornerShape(8.dp) // Optional: Rounded corners for the border
                        )
                        .padding(12.dp) // Inner padding for the text
                ) {
                    Text(attachedFile.fileName.trimAndEllipsize(15), color = Color.DarkGray, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.height(5.dp))

        Surface(tonalElevation = 2.dp) {
            Row(
                modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
//            IconButton(onClick = { /* mic pressed */ }) {
//                Icon(Icons.Default.Mic, contentDescription = "Voice")
//            }
                IconButton(onClick = onSelectFile) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Attach File")
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Message Nova…") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            val trimmed = text.trim()
                            if (trimmed.isNotEmpty()) {
                                onSend(trimmed)
                                text = ""
                            }
                        }
                    )
                )


                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = {
                        val trimmed = text.trim()
                        if (trimmed.isNotEmpty()) {
                            onSend(trimmed)
                            text = ""
                        }
                    },
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    }

}

@Composable
fun MessagesPane(messages: List<ChatMessage>, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    var viewportHeightPx by remember { mutableStateOf(0) }

    LaunchedEffect(messages) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .onSizeChanged { viewportHeightPx = it.height }
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            messages.forEach { msg ->
                MessageItem(message = msg)
            }
        }

        // Overlay scrollbar on the right edge
        OverlayScrollbar(
            scrollState = scrollState,
            viewportHeightPx = viewportHeightPx,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp) // keep it off the edge a bit
        )
    }
}

@Composable
fun OverlayScrollbar(
    scrollState: ScrollState,
    viewportHeightPx: Int,
    modifier: Modifier = Modifier,
    thickness: Dp = 6.dp,
    cornerRadius: Dp = 3.dp,
    minThumbHeightDp: Dp = 32.dp,
    trackAlpha: Float = 0.06f,
    thumbAlpha: Float = 0.35f,
) {
    if (viewportHeightPx <= 0 || scrollState.maxValue <= 0) return

    val density = LocalDensity.current
    val minThumbPx = with(density) { minThumbHeightDp.toPx() }
    val thicknessPx = with(density) { thickness.toPx() }
    val radiusPx = with(density) { cornerRadius.toPx() }

    // Compute thumb size relative to content size
    val contentHeightPx = viewportHeightPx + scrollState.maxValue
    val thumbHeightPx = maxOf(
        viewportHeightPx.toFloat() * (viewportHeightPx.toFloat() / contentHeightPx.toFloat()),
        minThumbPx
    )

    // Compute thumb offset in the track
    val maxThumbTravel = viewportHeightPx - thumbHeightPx
    val scrollFraction = scrollState.value.toFloat() / scrollState.maxValue.toFloat()
    val thumbTopPx = maxThumbTravel * scrollFraction

    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = trackAlpha)
    val thumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = thumbAlpha)

    Canvas(
        modifier = modifier
            .fillMaxHeight()
            .width(thickness)
    ) {
        // Track
        drawRoundRect(
            color = trackColor,
            cornerRadius = CornerRadius(radiusPx, radiusPx),
            size = Size(thicknessPx, size.height)
        )

        // Thumb
        drawRoundRect(
            color = thumbColor,
            cornerRadius = CornerRadius(radiusPx, radiusPx),
            topLeft = Offset(0f, thumbTopPx),
            size = Size(thicknessPx, thumbHeightPx)
        )
    }
}

fun formatMilliseconds(milliseconds: Long): String {
    val instant = Instant.ofEpochMilli(milliseconds)
    val zoneId = ZoneId.systemDefault()
    val zonedDateTime = instant.atZone(zoneId)
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withLocale(Locale.getDefault())
    return zonedDateTime.format(formatter)
}

@Composable
fun MessageItem(message: ChatMessage) {
    Column(Modifier.fillMaxWidth()) {
        if (message.toolInfo.isNotEmpty()) {
            ToolChipsRow(message.toolInfo)
            Spacer(Modifier.height(6.dp))
        }

        Row(Modifier.fillMaxWidth()) {
            if (message.isFromUser) {
                Spacer(Modifier.weight(1f))
                MessageBubble(
                    message,
                    modifier = Modifier
                        .fillMaxWidth(fraction = 2f / 3f) // cap at 2/3
                        .wrapContentWidth(Alignment.End)   // shrink to content, right aligned
                )
            } else {
                MessageBubble(
                    message,
                    modifier = Modifier
                        .fillMaxWidth(fraction = 2f / 3f) // cap at 2/3
                        .wrapContentWidth(Alignment.Start) // shrink to content, left aligned
                )
                Spacer(Modifier.weight(1f))
            }
        }

        message.timestamp.let {
            Spacer(Modifier.height(6.dp))
            val arrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
            Row(Modifier.fillMaxWidth(), horizontalArrangement = arrangement) {
                Text(
                    formatMilliseconds(it),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
fun ToolChipsRow(chips: List<ToolInfo>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        chips.forEach { chip -> ToolChip(chip) }
    }
}


@Composable
fun ToolChip(data: ToolInfo) {
    val (bg, fg) = when (data.status) {
        ToolStatus.Cloud -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) to MaterialTheme.colorScheme.primary
        ToolStatus.Local -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f) to MaterialTheme.colorScheme.tertiary
        ToolStatus.Complete -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f) to MaterialTheme.colorScheme.secondary
        ToolStatus.Running -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        ToolStatus.Error -> TODO()
    }
    Surface(color = bg, shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
// Simple dot as icon placeholder; swap with vector if you like
            Box(Modifier.size(8.dp).clip(CircleShape).background(fg))
            Spacer(Modifier.width(6.dp))
            Text(data.text, color = fg, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.isFromUser
    val bg = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(
        topStart = 16.dp, topEnd = 16.dp,
        bottomStart = if (isUser) 16.dp else 4.dp,
        bottomEnd = if (isUser) 4.dp else 16.dp
    )
    Surface(
        color = bg,
        shape = shape,
        tonalElevation = if (isUser) 0.dp else 2.dp,
        shadowElevation = if (isUser) 0.dp else 1.dp,
        modifier = modifier
    ) {
        Text(
            message.text,
            color = fg,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
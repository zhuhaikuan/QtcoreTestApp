/*
 * Copyright (C) 2025 Lenovo
 * All Rights Reserved.
 * Lenovo Confidential Restricted.
 */
package com.lenovo.quantum.test

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.lenovo.quantum.test.client.FileAttachment
import com.lenovo.quantum.test.components.MediaSurface
import org.jetbrains.compose.ui.tooling.preview.Preview

@Immutable
data class Message(
    val author: String,
    var content: String,
    val imageDescription: String = "content_description",
    val timestamp: String = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time.toString().substringBefore("."),
    val subtitleInfo : String = "",
    val compact : Boolean = false,
    val attachedFile : FileAttachment? = null,
    var mediaItem: MediaItem? = null,
    val jobId : Long? = null
)

/**
 * Entry point for a conversation screen.
 *
 * @param uiState [ConversationUiState] that contains messages to display
 * @param navigateToProfile User action when navigation to a profile is requested
 * @param modifier [Modifier] to apply to this layout node
 * @param onNavIconPressed Sends an event up when the user clicks on the menu
 */

const val ConversationTestTag = "ConversationTestTag"

@Composable
fun Messages(
    messages: List<Message>,
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var longPressedMessageIndex by remember { mutableStateOf<Int?>(null) }
    var previousMessageCount by remember { mutableIntStateOf(messages.size) }

    Box(modifier = modifier) {

        val authorMe = "Me"
        Column(
            modifier = Modifier
                .testTag(ConversationTestTag)
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            messages.forEach { message ->
                val index = messages.indexOf(message)
                val prevAuthor = messages.getOrNull(index + 1)?.author
                val nextAuthor = messages.getOrNull(index - 1)?.author
                val isFirstMessageByAuthor = prevAuthor != message.author
                val isLastMessageByAuthor = nextAuthor != message.author

                Message(
                    msg = message,
                    isUserMe = message.author == authorMe,
                    isFirstMessageByAuthor = isFirstMessageByAuthor,
                    isLastMessageByAuthor = isLastMessageByAuthor,
                    longPressed = longPressedMessageIndex == index,
                    onLongPressChanged = { isLongPressed ->
                        longPressedMessageIndex = if (isLongPressed) index else null
                    }
                )
            }
        }
        LaunchedEffect(messages.size) {
            if (messages.size > previousMessageCount) {
                scope.launch {
                    scrollState.scrollTo(scrollState.maxValue)
                }
            }
            previousMessageCount = messages.size
        }
    }
}

@Composable
fun Message(
    msg: Message,
    isUserMe: Boolean,
    isFirstMessageByAuthor: Boolean,
    isLastMessageByAuthor: Boolean,
    longPressed: Boolean,
    onLongPressChanged: (Boolean) -> Unit
) {
    val spaceBetweenAuthors = if (isLastMessageByAuthor) Modifier.padding(top = 8.dp) else Modifier
    Row(modifier = spaceBetweenAuthors) {
        AuthorAndTextMessage(
            msg = msg,
            isUserMe = isUserMe,
            isFirstMessageByAuthor = isFirstMessageByAuthor,
            isLastMessageByAuthor = isLastMessageByAuthor,
            modifier = Modifier
                //.padding(end = 16.dp, start = 8.dp)
                .weight(1f),
            longPressed = longPressed,
            onLongPressChanged = onLongPressChanged
        )
    }
}

@Composable
fun AuthorAndTextMessage(
    msg: Message,
    isUserMe: Boolean,
    isFirstMessageByAuthor: Boolean,
    isLastMessageByAuthor: Boolean,
    modifier: Modifier = Modifier,
    longPressed: Boolean,
    onLongPressChanged: (Boolean) -> Unit
) {
    val bgColor = if (msg.author == "ai") Color(0xEFEFEFFF) else Color.Transparent
    Column(modifier = modifier.background(color = bgColor)) {
        if (isLastMessageByAuthor) {
            AuthorNameTimestamp(msg)
        }
        ChatItemBubble(
            message = msg,
            isUserMe = isUserMe,
            LongPressed = longPressed,
            onLongPressChanged = onLongPressChanged
        )
        if (isFirstMessageByAuthor) {
            // Last bubble before next author
            Spacer(modifier = Modifier.height(8.dp))
        } else {
            // Between bubbles
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun AuthorNameTimestamp(msg: Message) {
    // Combine author and timestamp for a11y.
    Row(modifier = Modifier.padding(top=10.dp).semantics(mergeDescendants = true) {}) {
        /*Text(
            text = msg.author,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .alignBy(LastBaseline)
                .paddingFrom(LastBaseline, after = 8.dp) // Space to 1st bubble
        )*/
        Spacer(modifier = Modifier.width(24.dp))
        Text(
            text = msg.timestamp,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.alignBy(LastBaseline),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (msg.subtitleInfo.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = msg.subtitleInfo,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.alignBy(LastBaseline),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val ChatBubbleShape = RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)

@Composable
fun DayHeader(dayString: String) {
    Row(
        modifier = Modifier
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .height(16.dp)
    ) {
        DayHeaderLine()
        Text(
            text = dayString,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        DayHeaderLine()
    }
}

@Composable
private fun RowScope.DayHeaderLine() {
    Divider(
        modifier = Modifier
            .weight(1f)
            .align(Alignment.CenterVertically),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    )
}

@Composable
fun ChatItemBubble(
    message: Message,
    isUserMe: Boolean,
    LongPressed: Boolean,
    onLongPressChanged: (Boolean) -> Unit
) {
    val backgroundBubbleColor = if (isUserMe) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }


    Column(
        Modifier
            .padding(start = 8.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        onLongPressChanged(true)
                    },

                    onTap = {
                        onLongPressChanged(false)
                    }
                )
            }
    ) {
        Column {

            message.attachedFile?.let { file ->
                MediaSurface(
                    MediaItem(file.uri, file.fileName,file.mimeType)
                )
            }
            message.mediaItem?.let {
                MediaSurface(it)
            }

            val messagePadding =
                if (message.compact) Modifier.padding(start=16.dp, end=16.dp, top=2.dp, bottom=2.dp)
                else Modifier.padding(16.dp)
            val messageStyle =
                if (message.compact) MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF6E6E6E), fontSize = 12.sp)
                else MaterialTheme.typography.bodyLarge.copy(color = LocalContentColor.current)


            if (message.content.isNotBlank()) {
                SelectionContainer {
                    Text(
                        text = message.content,
                        style = messageStyle,
                        modifier = messagePadding,
                    )
                }
                if (!message.compact) {
                    val clipboard = LocalClipboardManager.current
                    IconButton(onClick = {
                        clipboard.setText(AnnotatedString(message.content))
                    }) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy"
                        )
                    }
                }
            }
        }
    }

    if (LongPressed) {
//        ShowLongPressBubble(
//            message.content,
//            onDismiss =
//                {
//                    onLongPressChanged(false)
//                },
//            setText = ::setText,
//        )

    }
}

/**
 * Display additional bubble while long-pressing the chat bubble
 */
@Composable
fun ShowLongPressBubble(
    text: String,
    onDismiss: () -> Unit,
    setText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(

                    onTap = {
                        onDismiss()
                    }
                )
            }
    ) {
        Surface(
            modifier = modifier
                .padding(16.dp)
                .background(Color.White)
                .border(1.dp, Color.Gray),
            shape = RoundedCornerShape(8.dp),
            color = Color.White
        ) {
            Row {
                TextButton(
                    onClick = {
                        onDismiss()
                    }
                ) {
                    Text("copy to clipboard")
                }
                TextButton(
                    onClick = {
                        CopyToInput(setText, text)
                        onDismiss()
                    }
                ) {
                    Text("copy to input")
                }
            }
        }
    }
}

/**
 * copy message content directly to input box
 */
fun CopyToInput(setText: (String) -> Unit, text: String) {
    setText(text)
}


@Preview
@Composable
fun MessagestPreview() {
    val scrollState = rememberScrollState()
    val initialMessages = listOf(
        Message("me", "msg 1"),
        Message("me", "msg 2"),
        Message("other", "msg3"),
        Message("other", "msg4"),
    )
    Column(
        Modifier
            .fillMaxSize()
    ) {
        Messages(
            messages = initialMessages,
            scrollState = scrollState
        )
    }
}

@Preview
@Composable
fun DayHeaderPrev() {
    DayHeader("Aug 6")
}

@Preview
@Composable
fun LongPressBubblePrev() {
    ShowLongPressBubble(text = "", onDismiss = {}, setText = {}, modifier = Modifier)
}

private val JumpToBottomThreshold = 56.dp

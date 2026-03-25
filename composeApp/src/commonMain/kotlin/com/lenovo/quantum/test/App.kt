/*
 * Copyright (C) 2025 Lenovo
 * All Rights Reserved.
 * Lenovo Confidential Restricted.
 */
package com.lenovo.quantum.test

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.lenovo.quantum.sdk.logging.logD
import com.lenovo.quantum.test.accountmanager.AccountManager
import com.lenovo.quantum.test.components.Dropdown
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import qtcoretestapp.composeapp.generated.resources.Res
import qtcoretestapp.composeapp.generated.resources.author_me
import qtcoretestapp.composeapp.generated.resources.ic_button_account
import qtcoretestapp.composeapp.generated.resources.login
import kotlin.streams.toList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App(chatbotViewModel: ChatbotViewModel) {
    val scrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    val authorMe = stringResource(Res.string.author_me)
    val scope = rememberCoroutineScope()

    val userData by AccountManager.state.collectAsState()

    var selectedItem by remember { mutableIntStateOf(0) }
    val navItems = listOf("首页", "文档")
    val navIcons = listOf(
        Icons.Default.Home,
        Icons.Default.DocumentScanner
    )

    chatbotViewModel.scrollToBottom = {
        scope.launch {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    fun isAddPdfDoc(): Boolean {
        var retVal = false
        if (chatbotViewModel.selectedCommand == "document" && (chatbotViewModel.selectedAction == ADD_ACTION || chatbotViewModel.selectedAction == ADD_AND_PARSE_ACTION)) {
            if (chatbotViewModel.chosenFile?.mimeType == "application/pdf")
                retVal = true
        }
        return retVal
    }

    fun getLabel(): String? {
        return when(chatbotViewModel.selectedCommand) {
            "fkb_memory" -> {
                when (chatbotViewModel.selectedAction) {
                    "add_memory" -> {
                        if (chatbotViewModel.fkbBatchJsonArray.length() == 0)
                            "Enter: \"A user text note here\"}"
                        else
                            "Batch Ingestion Mode"
                    }

                    "get_memory" -> {
                        if (chatbotViewModel.fkbBatchJsonArray.length() == 0)
                            "Enter {\"entries\": [1,3,4]} or {\"query\": \"What is my name?\"}"
                        else
                            "Batch Query Mode"
                    }

                    "delete_memory" -> "Enter {\"entries\": [1,3,4]}"
                    else -> null
                }
            }
            "document" -> {
                when(chatbotViewModel.selectedAction) {
                    ADD_ACTION, ADD_AND_PARSE_ACTION -> "Pick a pdf document"
                    LIST_ACTION -> if (!getPlatform().name.contains("Android")) {
                        "Enter folder to be listed"
                    } else {
                        null
                    }
                    DELETE_ACTION -> "Enter {\"doc_ids\": [1,2,3]}"
                    else -> null
                }
            }
            "pkbtool" -> {
                when(chatbotViewModel.selectedAction) {
                    KBQA_ACTION, SUMMARY_ACTION -> "Enter {\"query\": \"User query text\", \"doc_ids\": \"[1,2,3]\"}"
                    DOCUMENT_SEARCH_ACTION -> "Enter {\"query\": \"User query text\", \"fileTypes\": \"[\"pdf\"]\"}"
                    IMAGE_SEARCH_ACTION -> "Enter {\"query\": \"User query text\", \"imageDescription\": \"Image description text\"}"
                    else -> null
                }
            }
            else -> null
        }
    }

    @Composable
    fun MainScreen(innerPadding: PaddingValues) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 7.dp)
                        .horizontalScroll(horizontalScrollState)
                ) {
                    Dropdown(
                        title = "Command",
                        options = chatbotViewModel.commands,
                        onOptionSelected = { command ->
                            chatbotViewModel.setCommand(command)
                            if (command == "capabilities" || command == "cdn") {
                                chatbotViewModel.sendQuery(command)
                            }
                        },
                        selectedOption = chatbotViewModel.selectedCommand
                    )
                    Dropdown(
                        title = "Action",
                        options = chatbotViewModel.actions,
                        onOptionSelected = { action ->
                            chatbotViewModel.setAction(action)
                            val command = chatbotViewModel.selectedCommand
                            when (command) {
                                "fkb_memory" -> {
                                    if (action == "get_all_memory" || action == "delete_all_memory") {
                                        chatbotViewModel.sendQuery(command)
                                    }
                                }

                                "document" -> {
                                    when (action) {
                                        ADD_ACTION, ADD_AND_PARSE_ACTION, PARSE_ACTION, DELETE_ACTION -> {
                                            if (chatbotViewModel.chosenFile == null) {
                                                showTip()
                                            } else {
                                                chatbotViewModel.sendQuery("")
                                            }
                                        }

                                        LIST_ACTION -> chatbotViewModel.sendQuery("")
                                        COLLECT_ACTION -> chatbotViewModel.sendQuery("")
                                        STATISTIC_ACTION -> chatbotViewModel.sendQuery("")
                                        RE_PARSE_ACTION -> chatbotViewModel.sendQuery("")
                                        COLLECT_ALL_ACTION -> chatbotViewModel.sendQuery("")
                                        SYNC_ACTION -> chatbotViewModel.sendQuery("")
                                    }
                                }
                            }
                        },
                        selectedOption = chatbotViewModel.selectedAction
                    )
                    Dropdown(
                        title = "Handler",
                        options = chatbotViewModel.handlers,
                        onOptionSelected = { handler ->
                            chatbotViewModel.setHandler(handler)
                        },
                        selectedOption = chatbotViewModel.selectedHandler
                    )
                    Dropdown(
                        title = "Model",
                        options = chatbotViewModel.models,
                        onOptionSelected = { modelName ->
                            chatbotViewModel.setModel(modelName)
                        },
                        selectedOption = chatbotViewModel.selectedModel
                    )
                    Dropdown(
                        title = "Model Version",
                        options = chatbotViewModel.modelVersions,
                        onOptionSelected = { modelVersion ->
                            chatbotViewModel.setModelVersion(modelVersion)
                        },
                        selectedOption = chatbotViewModel.selectedModelVersion
                    )
                    Dropdown(
                        title = "Language",
                        options = chatbotViewModel.mSLanguages.map { it.locale },
                        onOptionSelected = { language ->
                            //For MS TTS we are displaying the locale
                            //but will use voiceLocale to call the model
                            if (chatbotViewModel.selectedModel == "mstts") {
                                val voiceLocale = chatbotViewModel.mSLanguages.find {
                                    it.locale == language
                                }?.voiceLocale
                                if (voiceLocale != null) {
                                    logD { "voiceLocale $voiceLocale" }
                                    chatbotViewModel.setMSLanguage(voiceLocale)
                                }
                            } else {
                                chatbotViewModel.setMSLanguage(language)
                            }
                        },
                        selectedOption = chatbotViewModel.selectedMSLanguage
                    )
                    Dropdown(
                        title = "Voice",
                        options = chatbotViewModel.mSTTSVoices.map { it.name },
                        onOptionSelected = { voice ->
                            chatbotViewModel.setMSTTSVoice(voice)
                        },
                        selectedOption = chatbotViewModel.selectedMSTTSVoice
                    )
                    Dropdown(
                        title = "CDN Capability",
                        options = chatbotViewModel.cdnCapabilities.map { it.component },
                        onOptionSelected = { capability ->
                            chatbotViewModel.setCDNCapability(capability)
                        },
                        selectedOption = chatbotViewModel.selectedCDNCapabilities
                    )
                    Dropdown(
                        title = "CDN Command",
                        options = chatbotViewModel.cdnCommands,
                        onOptionSelected = { command ->
                            chatbotViewModel.setCDNCommand(command)
                        },
                        selectedOption = chatbotViewModel.selectedCDNCommand
                    )
                    Dropdown(
                        title = "CDN Name",
                        options = chatbotViewModel.cdnNames,
                        onOptionSelected = { voice ->
                            chatbotViewModel.setCDNName(voice)
                        },
                        selectedOption = chatbotViewModel.selectedCDNName
                    )
                }
                Messages(
                    messages = chatbotViewModel.messages.value,
                    modifier = Modifier.weight(1f),
                    scrollState = scrollState
                )

                //add for test
                Button(
                    onClick = {
                        chatbotViewModel.selectedCommand = "retrieval"
                        val query = "Did Kendra miss the bus?"
                        chatbotViewModel.retrieval(query)
                    }
                ) {
                    Text(" start retrieval ")
                }

                if (chatbotViewModel.fkbBatchJsonArray.length() > 0) {
                    Button(
                        onClick = {
                            val json = chatbotViewModel.fkbBatchJsonArray.getJSONObject(
                                chatbotViewModel.fkbBatchIndex
                            )
                            chatbotViewModel.sendMemoryBatchCommand(
                                entry = json
                            )
                        },
                        enabled = (chatbotViewModel.fkbBatchIndex == 0)
                    ) {
                        Text("Submit batch of ${chatbotViewModel.fkbBatchTag}")
                    }
                }
                UserInput(
                    onAddSample = { sampleData ->
                        chatbotViewModel.addSamples(sampleData)
                    },
                    onMessageSent = { inputMessage ->
                        val isRunning = chatbotViewModel.currentJobID != null
                        if (!isRunning) {
                            chatbotViewModel.addMessage(
                                Message(
                                    author = authorMe,
                                    content = inputMessage,
                                    attachedFile = chatbotViewModel.chosenFile
                                )
                            )
                            chatbotViewModel.sendQuery(inputMessage)
                        } else {
                            // abort
                            chatbotViewModel.abort()
                        }
                    },
                    resetScroll = {
                        scope.launch {
                            scrollState.scrollTo(0)
                        }
                    },
                    // let this element handle the padding so that the elevation is shown behind the
                    // navigation bar
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding(),
                    onSelectFile = {
                        chatbotViewModel.pickFile()
                    },
                    selectedFile = chatbotViewModel.chosenFile,
                    isRunning = chatbotViewModel.currentJobID != null,
                    label = getLabel(),
                    displayMic = chatbotViewModel.selectedModel == "msstt" &&
                            chatbotViewModel.selectedMSLanguage.isNotEmpty(),
                    sendMessageEnabled = true //isAddPdfDoc()
                )
            }
        }
    }

    @Composable
    fun DocumentScreen(innerPadding: PaddingValues) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Messages(
                    messages = chatbotViewModel.messages.value,
                    modifier = Modifier.weight(1f),
                    scrollState = scrollState
                )
                if (chatbotViewModel.fkbBatchJsonArray.length() > 0) {
                    Button(
                        onClick = {
                            val json = chatbotViewModel.fkbBatchJsonArray.getJSONObject(
                                chatbotViewModel.fkbBatchIndex
                            )
                            chatbotViewModel.sendMemoryBatchCommand(
                                entry = json
                            )
                        },
                        enabled = (chatbotViewModel.fkbBatchIndex == 0)
                    ) {
                        Text("Submit batch of ${chatbotViewModel.fkbBatchTag}")
                    }
                }
                UserInput(
                    onAddSample = { sampleData ->
                        chatbotViewModel.addSamples(sampleData)
                    },
                    onMessageSent = { inputMessage ->
                        val isRunning = chatbotViewModel.currentJobID != null
                        if (!isRunning) {
                            chatbotViewModel.addMessage(
                                Message(
                                    author = authorMe,
                                    content = inputMessage,
                                    attachedFile = chatbotViewModel.chosenFile
                                )
                            )
                            chatbotViewModel.selectedAction = ADD_ACTION
                            chatbotViewModel.sendQuery(inputMessage)
                        } else {
                            // abort
                            chatbotViewModel.abort()
                        }
                    },
                    resetScroll = {
                        scope.launch {
                            scrollState.scrollTo(0)
                        }
                    },
                    // let this element handle the padding so that the elevation is shown behind the
                    // navigation bar
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding(),
                    onSelectFile = {
                        chatbotViewModel.pickFile2()
                    },
                    selectedFile = chatbotViewModel.chosenFile,
                    isRunning = chatbotViewModel.currentJobID != null,
                    label = getLabel(),
                    displayMic = chatbotViewModel.selectedModel == "msstt" &&
                            chatbotViewModel.selectedMSLanguage.isNotEmpty(),
                    sendMessageEnabled = true //isAddPdfDoc()
                )
            }
        }
    }

    @Composable
    fun ListItem(text: String) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "More"
                )
            }
        }
    }

    @Composable
    fun DocumentListScreen(innerPadding: PaddingValues) {
        val items = listOf("Item 1", "Item 2", "Item 3", "Item 4", "Item 5")

        LazyColumn(modifier =
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(items.size) { item ->
                ListItem(text = items[item])
            }
        }
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Test App")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                chatbotViewModel.newSession()
                                chatbotViewModel.clearMessages()
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear Session"
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        )
                        {
                            IconButton(
                                content = {
                                    if (!userData.avatarUrl.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = userData.avatarUrl,
                                            contentDescription = "User Account"
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(resource = Res.drawable.ic_button_account),
                                            contentDescription = "User Account"
                                        )
                                    }
                                },
                                onClick = {
                                    AccountManager.onAccountClick()
                                }
                            )
                            Text(
                                text = userData.name ?: stringResource(Res.string.login),
                                style = TextStyle(
                                    fontSize = 10.sp
                                ),
                            )
                        }
                    },
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White
                ) {
                    navItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            icon = { Icon(navIcons[index], contentDescription = item) },
                            label = { Text(item) },
                            selected = selectedItem == index,
                            onClick = {
                                selectedItem = index
                                chatbotViewModel.setCommand("query")
                                chatbotViewModel.clearMessages()
                            }
                        )
                    }
                }
            },
            contentWindowInsets = ScaffoldDefaults
                .contentWindowInsets
                .exclude(WindowInsets.navigationBars)
                .exclude(WindowInsets.ime),
        ) { innerPadding ->
            when(selectedItem) {
                0 -> {
                    chatbotViewModel.isDocumentScreen = false
                    MainScreen(innerPadding)
                }
                1 -> {
                    chatbotViewModel.isDocumentScreen = true
                    chatbotViewModel.selectedCommand = "document"
                    DocumentScreen(innerPadding)
                }
                2 -> {
                    chatbotViewModel.isDocumentScreen = false
                    DocumentListScreen(innerPadding)
                    chatbotViewModel.getAllDocuments()
                }
            }
        }
    }
}

expect fun showTip()

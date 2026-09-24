package com.johnfatoma.sana.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.johnfatoma.sana.live.LiveVoiceState
import com.johnfatoma.sana.ui.ChatMessage
import com.johnfatoma.sana.ui.ChatViewModel

@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    var input by remember { mutableStateOf("") }
    val voiceState = viewModel.voiceState.value
    val isVoiceActive = voiceState != LiveVoiceState.IDLE

    Scaffold(
        bottomBar = {
            Column {
                viewModel.errorMessage.value?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            if (isVoiceActive) {
                                viewModel.stopVoiceSession()
                            } else {
                                viewModel.startVoiceSession()
                            }
                        },
                    ) {
                        Icon(
                            imageVector = if (isVoiceActive) Icons.Filled.MicOff else Icons.Filled.Mic,
                            contentDescription = if (isVoiceActive) "Stop voice mode" else "Start voice mode",
                        )
                    }

                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        placeholder = { Text("Message Sana...") },
                        enabled = !isVoiceActive,
                    )

                    IconButton(
                        onClick = {
                            viewModel.sendMessage(input)
                            input = ""
                        },
                        enabled = input.isNotBlank() && !viewModel.isSending.value && !isVoiceActive,
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        },
    ) { paddingValues ->
        if (isVoiceActive) {
            // Voice mode takes over the screen with the orb, matching
            // the original IDLE/LISTENING/THINKING/SPEAKING flow.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    VoiceOrb(state = voiceState)
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(16.dp))
                    Text(
                        text = voiceState.name,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(viewModel.messages) { message ->
                    MessageBubble(message)
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = if (isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

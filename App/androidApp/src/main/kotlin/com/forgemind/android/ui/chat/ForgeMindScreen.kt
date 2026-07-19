package com.forgemind.android.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

@Composable
fun ForgeMindScreen() {

    var question by remember {
        mutableStateOf("")
    }

    val messages = remember {

        mutableStateListOf(

            ChatMessage(
                "Hello! I'm ForgeMind. Ask me anything about this maintenance procedure.",
                false
            )

        )

    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        Text(
            text = "ForgeMind Assistant",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(20.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)
        ) {

            items(messages) { message ->

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        if (message.isUser)
                            Arrangement.End
                        else
                            Arrangement.Start
                ) {

                    Card(
                        modifier = Modifier.widthIn(max = 300.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                if (message.isUser)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {

                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {

                            Text(
                                text =
                                    if (message.isUser) "You"
                                    else "ForgeMind",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(message.text)

                        }

                    }

                }

                Spacer(Modifier.height(8.dp))

            }

        }

        HorizontalDivider()

        Row(
            modifier = Modifier.padding(16.dp)
        ) {

            OutlinedTextField(
                value = question,
                onValueChange = {
                    question = it
                },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("Ask a question...")
                }
            )

            Spacer(Modifier.width(12.dp))
            IconButton(
                onClick = {

                    if (question.isBlank()) return@IconButton

                    val userQuestion = question

                    messages.add(
                        ChatMessage(
                            text = userQuestion,
                            isUser = true
                        )
                    )

                    question = ""

                    // Dummy response (replace with backend later)

                    val reply =
                        when {

                            userQuestion.contains("tool", true) ->
                                "A compatible tool of the same specification may be used, provided it fits securely and does not damage the fastener."

                            userQuestion.contains("shutdown", true) ->
                                "Yes. Disconnect the machine from power before removing the fan assembly."

                            userQuestion.contains("replace", true) ->
                                "After replacement, verify fan balance and ensure there is no abnormal vibration."

                            else ->
                                "Based on the current diagnosis, inspect the affected component carefully before proceeding. If the issue persists, consult the maintenance supervisor."

                        }

                    messages.add(
                        ChatMessage(
                            text = reply,
                            isUser = false
                        )
                    )

                }
            ) {

                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null
                )

            }

        }

    }
}


package com.forgemind.android.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.forgemind.android.ui.components.*
import com.forgemind.android.ui.dummy.HomeDummyData

@Composable
fun HomeScreen(
    onAlertClick: () -> Unit = {}
) {

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {

        item {

            ForgeTopBar(workerName = "Aarush")
            NotificationAlertCard(
                onClick = onAlertClick
            )

            Spacer(modifier = Modifier.height(20.dp))

            Spacer(modifier = Modifier.height(12.dp))

            MachineSearchBar()

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Safety Updates",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            AutoScrollingCarousel()

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Quick Tools",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

        }

        item {

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                userScrollEnabled = false,
                contentPadding = PaddingValues(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                items(HomeDummyData.quickTools.size) { index ->

                    val tool = HomeDummyData.quickTools[index]

                    QuickToolCard(
                        title = tool.title,
                        subtitle = "Open",
                        icon = when (index) {
                            0 -> Icons.Default.Build
                            1 -> Icons.Default.Build
                            2 -> Icons.Default.Home
                            else -> Icons.Default.Warning
                        }
                    )
                }
            }
        }

        item {

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Learning Hub",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(HomeDummyData.learningTopics) { topic ->

                    Box(
                        modifier = Modifier.width(280.dp)
                    ) {
                        LearningCard(
                            image = topic.image,
                            title = topic.title,
                            description = topic.description
                        )
                    }
                }
            }
        }

        item {

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Recent Maintenance",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

        }

        items(HomeDummyData.recentRepairs) { repair ->

            Box(
                modifier = Modifier.padding(
                    horizontal = 20.dp,
                    vertical = 8.dp
                )
            ) {

                RecentUpdateCard(
                    machineId = repair.machineId,
                    issue = repair.issue,
                    time = repair.time
                )

            }
        }
    }
}
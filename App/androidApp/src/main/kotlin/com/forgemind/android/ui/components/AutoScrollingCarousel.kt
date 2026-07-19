package com.forgemind.android.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AutoScrollingCarousel() {

    val banners = listOf(
        Pair(
            "Wear PPE Before Maintenance",
            "Disconnect power before servicing machines."
        ),
        Pair(
            "Safety Tip",
            "Inspect cooling fans every 30 days."
        ),
        Pair(
            "Reminder",
            "Always verify lockout/tagout procedures."
        )
    )

    val pagerState = rememberPagerState { banners.size }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3500)
            pagerState.animateScrollToPage(
                (pagerState.currentPage + 1) % banners.size
            )
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    ) { page ->

        SafetyBannerCard(
            title = banners[page].first,
            description = banners[page].second
        )
    }
}
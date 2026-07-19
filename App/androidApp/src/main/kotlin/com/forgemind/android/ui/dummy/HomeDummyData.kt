package com.forgemind.android.ui.dummy
import androidx.annotation.DrawableRes
import com.forgemind.android.R

data class QuickTool(
    val title: String
)

data class LearningTopic(
    @DrawableRes val image: Int,
    val title: String,
    val description: String
)

data class RecentRepair(
    val machineId: String,
    val issue: String,
    val time: String
)

object HomeDummyData {

    val quickTools = listOf(
        QuickTool("Manuals"),
        QuickTool("Toolbox"),
        QuickTool("Checklist"),
        QuickTool("Emergency")
    )

    val learningTopics = listOf(

        LearningTopic(
            R.drawable.fan,
            "Bent Blade",
            "Identify fan blade deformation."
        ),

        LearningTopic(
            R.drawable.bearing,
            "Bearing Failure",
            "Recognize early bearing wear."
        ),

        LearningTopic(
            R.drawable.motor,
            "Motor Overheating",
            "Causes and prevention."
        )
    )

    val recentRepairs = listOf(
        RecentRepair(
            "FAN-01",
            "Bent Blade",
            "Yesterday"
        ),
        RecentRepair(
            "PUMP-03",
            "Bearing Wear",
            "2 days ago"
        )
    )
}
package com.forgemind.android.model

data class Diagnosis(

    val fault: String,

    val confidence: Int,

    val severity: String,

    val summary: String,

    val recommendation: String

)
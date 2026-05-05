package com.example.cookingeasy.domain.model
import com.google.firebase.Timestamp

data class HistorySearch(
    var id: String = "",
    var userId: String = "",
    var keyword: String = "",
    var timestamp: String = ""
)
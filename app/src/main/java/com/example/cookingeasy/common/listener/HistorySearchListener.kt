package com.example.cookingeasy.common.listener

import com.example.cookingeasy.domain.model.HistorySearch

interface HistorySearchListener {
    fun onClick(historySearch: HistorySearch)
}
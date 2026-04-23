package com.example.cookingeasy.data.remote.firebase.fireStore

import com.example.cookingeasy.domain.model.HistorySearch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class HistorySearchFirestoreSource {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val historySearchCollection = db.collection("historySearch")

    suspend fun createHistorySearch(historySearch: HistorySearch) {
        val uid = getUid()
        val docRef = historySearchCollection.document(uid).collection("listHistory").document()

        val historyWithId = historySearch.copy(id = docRef.id)

        docRef.set(historyWithId).await()
    }

    suspend fun getHistorySearch(): List<HistorySearch> {
        val uid = getUid()
        return historySearchCollection.document(uid).collection("listHistory").get().await()
        .mapNotNull {
            it.toObject(HistorySearch::class.java)
        }
    }

    suspend fun deleteHistorySearch(uuid: String) {
        val uid = getUid()
        historySearchCollection.document(uid).collection("listHistory").document(uid).delete().await()
    }

    suspend fun clearHistorySearch() {
        val collectionRef = historySearchCollection.document(getUid()).collection("listHistory")
        while (true) {
            val snapshot = collectionRef.limit(500).get().await()
            if (snapshot.isEmpty) break

            val batch = db.batch()
            snapshot.documents.forEach {
                batch.delete(it.reference)
            }
            batch.commit().await()
        }
    }

    fun getUid(): String {
        return auth.currentUser?.uid ?: ""
    }
}
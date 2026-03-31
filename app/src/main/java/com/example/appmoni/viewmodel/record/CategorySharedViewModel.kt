package com.example.appmoni.viewmodel.record

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CategorySharedViewModel : ViewModel() {
    private val _searchQuery = MutableLiveData<String>("")

    val searchQuery: LiveData<String> get() = _searchQuery

    fun updateQuery(query: String) {
        _searchQuery.value = query
    }
}
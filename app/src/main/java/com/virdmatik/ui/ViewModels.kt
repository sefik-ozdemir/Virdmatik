package com.virdmatik.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.virdmatik.data.local.DayProgressRow
import com.virdmatik.data.local.ZikirEntity
import com.virdmatik.data.local.ZikirProgressRow
import com.virdmatik.data.repository.VirdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DayUiState(
    val date: String = LocalDate.now().toString(),
    val items: List<ZikirProgressRow> = emptyList(),
    val loading: Boolean = true
) {
    val progress: Float get() {
        val target = items.sumOf { it.target }
        if (target <= 0) return 0f
        return (items.sumOf { minOf(it.count, it.target) }.toFloat() / target).coerceIn(0f, 1f)
    }
}

@HiltViewModel
class HomeViewModel @Inject constructor(private val repo: VirdRepository) : ViewModel() {
    private val _date = MutableStateFlow(LocalDate.now().toString())
    val state: StateFlow<DayUiState> = _date.flatMapLatest { date ->
        flow {
            repo.initialize()
            emitAll(repo.observeDay(date).map { DayUiState(date, it, false) })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DayUiState())

    fun refresh() {
        viewModelScope.launch {
            repo.initialize()
            _date.value = LocalDate.now().toString()
        }
    }

    fun complete(id: Long) {
        viewModelScope.launch { repo.complete(_date.value, id) }
    }
}

data class CounterUiState(
    val loading: Boolean = true,
    val name: String = "",
    val date: String = "",
    val count: Int = 0,
    val target: Int = 0,
    val completed: Boolean = false
) { val progress: Float get() = if (target <= 0) 0f else (count.toFloat() / target).coerceIn(0f, 1f) }

@HiltViewModel
class CounterViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: VirdRepository
) : ViewModel() {
    private val zikirId: Long = checkNotNull(savedStateHandle["zikirId"])
    private val date: String = checkNotNull(savedStateHandle["date"])
    private val _state = MutableStateFlow(CounterUiState(date = date))
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.ensureDay(date)
            val zikir = repo.getZikir(zikirId) ?: return@launch
            repo.observeRecord(date, zikirId).collect { r ->
                if (r != null) _state.value = CounterUiState(false, zikir.name, date, r.count, r.targetSnapshot, r.isCompleted)
            }
        }
    }

    fun increment() {
        if (_state.value.completed) return
        viewModelScope.launch { repo.increment(date, zikirId) }
    }

    fun undo() {
        if (_state.value.count <= 0) return
        viewModelScope.launch { repo.decrement(date, zikirId) }
    }
}

@HiltViewModel
class HistoryViewModel @Inject constructor(private val repo: VirdRepository) : ViewModel() {
    private val today = LocalDate.now()
    val history: StateFlow<List<DayProgressRow>> = flow {
        repo.initialize()
        emitAll(repo.observeHistory(today.minusDays(29).toString(), today.toString()))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@HiltViewModel
class ManageViewModel @Inject constructor(private val repo: VirdRepository) : ViewModel() {
    val zikirs: StateFlow<List<ZikirEntity>> = flow {
        repo.initialize()
        emitAll(repo.observeActive())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(name: String, target: Int) = viewModelScope.launch { repo.addZikir(name, target) }
    fun update(id: Long, name: String, target: Int) = viewModelScope.launch { repo.updateZikir(id, name, target) }
    fun archive(id: Long) = viewModelScope.launch { repo.archiveZikir(id) }
}

@HiltViewModel
class HistoricalDayViewModel @Inject constructor(private val repo: VirdRepository) : ViewModel() {
    private val _state = MutableStateFlow(DayUiState())
    val state = _state.asStateFlow()
    private var loadedDate: String? = null

    fun load(date: String) {
        if (loadedDate == date) return
        loadedDate = date
        viewModelScope.launch {
            repo.ensureDay(date)
            repo.observeDay(date).collect { _state.value = DayUiState(date, it, false) }
        }
    }

    fun complete(id: Long) {
        val date = loadedDate ?: return
        viewModelScope.launch { repo.complete(date, id) }
    }
}

package org.ktc2.cokaen.wouldyouin.feat_booking.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.ktc2.cokaen.wouldyouin.core.ToastUtils
import org.ktc2.cokaen.wouldyouin.data.model.ReservationResponse
import org.ktc2.cokaen.wouldyouin.network.repository.ReservationAPIRetrofitRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class BookingFragmentViewModel @Inject constructor(
    application: Application,
    private val reservationRepository: ReservationAPIRetrofitRepository
) : ViewModel() {
    private val context = application.applicationContext

    private var lastId: Long = Long.MAX_VALUE
    private var isLastPage: Boolean = false
    private var currentPage: Int = 0

    private val _bookings = MutableStateFlow<List<ReservationResponse>>(emptyList())
    val bookings: StateFlow<List<ReservationResponse>> = _bookings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 변환된 예약 목록을 노출하는 Flow
    // StateFlow를 observe하는 방식으로 변경
    private val _processedBookings = MutableStateFlow<List<ReservationResponse>>(emptyList())
    val processedBookings: StateFlow<List<ReservationResponse>> = _processedBookings.asStateFlow()

    init {
        loadBookings()

        viewModelScope.launch {
            bookings.collect { reservationList ->
                val processed = reservationList
                    .filter { !isEventEnded(it.event.startTime) }
                    .sortedBy { parseEventDate(it.event.startTime) }
                    .distinctBy { it.id }
                _processedBookings.value = processed
            }
        }
    }

    fun loadBookings(page: Int = currentPage, size: Int = 10) {
        if (_isLoading.value || isLastPage) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = reservationRepository.getReservationList(page, size, lastId)
                if (response.reservations.isNotEmpty()) {
                    _bookings.update { currentList ->
                        (currentList + response.reservations)
                    }
                    lastId = response.reservations.last().id
                    currentPage++
                } else {
                    isLastPage = true
                }
            } catch (e: Exception) {
                Log.e("BookingViewModel", "Failed to load bookings", e)
                ToastUtils.showShortToast(context, "예매 목록 조회에 실패했습니다. 다시 시도해 주세요.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun isEventEnded(eventDate: List<String>): Boolean {
        return try {
            val parsedDate = parseEventDate(eventDate)
            parsedDate.isBefore(LocalDateTime.now())
        } catch (e: Exception) {
            Log.e("BookingViewModel", "Error parsing date: $eventDate", e)
            false
        }
    }

    private fun parseEventDate(dateList: List<String>): LocalDateTime {
        return try {
            require(dateList.size >= 5) { "Date list must contain at least 5 elements (year, month, day, hour, minute)" }

            val year = dateList[0].toInt()
            val month = dateList[1].toInt()
            val day = dateList[2].toInt()
            val hour = dateList[3].toInt()
            val minute = dateList[4].toInt()

            LocalDateTime.of(year, month, day, hour, minute)
        } catch (e: Exception) {
            Log.e("BookingViewModel", "Error parsing date from list: $dateList", e)
            LocalDateTime.now() // 파싱 실패시 현재 시간 반환
        }
    }

    fun refresh() {
        currentPage = 0
        isLastPage = false
        lastId = Long.MAX_VALUE
        viewModelScope.launch {
            _bookings.emit(emptyList())
            loadBookings()
        }
    }
}

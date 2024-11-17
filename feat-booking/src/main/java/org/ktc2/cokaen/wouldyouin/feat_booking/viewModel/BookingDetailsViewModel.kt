package org.ktc2.cokaen.wouldyouin.feat_booking.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.ktc2.cokaen.wouldyouin.core.ToastUtils
import org.ktc2.cokaen.wouldyouin.data.model.ReservationResponse
import org.ktc2.cokaen.wouldyouin.network.repository.ReservationAPIRetrofitRepository
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class BookingDetailsViewModel @Inject constructor(
    val reservationRepository: ReservationAPIRetrofitRepository,
    application: Application
): ViewModel() {
    private val context = application.applicationContext

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _reservation = MutableLiveData<ReservationResponse?>()
    val reservation: LiveData<ReservationResponse?> = _reservation

    fun loadReservationDetail(reservationId: Long) {
        Log.d("DetailBooking", "LoadReservationDetail started with id: $reservationId")
        if (isLoading.value == true) {
            Log.d("DetailBooking", "Already loading, returning")
            return
        }

        _isLoading.value = true
        Log.d("DetailBooking", "Starting coroutine")
        viewModelScope.launch {
            try {
                Log.d("DetailBooking", "About to call repository")
                val reservationDetail = reservationRepository.getReservationDetails(reservationId)
                Log.d("DetailBooking", "Repository call successful: $reservationDetail")
                _reservation.value = reservationDetail
            } catch (e: Exception) {
                Log.e("DetailBooking", "Error in loadReservationDetail", e)
                e.printStackTrace() // 스택 트레이스 출력
                ToastUtils.showShortToast(context, e.message ?: "예매 상세 정보 조회에 실패했습니다. 다시 시도해주세요.")
            } finally {
                _isLoading.value = false
                Log.d("DetailBooking", "Loading completed")
            }
        }
    }

    // ViewModel에서:
    private val _deletionSuccess = MutableLiveData<Boolean>()
    val deletionSuccess: LiveData<Boolean> = _deletionSuccess

    fun deleteReservation(reservationId: Long, eventStartTime: List<String>) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val startDateTime = parseEventDate(eventStartTime)
                val currentDateTime = LocalDateTime.now()
                val hoursDifference = ChronoUnit.HOURS.between(currentDateTime, startDateTime)

                if (hoursDifference >= 3) {
                    val success = reservationRepository.deleteReservation(reservationId)
                    _deletionSuccess.value = success  // 성공/실패 상태 업데이트
                    if (success) {
                        ToastUtils.showShortToast(context, "예매가 취소되었습니다.")
                    } else {
                        ToastUtils.showShortToast(context, "예매 취소에 실패했습니다. 다시 시도해 주세요")
                    }
                } else {
                    _deletionSuccess.value = false
                    ToastUtils.showShortToast(context, "공연 시작 3시간 전까지만 예매 취소가 가능합니다.")
                }
            } catch (e: Exception) {
                _deletionSuccess.value = false
                ToastUtils.showShortToast(context, e.message ?: "예매 취소에 실패했습니다. 다시 시도해 주세요")
            } finally {
                _isLoading.value = false
            }
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
            throw e  // 날짜 파싱 실패시 예외를 throw하여 적절한 에러 메시지 표시
        }
    }
}
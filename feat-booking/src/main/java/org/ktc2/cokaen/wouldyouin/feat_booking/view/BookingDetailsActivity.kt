package org.ktc2.cokaen.wouldyouin.feat_booking.view

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import dagger.hilt.android.AndroidEntryPoint
import org.ktc2.cokaen.wouldyouin.core.DateTimeUtils
import org.ktc2.cokaen.wouldyouin.core.ToastUtils
import org.ktc2.cokaen.wouldyouin.feat_booking.R
import org.ktc2.cokaen.wouldyouin.feat_booking.databinding.ActivityBookingDetailsBinding
import org.ktc2.cokaen.wouldyouin.feat_booking.viewModel.BookingDetailsViewModel

@AndroidEntryPoint
class BookingDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBookingDetailsBinding
    private val viewModel: BookingDetailsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        // Observer 설정을 먼저 합니다
        setupObservers()

        // 그 다음 데이터 로드
        loadReservationData()

        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.reservation.observe(this) { reservation ->
            reservation?.let {
                binding.apply {
                    imageUrl = it.event.thumbnailUrl
                    eventName.text = it.event.title
                    eventLocation.text = it.event.location.detailAddress
                    eventDate.text = DateTimeUtils.formatDetailTimeString(it.event.startTime)
                    paymentDate.text = DateTimeUtils.formatDetailTimeString(it.reservationDate)
                    paymentAmount.text = "₩${it.price}"
                    reservationNumber.text = it.id.toString()
                    bookerName.text = it.member.nickname
                    ticketQuantity.text = it.quantity.toString()

                    btnBack.setOnClickListener {
                        finish()
                    }
                }
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            // 로딩 상태에 따른 UI 처리
            binding.progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun loadReservationData() {
        val reservationId = intent.getStringExtra("reservationId")?.toLongOrNull()
        Log.d("DetailBooking", "ReservationId: $reservationId")

        if (reservationId != null && reservationId != -1L) {
            viewModel.loadReservationDetail(reservationId)
        } else {
            Toast.makeText(this, "예매 내역을 찾을 수 없습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

//    private fun setupClickListeners() {
//        binding.cancelButton.setOnClickListener {
//            intent.getStringExtra("reservationId")?.toLongOrNull()?.let { id ->
//                viewModel.deleteReservation(id)
//                ToastUtils.showShortToast(this,"예매 취소가 완료되었습니다.")
//                finish()
//            }
//        }
//    }
}
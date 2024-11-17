package org.ktc2.cokaen.wouldyouin.feat_profile.view

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.ktc2.cokaen.wouldyouin.core.ToastUtils
import org.ktc2.cokaen.wouldyouin.core_navigation.ActivityNavigationOptions
import org.ktc2.cokaen.wouldyouin.core_navigation.DeepLinkDestinations
import org.ktc2.cokaen.wouldyouin.core_navigation.NavigationCommand
import org.ktc2.cokaen.wouldyouin.core_navigation.NavigationDestination
import org.ktc2.cokaen.wouldyouin.core_navigation.NavigationUtil
import org.ktc2.cokaen.wouldyouin.data.model.CurationResponse
import org.ktc2.cokaen.wouldyouin.data.model.EventResponse
import org.ktc2.cokaen.wouldyouin.data.model.MemberType
import org.ktc2.cokaen.wouldyouin.feat_profile.R
import org.ktc2.cokaen.wouldyouin.feat_profile.adapter.CurationAdapter
import org.ktc2.cokaen.wouldyouin.feat_profile.adapter.HashtagAdapter
import org.ktc2.cokaen.wouldyouin.feat_profile.adapter.PostAdapter
import org.ktc2.cokaen.wouldyouin.feat_profile.databinding.ActivityCuratorProfileBinding
import org.ktc2.cokaen.wouldyouin.feat_profile.viewModel.CurationViewModel
import org.ktc2.cokaen.wouldyouin.feat_profile.viewModel.LikesViewModel
import org.ktc2.cokaen.wouldyouin.feat_profile.viewModel.ProfileViewModel
import javax.inject.Inject

@AndroidEntryPoint
class CuratorProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCuratorProfileBinding
    private val profileViewModel: ProfileViewModel by viewModels()
    private val curationViewModel: CurationViewModel by viewModels()
    private val likesViewModel: LikesViewModel by viewModels()

    @Inject
    lateinit var navigationUtil: NavigationUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCuratorProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.lifecycleOwner = this

        val curatorId = intent.getStringExtra("curatorId")?.toLongOrNull()
        if (curatorId != null) {
            profileViewModel.fetchMemberProfile(curatorId, this)
            curationViewModel.loadCurations(curatorId)
            setupLikeButton(curatorId)
        } else {
            ToastUtils.showShortToast(this, "큐레이터 정보를 찾을 수 없습니다.")
            finish()
        }

        setupObservers(curatorId)
    }

    private fun setupObservers(curatorId: Long?) {
        profileViewModel.memberProfile.observe(this) { memberResponse ->
            memberResponse?.data?.let { member ->
                // 여기서 hashtags 확인
                Log.d("CuratorProfile", "Member hashtags: ${member.hashtags}")

                binding.nickname.text = member.nickname
                binding.role.text = member.memberType
                binding.likes.text = member.likes.toString()
                binding.intro.text = member.intro
                binding.emali.text = member.email

                //프로필 이미지
                binding.imageUrl = member.profileUrl

                // null이 아닌 경우에만 호출
                member.hashtags?.let { hashtags ->
                    setupHashtagRecyclerView(hashtags)
                }
            }
        }

        lifecycleScope.launch {
            curationViewModel.curations.collect { curations ->
                curatorId?.let {
                    setupCurationRecyclerView(curations, it)
                }
            }
        }
    }

    private fun setupHashtagRecyclerView(hashtags: List<String>) {
        // null 체크 추가
        if (hashtags.isNotEmpty()) {
            val hashtagAdapter = HashtagAdapter(hashtags)
            binding.hashtag.apply {
                layoutManager = LinearLayoutManager(
                    this@CuratorProfileActivity,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
                adapter = hashtagAdapter
            }
        }
    }

    private fun setupCurationRecyclerView(curations: List<CurationResponse>, curatorId: Long) {
        val curationAdapter = CurationAdapter(curations)
        binding.posts.apply {
            adapter = curationAdapter
            // GridLayoutManager로 변경하고 spanCount를 2로 설정하여 2열로 표시
            layoutManager = GridLayoutManager(
                this@CuratorProfileActivity,
                2,  // 2열로 설정
                GridLayoutManager.VERTICAL,  // 세로 스크롤로 변경
                false
            )

            // 아이템 간격 설정
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    val spacing = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        8f, // 8dp
                        resources.displayMetrics
                    ).toInt()

                    // 모든 방향에 spacing 적용
                    outRect.left = spacing
                    outRect.right = spacing
                    outRect.top = spacing
                    outRect.bottom = spacing

                    // 첫 번째 행의 아이템들에 대해 상단 여백 추가
                    if (parent.getChildLayoutPosition(view) < 2) {
                        outRect.top = spacing * 2
                    }
                }
            })

            curationAdapter.setOnItemClickListener { curation ->
                startActivityTo(curation.id)
            }

            // 무한 스크롤 설정
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as GridLayoutManager
                    val totalItemCount = layoutManager.itemCount
                    val lastVisibleItem = layoutManager.findLastCompletelyVisibleItemPosition()

                    // 스크롤이 끝에 도달하기 전에 추가 데이터 로드
                    if (!curationViewModel.isLoading.value && totalItemCount <= lastVisibleItem + 4) {
                        curationViewModel.loadCurations(curatorId)
                    }
                }
            })
        }
    }

    private fun startActivityTo(curationId: Long) {
        val command = NavigationCommand(
            destination = NavigationDestination.Activity(DeepLinkDestinations.DETAIL_CURATION_DEEPLINK),
            data = mapOf("curationId" to curationId.toString()),
            activityOptions = ActivityNavigationOptions(
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            )
        )
        navigationUtil.navigate(command)
    }

    private fun setupLikeButton(curatorId: Long) {
        likesViewModel.checkIfLiked(curatorId, MemberType.curator)

        lifecycleScope.launch {
            likesViewModel.isLiked.collect { isLiked ->
                updateLikeButtonColor(isLiked)
            }
        }

        binding.likeButton.setOnClickListener {
            lifecycleScope.launch {
                likesViewModel.toggleLike(curatorId, MemberType.curator)
                delay(300)
                profileViewModel.fetchMemberProfile(curatorId, this@CuratorProfileActivity)
            }
        }
    }

    private fun updateLikeButtonColor(isLiked: Boolean) {
        val color = if (isLiked) {
            Color.parseColor("#FF0000")
        } else {
            Color.parseColor("#808080")
        }
        binding.likeButton.setColorFilter(color, PorterDuff.Mode.SRC_IN)
    }
}
package com.example.feat_likes.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.feat_likes.viewModel.CuratorLikesViewModel
import com.example.feat_likes.viewModel.HostLikesViewModel
import org.ktc2.cokaen.wouldyouin.data.model.LikeResponse
import org.ktc2.cokaen.wouldyouin.feat_likes.databinding.LikedOrganizerItemBinding
import java.lang.reflect.Member

class LikedHostAdapter :
    ListAdapter<LikeResponse, LikedHostAdapter.MemberViewHolder>(LikeResponseDiffCallback()) {

    var onItemClick: ((LikeResponse) -> Unit)? = null
    var onHeartClick: ((LikeResponse) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = LikedOrganizerItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MemberViewHolder(
        private val binding: LikedOrganizerItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(member: LikeResponse) {
            binding.apply {
                organizerName.text = member.nickname
                organizerInfo.text = member.intro
                imageUrl = member.profileImageUrl
                hashtag.adapter = HashtagAdapter(member.hashtags)

                hashtag.layoutManager = LinearLayoutManager(root.context, LinearLayoutManager.HORIZONTAL, false)

                // 하트 버튼 클릭
                binding.heartButton.setOnClickListener {
                    onHeartClick?.invoke(member)  // ❌ ViewHolder 내부의 지역 변수 사용
                }

                // 프로필 클릭
                memberProfile.setOnClickListener {
                    onItemClick?.invoke(member)
                }

                root.setOnClickListener {
                    onItemClick?.invoke(member)
                }
            }
            binding.executePendingBindings()
        }
    }
}
package com.example.smartassistant.ui

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smartassistant.R
import com.example.smartassistant.databinding.ItemMessageBinding

class ChatAdapter : ListAdapter<ChatMessage, ChatAdapter.Holder>(Diff) {

    class Holder(val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val message = getItem(position)
        val isUser = message.author == Author.USER
        val bubble = holder.binding.bubble
        bubble.text = message.text
        bubble.setBackgroundResource(if (isUser) R.drawable.bg_bubble_user else R.drawable.bg_bubble_assistant)
        bubble.layoutParams = (bubble.layoutParams as FrameLayout.LayoutParams).apply {
            gravity = if (isUser) Gravity.END else Gravity.START
        }
    }

    private object Diff : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage) = oldItem == newItem
    }
}

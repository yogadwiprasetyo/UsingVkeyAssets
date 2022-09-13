package id.co.sistema.vkey.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vkey.android.internal.vguard.engine.BasicThreatInfo
import id.co.sistema.vkey.databinding.ItemThreatsBinding

class ThreatAdapter : ListAdapter<BasicThreatInfo, ThreatAdapter.ThreatViewHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThreatViewHolder {
        return ThreatViewHolder(
            ItemThreatsBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ThreatViewHolder, position: Int) {
        val basicThreatInfo = getItem(position)
        holder.bind(basicThreatInfo)
    }

    class ThreatViewHolder(private val binding: ItemThreatsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(basicThreatInfo: BasicThreatInfo) {
            binding.apply {
                tvThreatClass.text = basicThreatInfo.threatClass
                tvThreatInfo.text = basicThreatInfo.threatInfo
                tvThreatName.text = basicThreatInfo.threatName
            }
        }
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<BasicThreatInfo>() {
            override fun areItemsTheSame(
                oldItem: BasicThreatInfo,
                newItem: BasicThreatInfo
            ): Boolean {
                return oldItem.threatName == newItem.threatName
            }

            override fun areContentsTheSame(
                oldItem: BasicThreatInfo,
                newItem: BasicThreatInfo
            ): Boolean {
                return oldItem.threatName == newItem.threatName
            }
        }
    }
}
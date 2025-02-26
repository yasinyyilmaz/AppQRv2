package com.yasinyilmaz.appqr2.ui.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.ui.semantics.text
import androidx.recyclerview.widget.RecyclerView
import com.yasinyilmaz.appqr2.R
import com.yasinyilmaz.appqr2.data.model.Device
import com.yasinyilmaz.appqr2.databinding.ListItemDeviceBinding

class MyAdapter(
    private var devices: List<Device>,
    private val onClick: (String) -> Unit,
    private val onLongClick: (String) -> Unit,
    private val onSelectedItemsChanged: (List<Device>) -> Unit
) : RecyclerView.Adapter<MyAdapter.DeviceViewHolder>() {

    private var isSelectable = false
    private val selectedItems = mutableListOf<Device>()

    fun updateData(newDevices: List<Device>) {
        devices = newDevices
        notifyDataSetChanged()
    }

    fun setSelectable(selectable: Boolean) {
        isSelectable = selectable
        if (!isSelectable) {
            selectedItems.clear()
        }
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedItems.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ListItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.bind(device)
    }

    override fun getItemCount(): Int = devices.size

    inner class DeviceViewHolder(private val binding: ListItemDeviceBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(device: Device) {
            binding.deviceName.text = device.deviceName
            binding.deviceId.text = device.deviceId
            updateOnOffPlaceholderBackground(device.isOn)

            binding.root.setOnClickListener {
                if (isSelectable) {
                    if (selectedItems.contains(device)) {
                        selectedItems.remove(device)
                        binding.root.setCardBackgroundColor(Color.WHITE)
                    } else {
                        selectedItems.add(device)
                        binding.root.setCardBackgroundColor(Color.LTGRAY)
                    }
                    onSelectedItemsChanged(selectedItems)
                } else {
                    onClick(device.deviceId)
                }
            }

            binding.root.setOnLongClickListener {
                onLongClick(device.deviceId)
                true
            }

            // Seçili öğeleri işaretle
            if (isSelectable) {
                if (selectedItems.contains(device)) {
                    binding.root.setCardBackgroundColor(Color.LTGRAY)
                } else {
                    binding.root.setCardBackgroundColor(Color.WHITE)
                }
            } else {
                binding.root.setCardBackgroundColor(Color.WHITE)
            }
            // Cihaz adına göre resmi ayarla
            when {
                device.deviceName.contains("alarm", ignoreCase = true) -> {
                    binding.deviceImageView.setImageResource(R.drawable.alarm)
                }
                device.deviceName.contains("röle", ignoreCase = true) -> {
                    binding.deviceImageView.setImageResource(R.drawable.relay)
                }
                device.deviceName.contains("kamera", ignoreCase = true) -> {
                    binding.deviceImageView.setImageResource(R.drawable.camera)
                }
                else -> {
                    binding.deviceImageView.setImageResource(R.drawable.others)
                }
            }
        }
        private fun updateOnOffPlaceholderBackground(isOn: Boolean) {
            val backgroundColor = if (isOn) {
                Color.parseColor("#7dff99") // Açık durum
            } else {
                Color.GRAY // Kapalı durum
            }
            val backgroundDrawable = binding.onOffBackground.background
            if (backgroundDrawable is LayerDrawable) {
                val gradientDrawable = backgroundDrawable.getDrawable(0) as GradientDrawable
                gradientDrawable.setColor(backgroundColor)
            }
        }
    }
}
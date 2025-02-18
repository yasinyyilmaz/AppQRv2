package com.yasinyilmaz.appqr2.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.semantics.text
import androidx.glance.visibility
import androidx.recyclerview.widget.RecyclerView
import com.yasinyilmaz.appqr2.R
import com.yasinyilmaz.appqr2.data.model.Device
import com.yasinyilmaz.appqr2.databinding.ListItemDeviceBinding

class MyAdapter(
    private var devices: List<Device>,
    private val onClick: (String) -> Unit,
    private val onLongClick: (String) -> Unit,
    private val onSelectedItemsChanged: (List<Device>) -> Unit,
    private val onSwitchChanged: (String, Boolean) -> Unit
) : RecyclerView.Adapter<MyAdapter.DeviceViewHolder>() {

    private var isSelectable = false
    private val selectedItems = mutableListOf<Device>()
    private val lightBlueColor = Color.parseColor("#e8ffe0") // Açık mavi renk

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ListItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.bind(device)
        holder.setDeviceCategory(position + 1) // Oda numarasını ayarla
    }

    override fun getItemCount() = devices.size

    fun setSelectable(selectable: Boolean) {
        if (isSelectable == selectable) return

        isSelectable = selectable
        if (!isSelectable) {
            clearSelection()
        }
        notifyItemRangeChanged(0, itemCount)
    }

    fun updateData(newDevices: List<Device>) {
        devices = newDevices
        notifyDataSetChanged()
    }

    fun clearSelection() {
        val oldSize = selectedItems.size
        selectedItems.clear()
        if (oldSize > 0) {
            notifyItemRangeChanged(0, itemCount)
        }
    }

    inner class DeviceViewHolder(private val binding: ListItemDeviceBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(device: Device) {
            binding.deviceName.text = device.deviceName
            binding.deviceId.text = binding.root.context.getString(R.string.device_id, device.deviceId)

            binding.checkbox.visibility = if (isSelectable) View.VISIBLE else View.GONE
            binding.checkbox.isChecked = selectedItems.contains(device)

            // Checkbox'ın tıklanma olayını dinle
            binding.checkbox.setOnClickListener {
                if (isSelectable) {
                    toggleSelection(device)
                }
            }

            binding.root.setOnClickListener {
                if (!isSelectable) {
                    onClick(device.deviceId)
                }
            }

            binding.root.setOnLongClickListener {
                onLongClick(device.deviceId)
                true
            }

            // Switch'in durumunu dinle
            binding.onOffSwitch.setOnCheckedChangeListener { _, isChecked ->
                updateSwitchState(isChecked)
                onSwitchChanged(device.deviceId, isChecked)
            }
            updateSwitchState(binding.onOffSwitch.isChecked)

            // Cihaz ismine göre resmi ayarla
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
                    binding.deviceImageView.setImageResource(R.drawable.ic_launcher_foreground) // Varsayılan resim
                }
            }
            // Seçili öğelerin rengini ayarla
            if (selectedItems.contains(device)) {
                binding.cardView.setCardBackgroundColor(Color.LTGRAY) // Arka planı açık gri yap
            } else {
                binding.cardView.setCardBackgroundColor(Color.WHITE) // Arka planı beyaz yap
            }
        }

        private fun toggleSelection(device: Device) {
            if (selectedItems.contains(device)) {
                selectedItems.remove(device)
                binding.cardView.setCardBackgroundColor(Color.WHITE) // Arka planı beyaz yap
            } else {
                selectedItems.add(device)
                binding.cardView.setCardBackgroundColor(Color.LTGRAY) // Arka planı açık gri yap
            }
            binding.checkbox.isChecked = selectedItems.contains(device)
            onSelectedItemsChanged(selectedItems)
        }

        private fun updateSwitchState(isChecked: Boolean) {
            if (isChecked) {
                binding.cardView.setCardBackgroundColor(lightBlueColor)
            } else {
                binding.cardView.setCardBackgroundColor(Color.WHITE)
            }
        }
        fun setDeviceCategory(roomNumber: Int) {
            binding.deviceCategory.text = "Oda-$roomNumber"
        }
    }
}
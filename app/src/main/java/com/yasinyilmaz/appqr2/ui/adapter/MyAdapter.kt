package com.yasinyilmaz.appqr2.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
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
    private val bgColor = Color.parseColor("#ddffd1")
    private val switchStates = HashMap<String, Boolean>() // Cihaz ID'sine göre switch durumlarını saklar
    private val switchColors = HashMap<String, Int>() // Cihaz ID'sine göre switch renklerini saklar

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
        // Mevcut switch durumlarını ve renklerini koru
        val currentSwitchStates = switchStates.toMap()
        val currentSwitchColors = switchColors.toMap()

        // Yeni cihaz listesiyle switch durumlarını ve renklerini güncelle
        switchStates.clear()
        switchColors.clear()
        newDevices.forEach { device ->
            switchStates[device.deviceId] = currentSwitchStates[device.deviceId] ?: false
            switchColors[device.deviceId] = currentSwitchColors[device.deviceId] ?: Color.WHITE
        }

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

            // CardView'a tıklama olayını dinle
            binding.cardView.setOnClickListener {
                if (isSelectable) {
                    toggleSelection(device)
                } else {
                    onClick(device.deviceId)
                }
            }

            binding.root.setOnLongClickListener {
                onLongClick(device.deviceId)
                true
            }

            // Switch'in durumunu ve rengini HashMap'ten al
            val isChecked = switchStates[device.deviceId] ?: false
            binding.onOffSwitch.isChecked = isChecked
            updateCardBackgroundColor(device, isChecked, false) // Seçim modundan bağımsız olarak arka planı güncelle

            // Switch'in durumunu dinle
            binding.onOffSwitch.setOnCheckedChangeListener { _, isChecked ->
                switchStates[device.deviceId] = isChecked // HashMap'teki durumu güncelle
                updateCardBackgroundColor(device, isChecked, true) // Switch'e tıklandığında arka planı güncelle
                onSwitchChanged(device.deviceId, isChecked)
            }

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
                    binding.deviceImageView.setImageResource(R.drawable.others) // Varsayılan resim
                }
            }

            // Seçili öğelerin rengini ayarla
            if (selectedItems.contains(device)) {
                binding.cardView.setCardBackgroundColor(Color.LTGRAY) // Arka planı açık gri yap
            } else {
                updateCardBackgroundColor(device, isChecked, false)
            }
        }

        private fun toggleSelection(device: Device) {
            if (selectedItems.contains(device)) {
                selectedItems.remove(device)
            } else {
                selectedItems.add(device)
            }
            // Seçili öğelerin rengini güncelle
            updateCardBackgroundColor(device, switchStates[device.deviceId] ?: false, false)
            onSelectedItemsChanged(selectedItems)
        }

        private fun updateCardBackgroundColor(device: Device, isChecked: Boolean, isSwitchClicked: Boolean) {
            if (isSelectable && !isSwitchClicked) {
                // Seçim modundaysak ve switch'e tıklanmadıysa
                if (selectedItems.contains(device)) {
                    binding.cardView.setCardBackgroundColor(Color.LTGRAY)
                } else {
                    binding.cardView.setCardBackgroundColor(Color.WHITE)
                }
            } else {
                // Seçim modunda değilsek veya switch'e tıklandıysa
                val color = if (isChecked) bgColor else Color.WHITE
                switchColors[device.deviceId] = color
                if (selectedItems.contains(device)) {
                    binding.cardView.setCardBackgroundColor(Color.LTGRAY)
                } else {
                    binding.cardView.setCardBackgroundColor(color)
                }
            }
        }

        fun setDeviceCategory(roomNumber: Int) {
            binding.deviceCategory.text = "Oda-$roomNumber"
        }
    }
}
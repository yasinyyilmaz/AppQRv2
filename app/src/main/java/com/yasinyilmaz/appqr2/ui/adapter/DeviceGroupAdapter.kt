package com.yasinyilmaz.appqr2.ui.adapter

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.color
import androidx.recyclerview.widget.RecyclerView
import com.yasinyilmaz.appqr2.R
import com.yasinyilmaz.appqr2.data.model.Device
import com.yasinyilmaz.appqr2.databinding.ItemDeviceGroupBinding

class DeviceGroupAdapter(
    private val groupNames: List<String>,
    private val allGroupNames: List<String>, // Tüm grup isimlerini içeren liste
    private var selectedGroupName: String, // Başlangıçta seçili olan grup adı
    private val onGroupClick: (String) -> Unit,
    private val onSpinnerClick: (String) -> Unit // Spinner'a tıklandığında çağrılacak fonksiyon
) : RecyclerView.Adapter<DeviceGroupAdapter.DeviceGroupViewHolder>() {

    private var deviceCounts: Map<String, Int> = emptyMap()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceGroupViewHolder {
        val binding = ItemDeviceGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DeviceGroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceGroupViewHolder, position: Int) {
        val groupName = groupNames[position]
        val groupCount = deviceCounts[groupName] ?: 0
        holder.bind(groupName, groupCount, groupName == selectedGroupName)
    }

    override fun getItemCount(): Int {
        return groupNames.size
    }

    fun setSelectedGroupName(groupName: String) {
        selectedGroupName = groupName
        notifyDataSetChanged()
    }

    fun updateDeviceCounts(devices: List<Device>) {
        val newDeviceCounts = mutableMapOf<String, Int>()
        allGroupNames.forEach { groupName -> // Tüm grup isimlerini kullanarak sayım yap
            val count = when (groupName) {
                "tüm cihazlar" -> devices.size
                "alarmlar" -> devices.count { it.deviceName.contains("alarm", ignoreCase = true) }
                "röleler" -> devices.count { it.deviceName.contains("röle", ignoreCase = true) }
                "kameralar" -> devices.count { it.deviceName.contains("kamera", ignoreCase = true) }
                "ses sistemleri" -> devices.count { it.deviceName.contains("ses sistemi", ignoreCase = true) }
                else -> devices.count {
                    !it.deviceName.contains("alarm", ignoreCase = true) &&
                            !it.deviceName.contains("röle", ignoreCase = true) &&
                            !it.deviceName.contains("kamera", ignoreCase = true) &&
                            !it.deviceName.contains("ses sistemi", ignoreCase = true)
                }
            }
            newDeviceCounts[groupName] = count
        }
        deviceCounts = newDeviceCounts
        notifyDataSetChanged()
    }

    inner class DeviceGroupViewHolder(private val binding: ItemDeviceGroupBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(groupName: String, groupCount: Int, isSelected: Boolean) {
            binding.groupName = groupName
            binding.groupCount = groupCount
            binding.executePendingBindings()
            binding.root.setOnClickListener {
                onGroupClick(groupName)
                setSelectedGroupName(groupName)
            }
            // Seçili öğenin stilini ayarla
            if (isSelected) {
                binding.groupNameTextView.setTypeface(Typeface.DEFAULT_BOLD)
                binding.groupNameTextView.setTextColor(ContextCompat.getColor(binding.root.context, R.color.colorCategories))
                binding.groupNameTextView.textSize = 16f
                binding.deviceCountTextView.setTypeface(Typeface.DEFAULT_BOLD)
                binding.deviceCountTextView.setTextColor(ContextCompat.getColor(binding.root.context, R.color.colorCategories))
                binding.deviceCountTextView.textSize = 14f
            } else {
                binding.groupNameTextView.setTypeface(Typeface.DEFAULT)
                binding.groupNameTextView.setTextColor(ContextCompat.getColor(binding.root.context, R.color.colorUnselected))
                binding.deviceCountTextView.setTypeface(Typeface.DEFAULT)
                binding.deviceCountTextView.setTextColor(Color.GRAY)
            }
        }
    }
}
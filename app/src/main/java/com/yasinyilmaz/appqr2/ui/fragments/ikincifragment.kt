package com.yasinyilmaz.appqr2.ui.fragments

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.ui.semantics.text
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.yasinyilmaz.appqr2.R
import com.yasinyilmaz.appqr2.data.local.DatabaseHelper
import com.yasinyilmaz.appqr2.data.model.Device
import com.yasinyilmaz.appqr2.data.repository.DeviceRepository
import com.yasinyilmaz.appqr2.databinding.FragmentIkincifragmentBinding
import com.yasinyilmaz.appqr2.ui.viewmodel.DeviceViewModel
import com.yasinyilmaz.appqr2.ui.viewmodel.DeviceViewModelFactory
class ikincifragment : Fragment() {

    private lateinit var deviceNameTextView: TextView
    private lateinit var updateDeviceNameButton: View
    private lateinit var onOffContainer: FrameLayout
    private lateinit var onOffBackground: View
    private lateinit var binding: FragmentIkincifragmentBinding
    private var deviceId: String? = null
    private var isOn: Boolean = false
    private lateinit var device: Device

    private val databaseHelper by lazy { DatabaseHelper.getInstance(requireContext()) }
    private val deviceRepository by lazy { DeviceRepository(databaseHelper) }
    private val deviceViewModelFactory by lazy { DeviceViewModelFactory(deviceRepository) }
    private val deviceViewModel: DeviceViewModel by viewModels { deviceViewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            deviceId = it.getString(ARG_DEVICE_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentIkincifragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        deviceNameTextView = binding.deviceNameTextView
        updateDeviceNameButton = binding.updateDeviceNameButton
        onOffContainer = binding.onOffContainer
        onOffBackground = binding.onOffContainer.findViewById(R.id.onOffBackground)

        // deviceId'nin null olup olmadığını kontrol ediyoruz
        deviceId?.let { deviceId ->
            // Cihaz listesini gözlemle
            deviceViewModel.deviceList.observe(viewLifecycleOwner) { devices ->
                // Cihaz listesinde ilgili cihazı bul
                devices.find { it.deviceId == deviceId }?.let { device ->
                    // Cihaz adını TextView'e yazdır
                    deviceNameTextView.text = device.deviceName
                    this.device = device
                    this.isOn = device.isOn
                    updateOnOffButtonBackground(isOn)
                }
            }

            // Cihaz adını güncelleme butonuna tıklama dinleyicisi ekle
            updateDeviceNameButton.setOnClickListener {
                showUpdateDeviceNameDialog(deviceId)
            }

            // onOffContainer'a tıklama dinleyicisi ekle
            onOffContainer.setOnClickListener {
                isOn = !isOn // isOn değerini tersine çevir
                updateOnOffButtonBackground(isOn)
                // DeviceViewModel'i kullanarak isOn değerini güncelle
                deviceViewModel.updateDeviceSwitchState(deviceId, isOn)
            }
        }

        // Cihazları yükle
        deviceViewModel.loadDevices()
    }

    private fun showUpdateDeviceNameDialog(deviceId: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Ürünün Adını Güncelle")

        val input = EditText(requireContext())
        builder.setView(input)

        builder.setPositiveButton("Güncelle") { dialog, _ ->
            val newDeviceName = input.text.toString()
            if (newDeviceName.isNotEmpty()) {
                deviceViewModel.updateDeviceName(deviceId, newDeviceName)
                dialog.dismiss()
            }
        }
        builder.setNegativeButton("Vazgeç") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun updateOnOffButtonBackground(isOn: Boolean) {
        val backgroundColor = if (isOn) {
            Color.parseColor("#7dff99") // Açık durum
        } else {
            Color.GRAY // Kapalı durum
        }
        val backgroundDrawable = onOffBackground.background
        if (backgroundDrawable is LayerDrawable) {
            val gradientDrawable = backgroundDrawable.getDrawable(0) as GradientDrawable
            gradientDrawable.setColor(backgroundColor)
        }
    }

    companion object {
        private const val ARG_DEVICE_ID = "device_id"

        fun newInstance(deviceId: String): ikincifragment {
            val fragment = ikincifragment()
            val args = Bundle()
            args.putString(ARG_DEVICE_ID, deviceId)
            fragment.arguments = args
            return fragment
        }
    }
}
package com.yasinyilmaz.appqr2.ui.fragments

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.glance.visibility
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.journeyapps.barcodescanner.CaptureActivity
import com.yasinyilmaz.appqr2.R
import com.yasinyilmaz.appqr2.data.local.DatabaseHelper
import com.yasinyilmaz.appqr2.data.model.Device
import com.yasinyilmaz.appqr2.data.repository.DeviceRepository
import com.yasinyilmaz.appqr2.databinding.FragmentIlkfragmentBinding
import com.yasinyilmaz.appqr2.ui.adapter.DeviceGroupAdapter
import com.yasinyilmaz.appqr2.ui.adapter.MyAdapter
import com.yasinyilmaz.appqr2.ui.adapter.RemainingDeviceAdapter
import com.yasinyilmaz.appqr2.ui.viewmodel.DeviceViewModel
import com.yasinyilmaz.appqr2.ui.viewmodel.DeviceViewModelFactory
import com.yasinyilmaz.appqr2.utils.QRCodeEncoder
import org.json.JSONObject

class ilkfragment : Fragment() {

    private lateinit var binding: FragmentIlkfragmentBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var horizontalRecyclerView: RecyclerView
    private lateinit var qrButton: Button
    private lateinit var selectIcon: ImageButton
    private lateinit var myAdapter: MyAdapter
    private lateinit var deviceGroupAdapter: DeviceGroupAdapter
    private lateinit var remainingDeviceAdapter: RemainingDeviceAdapter
    private lateinit var spinnerButton: ImageButton

    private val databaseHelper by lazy { DatabaseHelper.getInstance(requireContext()) }
    private val deviceRepository by lazy { DeviceRepository(databaseHelper) }
    private val deviceViewModelFactory by lazy { DeviceViewModelFactory(deviceRepository) }
    private val deviceViewModel: DeviceViewModel by viewModels { deviceViewModelFactory }

    private var isSelectable = false
    private val selectedDevices = mutableListOf<Device>()
    private val allGroupNames = listOf(
        "tüm cihazlar", "alarmlar", "röleler", "kameralar", "ses sistemleri", "diğer cihazlar"
    )
    // Başlangıçta seçili olan grup adını tanımla
    private var selectedGroupName: String = "tüm cihazlar"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentIlkfragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews()
        setupRecyclerView()
        setupHorizontalRecyclerView()
        setupListeners()
        setupBackPressedDispatcher()
        observeViewModel()
        loadData()
        checkSpinnerButtonVisibility()
    }

    private fun initializeViews() {
        recyclerView = binding.recyclerView
        horizontalRecyclerView = binding.horizontalRecyclerView
        qrButton = binding.btnqr
        selectIcon = binding.selectIcon
        spinnerButton = binding.spinnerButton
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = GridLayoutManager(context, 2)
        myAdapter = MyAdapter(
            emptyList(),
            onClick = { deviceId ->
                val secondFragment = ikincifragment.newInstance(deviceId)
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, secondFragment)
                    .addToBackStack(null)
                    .commit()
            },
            onLongClick = { deviceId -> showDeleteConfirmationDialog(deviceId) },
            onSelectedItemsChanged = { selectedList -> updateSelectedDevices(selectedList) },
            onSwitchChanged = { deviceId, isChecked -> println("Device ID: $deviceId, isChecked: $isChecked") }
        )
        recyclerView.adapter = myAdapter
    }

    private fun checkSpinnerButtonVisibility() {
        val isVisible = allGroupNames.size > 4
        spinnerButton.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    private fun setupHorizontalRecyclerView() {
        horizontalRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        deviceGroupAdapter = DeviceGroupAdapter(
            allGroupNames.take(4),
            allGroupNames,
            selectedGroupName, // Başlangıçta seçili olan grup adını gönder
            onGroupClick = { groupName ->
                filterDevicesByGroupName(groupName)
            },
            onSpinnerClick = { groupName ->
                filterDevicesByGroupName(groupName)
                deviceGroupAdapter.setSelectedGroupName(groupName)
            }
        )
        horizontalRecyclerView.adapter = deviceGroupAdapter

        remainingDeviceAdapter = RemainingDeviceAdapter(
            requireContext(),
            allGroupNames.drop(4),
            onSpinnerClick = { groupName ->
                filterDevicesByGroupName(groupName)
                deviceGroupAdapter.setSelectedGroupName(groupName)
            }
        )
        // horizontalRecyclerView.adapter = remainingDeviceAdapter // Bu satırı kaldırın
        checkHorizontalScrollbarVisibility()
    }

    private fun checkHorizontalScrollbarVisibility() {
        horizontalRecyclerView.post {
            val layoutManager = horizontalRecyclerView.layoutManager as? LinearLayoutManager
            if (layoutManager != null) {
                val totalItemWidth = (0 until layoutManager.itemCount).sumOf { index ->
                    val viewHolder = horizontalRecyclerView.findViewHolderForAdapterPosition(index)
                    viewHolder?.itemView?.measuredWidth ?: 0
                }

                val recyclerViewWidth = horizontalRecyclerView.measuredWidth
                val hasHorizontalScroll = totalItemWidth > recyclerViewWidth

                spinnerButton.visibility = View.VISIBLE
            }
        }
    }

    private fun filterDevicesByGroupName(groupName: String) {
        val allDevices = deviceViewModel.deviceList.value ?: emptyList()
        val filteredDevices = when (groupName) {
            "tüm cihazlar" -> allDevices
            "alarmlar" -> allDevices.filter { it.deviceName.contains("alarm", ignoreCase = true) }
            "röleler" -> allDevices.filter { it.deviceName.contains("röle", ignoreCase = true) }
            "kameralar" -> allDevices.filter { it.deviceName.contains("kamera", ignoreCase = true) }
            "ses sistemleri" -> allDevices.filter { it.deviceName.contains("ses sistemi", ignoreCase = true) }
            else -> allDevices.filter {
                !it.deviceName.contains("alarm", ignoreCase = true) &&
                        !it.deviceName.contains("röle", ignoreCase = true) &&
                        !it.deviceName.contains("kamera", ignoreCase = true) &&
                        !it.deviceName.contains("ses sistemi", ignoreCase = true)
            }
        }
        updateMainRecyclerView(filteredDevices)
        // Her zaman tüm cihazları kullanarak sayıları güncelle
        deviceGroupAdapter.updateDeviceCounts(allDevices)
        checkHorizontalScrollbarVisibility()
        // Seçili grup adını güncelle
        selectedGroupName = groupName
        deviceGroupAdapter.setSelectedGroupName(groupName)
    }

    private fun setupListeners() {
        qrButton.setOnClickListener { startQRCodeScanner() }
        selectIcon.setOnClickListener { toggleSelectionMode() }
        spinnerButton.setOnClickListener { showPopupMenu(spinnerButton) }
    }

    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)
        val remainingGroups = allGroupNames.drop(4)
        for (group in remainingGroups) {
            popupMenu.menu.add(group)
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            val selectedGroupName = menuItem.title.toString()
            remainingDeviceAdapter.onSpinnerClick(selectedGroupName)
            true
        }

        popupMenu.show()
    }

    private fun setupBackPressedDispatcher() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (isSelectable) {
                toggleSelectionMode()
            } else {
                isEnabled = false
                requireActivity().onBackPressed()
            }
        }
    }

    private fun observeViewModel() {
        deviceViewModel.deviceList.observe(viewLifecycleOwner) { devices ->
            updateMainRecyclerView(devices)
            // Her zaman tüm cihazları kullanarak sayıları güncelle
            deviceGroupAdapter.updateDeviceCounts(devices)
            checkHorizontalScrollbarVisibility()
            checkSpinnerButtonVisibility() // spinnerButton görünürlüğünü kontrol et
        }
    }


    private fun updateMainRecyclerView(devices: List<Device>) {
        myAdapter.updateData(devices)
    }

    private val qrScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val qrResult = result.data?.getStringExtra("SCAN_RESULT")
            qrResult?.let { processQRCodeData(it) }
        }
    }

    private fun startQRCodeScanner() {
        val intent = Intent(requireContext(), CaptureActivity::class.java)
        qrScannerLauncher.launch(intent)
    }

    private fun processQRCodeData(qrData: String) {
        try {
            val jsonObject = JSONObject(qrData)
            val deviceId = jsonObject.getString("deviceid")
            val deviceName = jsonObject.getString("devicename")
            val newDevice = Device(deviceId, deviceName, qrData)
            deviceViewModel.addDevice(newDevice)
        } catch (e: Exception) {
            e.printStackTrace()
            showErrorDialog("Geçersiz QR Kodu")
        }
    }

    private fun loadData() {
        deviceViewModel.loadDevices()
    }

    private fun showDeleteConfirmationDialog(deviceId: String) {
        AlertDialog.Builder(requireContext())
            .setMessage("Bu ürünü silmek istediğine emin misin?")
            .setCancelable(false)
            .setPositiveButton("Evet") { _, _ -> deviceViewModel.deleteDevice(deviceId) }
            .setNegativeButton("Hayır") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun updateSelectedDevices(selectedList: List<Device>) {
        selectedDevices.clear()
        selectedDevices.addAll(selectedList)
    }

    private fun toggleSelectionMode() {
        isSelectable = !isSelectable
        myAdapter.setSelectable(isSelectable)

        val selectButton = view?.findViewById<Button>(R.id.btnSelect)
        selectButton?.visibility = if (isSelectable) View.VISIBLE else View.GONE
        // Sadece seçilebilir modda ise tıklanabilir yap
        if (isSelectable) {
            selectButton?.setOnClickListener {
                if (selectedDevices.isNotEmpty()) {
                    showQRCodeDialog()
                } else {
                    showErrorDialog("Hiçbir cihaz seçmediniz!")
                }
            }
        } else {
            selectButton?.setOnClickListener(null) // Seçim modu kapalıyken tıklamayı devre dışı bırak
            selectedDevices.clear()
            myAdapter.clearSelection()
        }
    }

    private fun showQRCodeDialog() {
        val dataToEncode = selectedDevices.joinToString(separator = "\n") { device ->
            "${device.deviceName}, ${device.deviceId}, ${device.rawQrData}"
        }
        val qrBitmap: Bitmap? = QRCodeEncoder.generateQRCode(dataToEncode)
        if (qrBitmap != null) {
            showQRCodeImageDialog(qrBitmap)
        } else {
            showErrorDialog("QR Kodu oluşturulurken bir hata oluştu!")
        }
    }

    private fun showQRCodeImageDialog(qrBitmap: Bitmap) {
        val builder = AlertDialog.Builder(requireContext())
        val imageView = ImageView(requireContext())
        imageView.setImageBitmap(qrBitmap)
        builder.setView(imageView)
            .setPositiveButton("Tamam") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton("Tamam") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }
}
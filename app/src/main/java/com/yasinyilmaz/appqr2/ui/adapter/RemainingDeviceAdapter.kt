package com.yasinyilmaz.appqr2.ui.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class RemainingDeviceAdapter(
    context: Context,
    private val groupNames: List<String>,
    val onSpinnerClick: (String) -> Unit
) : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, groupNames) {

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)
        val groupName = getItem(position)
        if (groupName != null) {
            (view as TextView).text = groupName
            view.setOnClickListener {
                onSpinnerClick(groupName)
            }
        }
        return view
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val groupName = getItem(position)
        if (groupName != null) {
            (view as TextView).text = groupName
        }
        return view
    }
}
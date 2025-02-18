package com.yasinyilmaz.appqr2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.yasinyilmaz.appqr2.ui.fragments.ilkfragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            val firstFragment = ilkfragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, firstFragment)
                .commit()
        }
    }
}

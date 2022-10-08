package com.liqiaotong.windowlist

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.liqiaotong.windowlist.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        binding?.add?.setOnClickListener {
            binding?.windowListView?.addWindow(binding?.root,true)
        }
        binding?.close?.setOnClickListener {
            binding?.windowListView?.closeWindow()
        }
    }
}
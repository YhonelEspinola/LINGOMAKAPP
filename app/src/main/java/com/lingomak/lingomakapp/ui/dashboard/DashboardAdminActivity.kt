package com.lingomak.lingomakapp.ui.dashboard

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.lingomak.lingomakapp.databinding.DashboardAdminBinding

class DashboardAdminActivity : AppCompatActivity() {

    private lateinit var binding: DashboardAdminBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DashboardAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
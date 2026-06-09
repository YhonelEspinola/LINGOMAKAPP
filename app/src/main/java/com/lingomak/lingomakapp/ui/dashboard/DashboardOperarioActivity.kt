package com.lingomak.lingomakapp.ui.dashboard

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.lingomak.lingomakapp.databinding.DashboardOperarioBinding

class DashboardOperarioActivity : AppCompatActivity() {


    private lateinit var  binding: DashboardOperarioBinding

    override fun onCreate(saveInstanceState: Bundle?){
        super.onCreate(saveInstanceState)

        binding = DashboardOperarioBinding.inflate(layoutInflater)

        setContentView(binding.root)
    }

}
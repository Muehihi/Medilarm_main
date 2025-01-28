package com.example.medilarm

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HomePage : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var alarmAdapter: AlarmAdapter
    private var alarmList: MutableList<AlarmData> = mutableListOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView = findViewById(R.id.recyclerView)
        alarmAdapter = AlarmAdapter(alarmList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = alarmAdapter

        loadAlarms()
        // Retrieve the alarm list from Intent
        val intent = intent
        if (intent.hasExtra("alarmList")) {
            alarmList = intent.getSerializableExtra("alarmList") as MutableList<AlarmData>
        }

        
        val btnAdd: ImageView = findViewById(R.id.btn_add)
        btnAdd.setOnClickListener {
            val intent = Intent(this, Alarm::class.java)
            startActivity(intent)
        }

        val btnProfile: ImageView = findViewById(R.id.btn_Profile)
        btnProfile.setOnClickListener {
            val intent = Intent(this, Profile::class.java)
            startActivity(intent)
        }

        val btnshelf = findViewById<ImageView>(R.id.btn_shelf)
        btnshelf.setOnClickListener {
            val intent = Intent(this, ShelfActivity::class.java)
            startActivity(intent)
        }
    }
    private fun loadAlarms() {
        val sharedPreferences = getSharedPreferences("alarmPrefs", MODE_PRIVATE)
        val alarmDataString = sharedPreferences.getString("alarmData", "") ?: ""

        if (alarmDataString.isNotEmpty()) {
            // Convert the saved string back to a list of AlarmData objects
            val alarmStrings = alarmDataString.split(",")
            alarmList.clear()
            for (alarmString in alarmStrings) {
                val parts = alarmString.split(":") // Assuming the format is something like "MedicineName:Dosage:hour:minute"
                if (parts.size == 4) {
                    val alarmData = AlarmData(parts[0], parts[1], parts[2].toInt(), parts[3].toInt())
                    alarmList.add(alarmData)
                }
            }
            alarmAdapter.notifyDataSetChanged()
        }
    }

}
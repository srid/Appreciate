package ca.srid.appreciate

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Settings UI and permission management.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var settings: SettingsStore

    private lateinit var reminderTextEdit: EditText
    private lateinit var packSpinner: Spinner
    private lateinit var enabledSwitch: Switch
    private lateinit var bootSwitch: Switch
    private lateinit var minIntervalSeek: SeekBar
    private lateinit var maxIntervalSeek: SeekBar
    private lateinit var durationSeek: SeekBar
    private lateinit var minIntervalLabel: TextView
    private lateinit var maxIntervalLabel: TextView
    private lateinit var durationLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        settings = SettingsStore(this)

        reminderTextEdit = findViewById(R.id.reminderTextEdit)
        packSpinner = findViewById(R.id.packSpinner)
        enabledSwitch = findViewById(R.id.enabledSwitch)
        bootSwitch = findViewById(R.id.bootSwitch)
        minIntervalSeek = findViewById(R.id.minIntervalSeek)
        maxIntervalSeek = findViewById(R.id.maxIntervalSeek)
        durationSeek = findViewById(R.id.durationSeek)
        minIntervalLabel = findViewById(R.id.minIntervalLabel)
        maxIntervalLabel = findViewById(R.id.maxIntervalLabel)
        durationLabel = findViewById(R.id.durationLabel)

        loadSettings()
        setupListeners()
        checkOverlayPermission()
        checkAlarmPermission()
        requestNotificationPermission()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        saveSettings()
    }

    private fun loadSettings() {
        reminderTextEdit.setText(settings.reminderText)
        enabledSwitch.isChecked = settings.isEnabled
        bootSwitch.isChecked = settings.launchAtBoot

        // Pack spinner
        refreshPackSpinner()

        // SeekBar: min interval 0.5-60 min (steps of 0.5)
        minIntervalSeek.max = 119 // 0.5 to 60, step 0.5 => (60-0.5)/0.5 = 119
        minIntervalSeek.progress = ((settings.minIntervalMinutes - 0.5f) / 0.5f).toInt()
        updateMinLabel()

        // SeekBar: max interval 1-120 min (steps of 1)
        maxIntervalSeek.max = 119 // 1 to 120
        maxIntervalSeek.progress = (settings.maxIntervalMinutes - 1f).toInt()
        updateMaxLabel()

        // SeekBar: duration 2-10s (steps of 0.5)
        durationSeek.max = 16 // (10-2)/0.5 = 16
        durationSeek.progress = ((settings.displayDurationSeconds - 2f) / 0.5f).toInt()
        updateDurationLabel()
    }

    private fun saveSettings() {
        settings.reminderText = reminderTextEdit.text.toString()
        settings.isEnabled = enabledSwitch.isChecked
        settings.launchAtBoot = bootSwitch.isChecked

        // Restart or stop service based on enabled state
        val serviceIntent = Intent(this, OverlayService::class.java)
        if (enabledSwitch.isChecked) {
            serviceIntent.action = OverlayService.ACTION_START
            startForegroundService(serviceIntent)
        } else {
            serviceIntent.action = OverlayService.ACTION_STOP
            startService(serviceIntent)
        }
    }

    private fun setupListeners() {
        // Pack spinner
        packSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val names = settings.packNames
                if (position < names.size) {
                    val name = names[position]
                    if (name != settings.selectedPack) {
                        settings.selectedPack = name
                        reminderTextEdit.setText(settings.reminderText)
                    }
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Text edits update the current pack's content
        reminderTextEdit.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                settings.reminderText = s.toString()
            }
        })

        // Add pack button
        findViewById<Button>(R.id.addPackButton).setOnClickListener {
            val input = android.widget.EditText(this)
            input.hint = "Pack name"
            android.app.AlertDialog.Builder(this)
                .setTitle("New Pack")
                .setMessage("Enter a name for the new reminder pack:")
                .setView(input)
                .setPositiveButton("Add") { _, _ ->
                    val name = input.text.toString().trim()
                    if (settings.addPack(name)) {
                        refreshPackSpinner()
                        reminderTextEdit.setText("")
                    } else {
                        Toast.makeText(this, "Pack name already exists or is empty", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Delete pack button
        findViewById<Button>(R.id.deletePackButton).setOnClickListener {
            if (settings.packs.size <= 1) {
                Toast.makeText(this, "Cannot delete the last pack", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val name = settings.selectedPack
            if (settings.deletePack(name)) {
                refreshPackSpinner()
                reminderTextEdit.setText(settings.reminderText)
            }
        }

        enabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.isEnabled = isChecked
            val serviceIntent = Intent(this, OverlayService::class.java)
            if (isChecked) {
                serviceIntent.action = OverlayService.ACTION_START
                startForegroundService(serviceIntent)
            } else {
                serviceIntent.action = OverlayService.ACTION_STOP
                startService(serviceIntent)
            }
        }

        bootSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.launchAtBoot = isChecked
        }

        minIntervalSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                settings.minIntervalMinutes = 0.5f + progress * 0.5f
                updateMinLabel()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        maxIntervalSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                settings.maxIntervalMinutes = 1f + progress
                updateMaxLabel()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        durationSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                settings.displayDurationSeconds = 2f + progress * 0.5f
                updateDurationLabel()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        findViewById<Button>(R.id.showNowButton).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Grant overlay permission first", Toast.LENGTH_SHORT).show()
                requestOverlayPermission()
                return@setOnClickListener
            }
            saveSettings()
            val intent = Intent(this, OverlayService::class.java).apply {
                action = OverlayService.ACTION_SHOW_NOW
            }
            startService(intent)
        }

        findViewById<Button>(R.id.grantPermissionButton).setOnClickListener {
            requestOverlayPermission()
        }

        findViewById<Button>(R.id.grantAlarmButton).setOnClickListener {
            requestAlarmPermission()
        }
    }

    private fun refreshPackSpinner() {
        val packNames = settings.packNames
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, packNames)
        packSpinner.adapter = adapter
        val selectedIndex = packNames.indexOf(settings.selectedPack)
        if (selectedIndex >= 0) packSpinner.setSelection(selectedIndex)
    }

    private fun updateMinLabel() {
        val v = settings.minIntervalMinutes
        minIntervalLabel.text = if (v < 1) "${(v * 60).toInt()}s" else "${v}min"
    }

    private fun updateMaxLabel() {
        val v = settings.maxIntervalMinutes
        maxIntervalLabel.text = "${v.toInt()}min"
    }

    private fun updateDurationLabel() {
        durationLabel.text = "${settings.displayDurationSeconds}s"
    }

    private fun checkOverlayPermission() {
        val btn = findViewById<Button>(R.id.grantPermissionButton)
        if (Settings.canDrawOverlays(this)) {
            btn.text = "✅ Overlay Permission Granted"
            btn.isEnabled = false
        } else {
            btn.text = "⚠️ Grant Overlay Permission"
            btn.isEnabled = true
        }
        tryStartService()
    }

    private fun checkAlarmPermission() {
        val btn = findViewById<Button>(R.id.grantAlarmButton)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (alarmManager.canScheduleExactAlarms()) {
            btn.text = "✅ Alarm Permission Granted"
            btn.isEnabled = false
        } else {
            btn.text = "⚠️ Grant Alarm Permission"
            btn.isEnabled = true
        }
        tryStartService()
    }

    /** Only start the service when ALL required permissions are granted. */
    private fun tryStartService() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Settings.canDrawOverlays(this) && alarmManager.canScheduleExactAlarms() && settings.isEnabled) {
            val intent = Intent(this, OverlayService::class.java).apply {
                action = OverlayService.ACTION_START
            }
            startForegroundService(intent)
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun requestAlarmPermission() {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        checkOverlayPermission()
        checkAlarmPermission()
    }
}

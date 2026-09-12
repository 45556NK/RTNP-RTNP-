package com.rtnp.demo

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import com.rtnp.demo.audio.AudioManager
import com.rtnp.demo.core.TemplateRegistry
import com.rtnp.demo.data.JsonUnitLoader
import com.rtnp.demo.data.JsonWeaponLoader
import com.rtnp.demo.gpu.GameGLView
import com.rtnp.demo.logger.Logger
import com.rtnp.demo.trait.TraitManager
import com.rtnp.demo.ui.CreditsScreen
import com.rtnp.demo.ui.LoadingScreen
import com.rtnp.demo.ui.MainMenu
import com.rtnp.demo.ui.WarningMessage
import com.rtnp.demo.ui.settings.SettingScreen

class MainActivity : Activity() {

    companion object {
        private const val REQUEST_STORAGE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Logger.init(this, Logger.Level.DEBUG)
        super.onCreate(savedInstanceState)
        WarningMessage.init(this)

        requestStoragePermissions()
        setupFullScreen()
        disableLogs()

        initAudio()
        loadUnitTemplates()
        loadWeaponTemplates()
        initTemplateRegistry()
        TraitManager.init(this)

        showLoadingScreen()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onPause() {
        super.onPause()
        try {
            AudioManager.pause()
        } catch (_: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        try {
            if (AudioManager.isMusicOn()) {
                AudioManager.resume()
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            AudioManager.release()
        } catch (_: Exception) {}
    }

    private fun setupFullScreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }

    private fun disableLogs() {
        android.util.Log.println(android.util.Log.ASSERT, "TAG", "disable all logs")
    }

    private fun initAudio() {
        try {
            AudioManager.init(this)
        } catch (_: Exception) {}
    }

    private fun loadUnitTemplates() {
        try {
            JsonUnitLoader.loadFromAssets(this)
        } catch (_: Exception) {}
    }

    private fun loadWeaponTemplates() {
        try {
            JsonWeaponLoader.loadFromAssets(this)
        } catch (_: Exception) {}
    }

    private fun initTemplateRegistry() {
        try {
            TemplateRegistry.init(this)
        } catch (_: Exception) {}
    }

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (_: Exception) {}
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    ),
                    REQUEST_STORAGE
                )
            }
        }
    }

    private fun showLoadingScreen() {
        if (isFinishing || isDestroyed) return
        try {
            setContentView(LoadingScreen(this))
        } catch (_: Exception) {
            setContentView(GameView(this))
        }
    }

    fun onLoadingComplete() {
        if (isFinishing || isDestroyed) return
        try {
            setContentView(MainMenu(this))
        } catch (_: Exception) {}
    }

    fun startGame() {
        if (isFinishing || isDestroyed) return
        try {
            val rootLayout = FrameLayout(this)
            val gameGLView = GameGLView(this)
            val gameUIView = GameView(this)

            gameUIView.setGpuRenderer(gameGLView.renderer)

            rootLayout.addView(gameGLView)
            rootLayout.addView(gameUIView)

            setContentView(rootLayout)
        } catch (_: Exception) {}
    }

    fun showMainMenu() {
        if (isFinishing || isDestroyed) return
        try {
            removeAllOverlays()
            setContentView(MainMenu(this))
        } catch (_: Exception) {}
    }

    private fun showOverlayView(overlayView: View) {
        val container = findViewById<FrameLayout>(android.R.id.content) ?: return
        overlayView.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        container.addView(overlayView)
    }

    private fun removeAllOverlays() {
        val container = findViewById<FrameLayout>(android.R.id.content) ?: return
        val toRemove = mutableListOf<View>()
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child.tag == "overlay") {
                toRemove.add(child)
            }
        }
        toRemove.forEach { container.removeView(it) }
    }

    fun showSettings(returnView: View) {
        if (isFinishing || isDestroyed) return
        try {
            val settingScreen = SettingScreen(this, object : SettingScreen.OnBackListener {
                override fun onBack() {
                    if (isFinishing || isDestroyed) return
                    // 通过 tag 查找并移除覆盖层
                    val container = findViewById<FrameLayout>(android.R.id.content)
                    container?.let {
                        for (i in it.childCount - 1 downTo 0) {
                            val child = it.getChildAt(i)
                            if (child.tag == "overlay") {
                                it.removeView(child)
                                break
                            }
                        }
                    }
                }
            })
            settingScreen.tag = "overlay"
            showOverlayView(settingScreen)
        } catch (_: Exception) { }
    }

    fun showCredits(returnView: View) {
        if (isFinishing || isDestroyed) return
        try {
            val creditsScreen = CreditsScreen(this, object : CreditsScreen.OnBackListener {
                override fun onBack() {
                    if (isFinishing || isDestroyed) return
                    val container = findViewById<FrameLayout>(android.R.id.content)
                    container?.let {
                        for (i in it.childCount - 1 downTo 0) {
                            val child = it.getChildAt(i)
                            if (child.tag == "overlay") {
                                it.removeView(child)
                                break
                            }
                        }
                    }
                }
            })
            creditsScreen.tag = "overlay"
            showOverlayView(creditsScreen)
        } catch (_: Exception) { }
    }

}
package com.scanfill.app.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.scanfill.app.R
import com.scanfill.app.format.TextFormatter
import com.scanfill.app.ocr.OcrManager
import com.scanfill.app.prefs.Prefs
import com.scanfill.app.service.PasteAccessibilityService
import com.scanfill.app.theme.ThemeEngine
import com.scanfill.app.util.Bitmaps
import com.scanfill.app.util.Glass
import com.scanfill.app.util.RecentPhoto
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 扫描页（半屏悬浮卡片）：
 * - 竖屏悬浮在屏幕下方一半，横屏悬浮在右方一半，不全屏
 * - 点击卡片外区域关闭
 * - 悬浮球 / 磁贴 / 通知 / 应用内 -> 识别后填入
 * - PROCESS_TEXT（长按编辑菜单"扫描填入"）-> 识别结果替换选中的文字
 */
class ScanActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SOURCE = "source"
        const val SOURCE_BALL = "ball"
        const val SOURCE_MAIN = "main"
        const val SOURCE_TILE = "tile"
    }

    private var processTextMode = false
    private var fillFromMain = false

    private lateinit var root: FrameLayout
    private lateinit var card: FrameLayout
    private lateinit var topBar: LinearLayout
    private lateinit var viewfinder: LinearLayout
    private lateinit var busyOverlay: LinearLayout
    private lateinit var resultCard: FrameLayout
    private lateinit var resultPanel: LinearLayout
    private lateinit var bottomPanel: LinearLayout
    private lateinit var recentPhoto: ImageView
    private lateinit var btnAlbum: ImageButton
    private lateinit var btnFlash: ImageButton
    private lateinit var btnShutter: FrameLayout
    private lateinit var shutterCore: View
    private lateinit var chipEngine: TextView
    private lateinit var etResult: EditText
    private lateinit var tvMeta: TextView
    private lateinit var btnFill: TextView
    private lateinit var btnCopy: TextView

    private val cameraExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    private var imageCapture: ImageCapture? = null
    private var camera: androidx.camera.core.Camera? = null
    private var torchOn = false
    private var lastCapture: android.graphics.Bitmap? = null
    private var recognizeStartAt = 0L
    private var recentUri: Uri? = null

    private val requestCamera =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) setupCamera() else {
                Toast.makeText(this, R.string.camera_permission_needed, Toast.LENGTH_LONG).show()
                finish()
            }
        }

    private val requestMedia =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            setupRecentPhoto()
        }

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { recognizeBitmap(Bitmaps.fromUri(this, it)) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        processTextMode = Intent.ACTION_PROCESS_TEXT == intent.action
        fillFromMain = intent.getStringExtra(EXTRA_SOURCE) == SOURCE_MAIN

        setContentView(R.layout.activity_scan)
        bindViews()
        applyCardStyle()
        setupRecentPhoto()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            setupCamera()
        } else {
            requestCamera.launch(Manifest.permission.CAMERA)
        }
    }

    private fun bindViews() {
        root = findViewById(R.id.scan_root)
        card = findViewById(R.id.scan_card)
        topBar = findViewById(R.id.top_bar)
        viewfinder = findViewById(R.id.viewfinder)
        busyOverlay = findViewById(R.id.busy_overlay)
        resultCard = findViewById(R.id.result_card)
        resultPanel = findViewById(R.id.result_panel)
        bottomPanel = findViewById(R.id.bottom_panel)
        recentPhoto = findViewById(R.id.iv_recent)
        btnAlbum = findViewById(R.id.btn_album)
        btnFlash = findViewById(R.id.btn_flash)
        btnShutter = findViewById(R.id.btn_shutter)
        shutterCore = findViewById(R.id.shutter_core)
        chipEngine = findViewById(R.id.chip_engine)
        etResult = findViewById(R.id.et_result)
        tvMeta = findViewById(R.id.tv_meta)
        btnFill = findViewById(R.id.btn_fill)
        btnCopy = findViewById(R.id.btn_copy)

        // 点击卡片外区域关闭
        root.setOnClickListener { finish() }
        card.setOnClickListener { /* 吃掉点击，防止透传到遮罩 */ }

        findViewById<View>(R.id.btn_close).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btn_retake).setOnClickListener { backToCamera() }
        btnCopy.setOnClickListener { copyAndFinish() }
        btnFill.setOnClickListener { onFill() }
        btnShutter.setOnClickListener { takePhoto() }
        btnAlbum.setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        btnFlash.setOnClickListener { toggleTorch() }
        recentPhoto.setOnClickListener {
            recentUri?.let { recognizeBitmap(Bitmaps.fromUri(this, it)) }
        }
        chipEngine.setOnClickListener { toggleEngine() }

        updateEngineChip()
        btnFill.text = when {
            processTextMode -> getString(R.string.btn_replace)
            else -> getString(R.string.btn_fill)
        }
        if (!processTextMode && Prefs.fillMode == Prefs.FILL_CLIPBOARD) {
            btnFill.text = getString(R.string.btn_copy)
            btnCopy.visibility = View.GONE
        }
    }

    /** 半屏悬浮卡片：竖屏占下半屏，横屏占右半屏，四周留边 */
    private fun applyCardStyle() {
        val dm = resources.displayMetrics
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val margin = Glass.dp(this, 10)

        val lp = card.layoutParams as FrameLayout.LayoutParams
        if (isLandscape) {
            lp.width = dm.widthPixels / 2
            lp.height = FrameLayout.LayoutParams.MATCH_PARENT
            lp.gravity = Gravity.END or Gravity.CENTER_VERTICAL
        } else {
            lp.width = FrameLayout.LayoutParams.MATCH_PARENT
            lp.height = dm.heightPixels / 2
            lp.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        }
        lp.setMargins(margin, margin, margin, margin)
        card.layoutParams = lp

        // 圆角深色卡片底
        val radius = Glass.dp(this, if (Glass.liquid) 28 else 22).toFloat()
        card.background = GradientDrawable().apply {
            cornerRadius = radius
            setColor(0xF20D0D0D.toInt())
        }

        // 快门
        val ring = findViewById<View>(R.id.shutter_ring)
        ring.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setStroke(Glass.dp(this@ScanActivity, 5), 0xFFFFFFFF.toInt())
        }
        shutterCore.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Prefs.accent)
        }

        // 结果面板玻璃底
        resultPanel.background = Glass.panelBackground(this, ThemeEngine.isDarkNow(this))

        // 最近照片：圆角 + 白描边
        recentPhoto.clipToOutline = true
        val corner = Glass.dp(this, 16).toFloat()
        recentPhoto.background = GradientDrawable().apply {
            cornerRadius = corner
            setStroke(Glass.dp(this@ScanActivity, 3), 0xFFFFFFFF.toInt())
        }
        recentPhoto.outlineProvider = object : android.view.ViewOutlineProvider() {
            override fun getOutline(view: View, outline: android.graphics.Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, corner)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyCardStyle()
    }

    private fun setupRecentPhoto() {
        if (!RecentPhoto.hasPermission(this)) {
            recentPhoto.visibility = View.GONE
            if (!processTextMode) {
                val perms = if (android.os.Build.VERSION.SDK_INT >= 33) {
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                } else {
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                requestMedia.launch(perms)
            }
            return
        }
        recentUri = RecentPhoto.latestUri(this)
        if (recentUri == null) {
            recentPhoto.visibility = View.GONE
            return
        }
        val thumb = RecentPhoto.loadThumbnail(this, recentUri!!)
        if (thumb != null) {
            recentPhoto.setImageBitmap(thumb)
            recentPhoto.visibility = View.VISIBLE
        }
    }

    private fun setupCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            if (isFinishing || isDestroyed) return@addListener
            val provider = future.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = findViewById<androidx.camera.view.PreviewView>(R.id.preview).surfaceProvider
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                provider.unbindAll()
                camera = provider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture
                )
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.ocr_failed, e.message), Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val capture = imageCapture ?: return
        capture.takePicture(cameraExecutor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                val bmp = try {
                    Bitmaps.fromJpegImageProxy(image)
                } finally {
                    image.close()
                }
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) recognizeBitmap(bmp)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        Toast.makeText(this@ScanActivity, getString(R.string.ocr_failed, exception.message), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun recognizeBitmap(bmp: android.graphics.Bitmap?) {
        if (bmp == null) {
            Toast.makeText(this, getString(R.string.ocr_failed, "decode"), Toast.LENGTH_SHORT).show()
            return
        }
        lastCapture = bmp
        recognizeStartAt = SystemClock.elapsedRealtime()
        busyOverlay.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val raw = OcrManager.recognize(this@ScanActivity, bmp)
                val formatted = TextFormatter.format(raw)
                val cost = SystemClock.elapsedRealtime() - recognizeStartAt
                showResult(formatted, cost)
            } catch (e: Exception) {
                busyOverlay.visibility = View.GONE
                Toast.makeText(this@ScanActivity, getString(R.string.ocr_failed, e.message), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showResult(text: String, costMs: Long) {
        busyOverlay.visibility = View.GONE
        viewfinder.visibility = View.GONE
        bottomPanel.visibility = View.GONE
        resultCard.visibility = View.VISIBLE

        etResult.setText(text)
        etResult.setSelection(text.length)
        tvMeta.text = "${OcrManager.get(this).displayName} · ${costMs}ms"
        updateEngineChip()
    }

    private fun backToCamera() {
        resultCard.visibility = View.GONE
        viewfinder.visibility = View.VISIBLE
        bottomPanel.visibility = View.VISIBLE
        setupCamera()
    }

    private fun onFill() {
        val text = etResult.text.toString()
        if (text.isEmpty()) return

        when {
            processTextMode -> {
                setResult(RESULT_OK, Intent().putExtra(Intent.EXTRA_PROCESS_TEXT, text))
                finish()
            }
            Prefs.fillMode == Prefs.FILL_CLIPBOARD -> {
                copyAndFinish()
            }
            PasteAccessibilityService.isEnabled(this) -> {
                // 先复制到剪贴板，无障碍服务对聚焦输入框执行"全选+粘贴"
                Bitmaps.setClipboardText(this, text)
                PasteAccessibilityService.schedulePaste()
                Toast.makeText(this, R.string.filled, Toast.LENGTH_SHORT).show()
                finish()
            }
            else -> {
                Bitmaps.setClipboardText(this, text)
                Toast.makeText(this, R.string.fill_fallback, Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun copyAndFinish() {
        Bitmaps.setClipboardText(this, etResult.text.toString())
        Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun toggleTorch() {
        torchOn = !torchOn
        camera?.cameraControl?.enableTorch(torchOn)
        btnFlash.alpha = if (torchOn) 1f else 0.6f
    }

    private fun toggleEngine() {
        Prefs.ocrEngine = if (Prefs.ocrEngine == Prefs.ENGINE_MLKIT) Prefs.ENGINE_PADDLE else Prefs.ENGINE_MLKIT
        updateEngineChip()
        Toast.makeText(this, "引擎：${OcrManager.get(this).displayName}", Toast.LENGTH_SHORT).show()
    }

    private fun updateEngineChip() {
        val name = if (Prefs.ocrEngine == Prefs.ENGINE_MLKIT) {
            getString(R.string.engine_mlkit)
        } else {
            getString(R.string.engine_paddle)
        }
        chipEngine.text = "引擎 · $name"
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

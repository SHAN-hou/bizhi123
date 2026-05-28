package com.example.livewallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import androidx.preference.PreferenceManager
import kotlin.math.hypot
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class ParticleWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = ParticleEngine()

    inner class ParticleEngine : WallpaperService.Engine() {

        private val handler = Handler(Looper.getMainLooper())
        private val drawRunnable = Runnable { drawFrame() }
        private val particles = mutableListOf<Particle>()
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)

        private var width = 0
        private var height = 0
        private var visible = false

        // 设置参数
        private var particleCount = 120
        private var colorScheme = "blue"
        private var speedMultiplier = 1.0f
        private var showConnectLines = true
        private var touchInteract = true
        private var touchX = -1f
        private var touchY = -1f
        private var isTouching = false

        private val connectDistance = 150f

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
            loadPreferences()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                loadPreferences()
                handler.post(drawRunnable)
            } else {
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder?, format: Int, width: Int, height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            this.width = width
            this.height = height
            initParticles()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            visible = false
            handler.removeCallbacks(drawRunnable)
        }

        override fun onTouchEvent(event: MotionEvent?) {
            if (!touchInteract || event == null) return
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    touchX = event.x
                    touchY = event.y
                    isTouching = true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isTouching = false
                }
            }
        }

        private fun loadPreferences() {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this@ParticleWallpaperService)
            particleCount = prefs.getString("particle_count", "120")?.toIntOrNull() ?: 120
            colorScheme = prefs.getString("color_scheme", "blue") ?: "blue"
            speedMultiplier = prefs.getString("particle_speed", "1.0")?.toFloatOrNull() ?: 1.0f
            showConnectLines = prefs.getBoolean("connect_lines", true)
            touchInteract = prefs.getBoolean("touch_interact", true)
        }

        private fun initParticles() {
            particles.clear()
            if (width == 0 || height == 0) return
            for (i in 0 until particleCount) {
                particles.add(createParticle())
            }
        }

        private fun createParticle(): Particle {
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2
            val speed = (Random.nextFloat() * 1.5f + 0.3f) * speedMultiplier
            return Particle(
                x = Random.nextFloat() * width,
                y = Random.nextFloat() * height,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                radius = Random.nextFloat() * 3f + 1.5f,
                alpha = Random.nextInt(100, 255)
            )
        }

        private fun drawFrame() {
            if (!visible) return

            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockCanvas()
                if (canvas != null) {
                    drawBackground(canvas)
                    updateParticles()
                    if (showConnectLines) drawConnectLines(canvas)
                    drawParticles(canvas)
                }
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas)
                    } catch (_: Exception) {}
                }
            }

            handler.removeCallbacks(drawRunnable)
            if (visible) {
                handler.postDelayed(drawRunnable, 16L) // ~60fps
            }
        }

        private fun drawBackground(canvas: Canvas) {
            canvas.drawColor(Color.parseColor("#0D1117"))
        }

        private fun getParticleColors(): Pair<Int, Int> {
            return when (colorScheme) {
                "blue" -> Pair(Color.parseColor("#00BFFF"), Color.parseColor("#1E90FF"))
                "green" -> Pair(Color.parseColor("#00FF87"), Color.parseColor("#60EFFF"))
                "purple" -> Pair(Color.parseColor("#BD00FF"), Color.parseColor("#FF6EC7"))
                "red" -> Pair(Color.parseColor("#FF4500"), Color.parseColor("#FF8C00"))
                "gold" -> Pair(Color.parseColor("#FFD700"), Color.parseColor("#FFA500"))
                else -> Pair(Color.parseColor("#00BFFF"), Color.parseColor("#1E90FF"))
            }
        }

        private fun updateParticles() {
            for (p in particles) {
                // 触摸吸引效果
                if (isTouching && touchInteract) {
                    val dx = touchX - p.x
                    val dy = touchY - p.y
                    val dist = hypot(dx, dy)
                    if (dist < 300f && dist > 1f) {
                        val force = 2.0f / dist
                        p.vx += dx * force
                        p.vy += dy * force
                    }
                }

                // 速度衰减（避免无限加速）
                p.vx *= 0.99f
                p.vy *= 0.99f

                // 保持最低速度
                val currentSpeed = hypot(p.vx, p.vy)
                val minSpeed = 0.3f * speedMultiplier
                if (currentSpeed < minSpeed && currentSpeed > 0.01f) {
                    val scale = minSpeed / currentSpeed
                    p.vx *= scale
                    p.vy *= scale
                }

                p.x += p.vx
                p.y += p.vy

                // 边界处理：从对面重新出现
                if (p.x < -10) p.x = width.toFloat() + 10
                if (p.x > width + 10) p.x = -10f
                if (p.y < -10) p.y = height.toFloat() + 10
                if (p.y > height + 10) p.y = -10f
            }
        }

        private fun drawConnectLines(canvas: Canvas) {
            val (color1, _) = getParticleColors()
            for (i in particles.indices) {
                for (j in i + 1 until particles.size) {
                    val dx = particles[i].x - particles[j].x
                    val dy = particles[i].y - particles[j].y
                    val dist = hypot(dx, dy)
                    if (dist < connectDistance) {
                        val alpha = ((1 - dist / connectDistance) * 80).toInt()
                        linePaint.color = color1
                        linePaint.alpha = alpha
                        linePaint.strokeWidth = 1f
                        canvas.drawLine(
                            particles[i].x, particles[i].y,
                            particles[j].x, particles[j].y,
                            linePaint
                        )
                    }
                }
            }
        }

        private fun drawParticles(canvas: Canvas) {
            val (color1, color2) = getParticleColors()
            for ((index, p) in particles.withIndex()) {
                val color = if (index % 2 == 0) color1 else color2
                paint.color = color
                paint.alpha = p.alpha
                paint.style = Paint.Style.FILL

                // 绘制光晕效果
                val glowPaint = Paint(paint)
                glowPaint.alpha = p.alpha / 4
                canvas.drawCircle(p.x, p.y, p.radius * 3, glowPaint)

                // 绘制粒子本体
                canvas.drawCircle(p.x, p.y, p.radius, paint)
            }
        }
    }

    data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var radius: Float,
        var alpha: Int
    )
}

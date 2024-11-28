package com.example.ponggame

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.locks.ReentrantLock

class GameView(context: Context, attrs: AttributeSet?) : SurfaceView(context, attrs), Runnable,SurfaceHolder.Callback {
    private var isPlaying = false
    private lateinit var thread: Thread

    private val ball = RectF()
    private val playerPaddle = RectF()
    private val aiPaddle = RectF()
    private var ballSpeedX = 10f
    private var ballSpeedY = 10f
    private val paint = Paint()
    private val ballPaint = Paint()
    private val paddlePaint = Paint()
    private val linePaint = Paint()
    var player1Score = 0
    var player2Score = 0
    private lateinit var scoreboard: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val lock = ReentrantLock()
    private val AI_SPEED_FACTOR = 0.1f // Adjust this value to control AI speed
    private val AI_PROXIMITY_THRESHOLD = 200f // Adjust this value to control AI proximity
    private val AI_MISS_PROBABILITY = 0.1f // Adjust this value to control AI miss probability


    init {
        resetGame()
        holder.addCallback(this)
        // Ball paint
        ballPaint.color = Color.parseColor("#FF69b4") // Neon Pink

        // Paddle paint
        paddlePaint.color = Color.parseColor("#7FFF00") // Neon Green

        // Dotted line paint
        linePaint.color = Color.BLUE
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeWidth = 10f
        linePaint.pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
        scoreboard = (context as Activity).findViewById(R.id.scoreboard)// initialize scoreboard
        player1Score = 0
        player2Score = 0
        updateScoreboard()

    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        resetGame() // Call resetGame() here
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // No need to handle surface changes for this issue
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // No need to handle surface destruction for this issue
    }
    override fun run() {
        while (isPlaying) {
            update()
            draw()
            controlFPS()
        }
    }

    fun resetGame() {
        // Initialize ball and paddles
        ball.set(500f, 500f, 550f, 550f)

        // Set paddle dimensions
        val paddleWidth = 200f
        val paddleHeight = 50f

        // Player paddle at the bottom
        playerPaddle.set(
            width / 2f - paddleWidth / 2, // Center horizontally
            height - paddleHeight - 20f, // Position above bottom edge
            width / 2f + paddleWidth / 2,
            height - 20f
        )

        // AI paddle at the top
        aiPaddle.set(
            width / 2f - paddleWidth / 2, // Center horizontally
            20f, // Position below top edge
            width / 2f + paddleWidth / 2,
            20f + paddleHeight // Correct bottom position
        )
        // Reset ball position to center or slightly below center
        ball.set(width / 2f - ball.width() / 2, height / 2f - ball.height() / 2 + 50f,
            width / 2f + ball.width() / 2, height / 2f + ball.height() / 2 + 50f)

        // Randomize ball velocity
        ballSpeedX = if (Math.random() < 0.5) -10f else 10f // Randomly choose left or right direction
        ballSpeedY = if (Math.random() < 0.5) -10f else 10f // Randomly choose up or down direction
    }

    private fun update() {
        lock.lock()

        try {
            // Update ball position
            ball.offset(ballSpeedX, ballSpeedY)

            // Ball collision with walls
            if (ball.left < 0 || ball.right > width) {
                ballSpeedX = -ballSpeedX
            }
            if (ball.top < 0) { // Player 2 scores
                player2Score++
                handler.post {
                    updateScoreboard()
                    Toast.makeText(context, "Player 2 Scores!", Toast.LENGTH_SHORT).show()
                    resetGame() // Reset the round
                }
            } else if (ball.bottom > height) { // Player 1 scores
                player1Score++
                handler.post {
                    updateScoreboard()
                    Toast.makeText(context, "Player 1 Scores!", Toast.LENGTH_SHORT).show()
                    resetGame() // Reset the round
                }
            }

            // Ball collision with paddles
            if (RectF.intersects(ball, playerPaddle) || RectF.intersects(ball, aiPaddle)) {
                ballSpeedY = -ballSpeedY

                // Adjust the ball position to prevent sticking
                if (RectF.intersects(ball, playerPaddle)) {
                    ball.offset(0f, playerPaddle.top - ball.bottom) // move the ball above the player paddle.
                } else{
                    ball.offset(0f, aiPaddle.bottom - ball.top) // move the ball below the AI paddle.
                }
            }
            if (player1Score >= 10 || player2Score >= 10) {
                handler.post {
                    val winner = if (player1Score >= 10) "Player 1" else "Player 2"
                    Toast.makeText(context, "$winner Wins!", Toast.LENGTH_LONG).show()
                    resetGame() // Reset the game state
                    (context as MainActivity).showRestartButton() // Show the restart button
                }
                return // Exit the update loop
            }
            // AI paddle logic
            if (ball.centerY() < AI_PROXIMITY_THRESHOLD && Math.random() > AI_MISS_PROBABILITY) { // Check proximity and miss probability
                val paddleCenterX = aiPaddle.centerX()
                val movement = (ball.centerX() - paddleCenterX) * AI_SPEED_FACTOR
                val newPaddleLeft = Math.max(0f, paddleCenterX + movement - aiPaddle.width() / 2)
                val newPaddleRight = Math.min(width.toFloat(), paddleCenterX + movement + aiPaddle.width() / 2)
                aiPaddle.set(newPaddleLeft, aiPaddle.top, newPaddleRight, aiPaddle.bottom)
            }
        } finally {
            lock.unlock()
        }
    }
    fun updateScoreboard() {
        scoreboard.text = String.format(context.getString(R.string.player_1_score), player1Score, player2Score)
    }
    private fun draw() {
        if (holder.surface.isValid) {
            val canvas: Canvas = holder.lockCanvas()

            // Background color
            canvas.drawColor(Color.parseColor("#000000")) // Dark purple

            // Draw paddles and ball with neon colors
            canvas.drawRect(playerPaddle, paddlePaint)
            canvas.drawRect(aiPaddle, paddlePaint)
            canvas.drawOval(ball, ballPaint)

            // Draw dotted line
            val centerY = height / 2f
            canvas.drawLine(0f, centerY, width.toFloat(), centerY, linePaint)

            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun controlFPS() {
        Thread.sleep(16) // ~60 FPS
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                playerPaddle.offsetTo(event.x - playerPaddle.width() / 2, playerPaddle.top)
            }
        }
        return true
    }

    fun startGame() {
        isPlaying = true
        thread = Thread(this)
        thread.start()
    }

    fun stopGame() {
        isPlaying = false
        thread.join()
    }

}

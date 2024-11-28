package com.example.ponggame

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var gameView: GameView
    private lateinit var startButton: Button
    private lateinit var restartButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gameView = GameView(this, null)
        findViewById<FrameLayout>(R.id.game_area).addView(gameView)

        startButton = findViewById(R.id.startButton)
        restartButton = findViewById(R.id.restartButton)

        startButton.setOnClickListener {
            gameView.startGame()
            startButton.visibility = View.GONE // Hide Start button after starting the game
        }

        restartButton.setOnClickListener {
            gameView.resetGame()
            gameView.player1Score = 0
            gameView.player2Score = 0
            gameView.updateScoreboard()
            restartButton.visibility = View.GONE
            startButton.visibility = View.VISIBLE
            gameView.startGame()
        }
    }

    fun showRestartButton() {
        restartButton.visibility = View.VISIBLE
    }

    override fun onPause() {
        super.onPause()
        gameView.stopGame()
    }
}
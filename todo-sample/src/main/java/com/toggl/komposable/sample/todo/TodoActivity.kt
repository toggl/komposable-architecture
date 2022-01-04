package com.toggl.komposable.sample.todo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TodoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TodoScaffold()
            }
        }
    }
}

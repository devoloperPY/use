package com.sigit.modern

import android.os.Bundle
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appClass = Class.forName("App")
        val instance = appClass.getDeclaredConstructor().newInstance() as ComponentActivity
        instance.onCreate(savedInstanceState)
    }
}

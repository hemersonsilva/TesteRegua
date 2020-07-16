package br.com.hemersonsilva.testeregua

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), SimpleRulerView.OnValueChangeListener {
    private lateinit var textView1: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val ruler =(findViewById<View>(R.id.simple_ruler) as SimpleRulerView)
        ruler.onValueChangeListener = this
        textView1 = findViewById<View>(R.id.text1) as TextView
    }

    override fun onChange(view: SimpleRulerView?, position: Int, value: Float) {
        textView1.text = "" + value
    }
}
package com.cui.annotationsproject

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.cui.libannotation.BindView
import com.cui.libapi.InjectHelper

class MainActivity : AppCompatActivity() {

    @BindView(R.id.tv)
    lateinit var textview: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        InjectHelper.inject(this)
        textview.text = "hellow"
    }
}

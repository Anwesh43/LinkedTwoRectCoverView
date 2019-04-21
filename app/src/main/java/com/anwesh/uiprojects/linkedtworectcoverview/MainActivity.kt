package com.anwesh.uiprojects.linkedtworectcoverview

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.anwesh.uiprojects.tworectcoverview.TwoRectCoverView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TwoRectCoverView.create(this)
    }
}

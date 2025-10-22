package com.example.projectchat3.ui.chats

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.projectchat3.R
import android.widget.ImageView

class FullImageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_image)

        val imageUrl = intent.getStringExtra("imageUrl")
        val imageView = findViewById<ImageView>(R.id.fullImageView)

        Glide.with(this)
            .load(imageUrl)
            .into(imageView)

        imageView.setOnClickListener {
            finish() // chạm ra ngoài để thoát
        }
    }
}

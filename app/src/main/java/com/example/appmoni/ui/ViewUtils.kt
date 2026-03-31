package com.example.appmoni.ui

import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.appmoni.R
import java.text.Normalizer


// Hàm tạo Toast
fun Context.showCustomToast(message: String, iconResId: Int) {
    // Lấy giao diện XML vừa tạo ra
    val inflater = LayoutInflater.from(this)
    val layout = inflater.inflate(R.layout.layout_custom_toast, null)

    val textView = layout.findViewById<TextView>(R.id.tv_toast_message)
    textView.text = message

    val imageView = layout.findViewById<ImageView>(R.id.img_toast_icon)
    imageView.setImageResource(iconResId)

    val toast = Toast(this)
    toast.duration = Toast.LENGTH_LONG
    toast.view = layout
    toast.show()
}

// Hàm bỏ dấu tiếng việt
fun String.removeAccents(): String {
    val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
    return "\\p{InCombiningDiacriticalMarks}+".toRegex()
        .replace(temp, "")
        .replace("Đ", "D")
        .replace("đ", "d")
}
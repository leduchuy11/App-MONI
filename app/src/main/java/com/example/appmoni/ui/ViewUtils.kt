package com.example.appmoni.ui

import android.content.Context
import android.text.Editable
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.appmoni.R
import java.text.Normalizer
import android.text.TextWatcher
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class ToastType {
    SUCCESS, WARNING, ERROR
}

//Hàm tạo toast phiên bản 2
fun Context.showToast(message: String, type: ToastType) {
    val inflater = LayoutInflater.from(this)

    val layoutRes = when (type) {
        ToastType.SUCCESS -> R.layout.layout_toast_success
        ToastType.WARNING -> R.layout.layout_toast_warning
        ToastType.ERROR   -> R.layout.layout_toast_failure
    }

    val view = inflater.inflate(layoutRes, null)

    val tvMessage = view.findViewById<TextView>(R.id.tv_toast_message)
    tvMessage.text = message

    Toast(this).apply {
        duration = Toast.LENGTH_SHORT
        this.view = view
        show()
    }
}

// Hàm tạo Toast phiên bản 1
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

// Hàm mở rộng cho EditText: Tự động format số tiền có dấu chấm khi người dùng gõ phím
fun EditText.addCurrencyFormatter() {
    this.addTextChangedListener(object : TextWatcher {
        private var current = ""

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            if (s.toString() != current) {
                this@addCurrencyFormatter.removeTextChangedListener(this)

                val cleanString = s.toString().replace(".", "")

                if (cleanString.isNotEmpty()) {
                    try {
                        val parsed = cleanString.toLong()
                        val symbols = DecimalFormatSymbols(Locale.getDefault())
                        symbols.groupingSeparator = '.'
                        val formatter = DecimalFormat("#,###", symbols)
                        val formatted = formatter.format(parsed)

                        current = formatted
                        this@addCurrencyFormatter.setText(formatted)
                        this@addCurrencyFormatter.setSelection(formatted.length)
                    } catch (e: NumberFormatException) {
                        // Bỏ qua nếu số quá giới hạn của Long
                    }
                } else {
                    current = ""
                    this@addCurrencyFormatter.setText("")
                }

                this@addCurrencyFormatter.addTextChangedListener(this)
            }
        }
    })
}


// Hàm mở rộng cho String: Xóa dấu chấm và chuyển chuỗi tiền tệ về kiểu Long
fun String.parseCurrencyValue(): Long {
    val cleanString = this.replace(".", "")
    return cleanString.toLongOrNull() ?: 0L
}
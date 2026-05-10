package com.example.appmoni.ui.main.home.manageLimit.detail

import android.content.Context
import android.widget.TextView
import com.example.appmoni.R
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF

class CustomMarkerView(
    context: Context,
    layoutResource: Int,
    private val xLabels: List<String>
) : MarkerView(context, layoutResource) {

    private val tvContent: TextView = findViewById(R.id.tv_marker_content)

    // Hàm này chạy mỗi khi chạm vào 1 điểm trên biểu đồ
    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e != null) {
            val index = e.x.toInt()

            val date = if (index >= 0 && index < xLabels.size) xLabels[index] else ""

            val amount = "%,d đ".format(e.y.toLong()).replace(',', '.')

            tvContent.text = "$date\n$amount"
        }
        super.refreshContent(e, highlight)
    }

    // Hàm này để chỉnh vị trí bóng thoại: Nằm ngay trên đầu điểm chạm và căn giữa
    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat() - 15f)
    }
}
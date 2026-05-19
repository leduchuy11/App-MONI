package com.example.appmoni.ui.main.report.expenseAnalysis


import android.content.Context
import android.widget.TextView
import com.example.appmoni.R
import com.example.appmoni.data.model.analysis.AnalysisItem
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class AnalysisCustomMarkerView(
    context: Context,
    layoutResource: Int,
    private val dataList: List<AnalysisItem>
) : MarkerView(context, layoutResource) {

    private val tvDate: TextView = findViewById(R.id.tv_marker_date)
    private val tvAmount: TextView = findViewById(R.id.tv_marker_amount)

    private val moneyFormatter =
        DecimalFormat("#,###", DecimalFormatSymbols(Locale.getDefault()).apply {
            groupingSeparator = '.'
        })

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e != null) {
            val index = e.x.toInt()
            if (index >= 0 && index < dataList.size) {
                val item = dataList[index]
                tvDate.text = item.timeLabel
                tvAmount.text = "${moneyFormatter.format(item.amount)} đ"
            }
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat() - 10f)
    }
}
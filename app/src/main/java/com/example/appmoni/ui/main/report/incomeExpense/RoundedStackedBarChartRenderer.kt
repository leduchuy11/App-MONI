package com.example.appmoni.ui.main.report.incomeExpense

import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.renderer.BarChartRenderer
import com.github.mikephil.charting.utils.ViewPortHandler

class RoundedStackedBarChartRenderer(
    chart: BarDataProvider,
    animator: ChartAnimator,
    viewPortHandler: ViewPortHandler,
    private val mRadius: Float,
    private val incomeColor: Int,
    private val expenseColor: Int
) : BarChartRenderer(chart, animator, viewPortHandler) {

    override fun drawDataSet(c: Canvas, dataSet: IBarDataSet, index: Int) {
        val trans = mChart.getTransformer(dataSet.axisDependency)

        mBarBorderPaint.color = dataSet.barBorderColor
        mBarBorderPaint.strokeWidth = com.github.mikephil.charting.utils.Utils.convertDpToPixel(dataSet.barBorderWidth)

        val drawBorder = dataSet.barBorderWidth > 0f

        val phaseX = mAnimator.phaseX
        val phaseY = mAnimator.phaseY

        if (mBarBuffers != null && mBarBuffers.size > index) {
            val buffer = mBarBuffers[index]
            buffer.setPhases(phaseX, phaseY)
            buffer.setDataSet(index)
            buffer.setInverted(mChart.isInverted(dataSet.axisDependency))
            buffer.setBarWidth(mChart.barData.barWidth)

            buffer.feed(dataSet)

            trans.pointValuesToPixel(buffer.buffer)

            val isCustomColors = dataSet.colors.size > 1

            var j = 0
            while (j < buffer.size()) {
                if (!mViewPortHandler.isInBoundsLeft(buffer.buffer[j + 2])) {
                    j += 4
                    continue
                }

                if (!mViewPortHandler.isInBoundsRight(buffer.buffer[j]))
                    break

                // Lấy tọa độ từng cục của thanh Bar
                val left = buffer.buffer[j]
                val top = buffer.buffer[j + 1]
                val right = buffer.buffer[j + 2]
                val bottom = buffer.buffer[j + 3]

                if (isCustomColors) {
                    mRenderPaint.color = dataSet.getColor(j / 4)
                } else {
                    mRenderPaint.color = dataSet.color
                }

                val path = Path()

                // Logic bo góc riêng cho từng màu (Thu và Chi)
                val radii = when (mRenderPaint.color) {
                    incomeColor -> {
                        // Màu xanh (Thu) -> Bo 2 góc phía TRÊN
                        floatArrayOf(mRadius, mRadius, mRadius, mRadius, 0f, 0f, 0f, 0f)
                    }
                    expenseColor -> {
                        // Màu đỏ (Chi) -> Bo 2 góc phía DƯỚI
                        floatArrayOf(0f, 0f, 0f, 0f, mRadius, mRadius, mRadius, mRadius)
                    }
                    else -> {
                        // Màu khác (nếu có) -> Không bo góc
                        floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
                    }
                }

                path.addRoundRect(RectF(left, top, right, bottom), radii, Path.Direction.CW)
                c.drawPath(path, mRenderPaint)

                if (drawBorder) {
                    c.drawPath(path, mBarBorderPaint)
                }

                j += 4
            }
        }
    }
}
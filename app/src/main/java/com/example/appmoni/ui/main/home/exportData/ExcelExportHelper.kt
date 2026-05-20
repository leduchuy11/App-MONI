package com.example.appmoni.ui.main.home.exportData

import android.content.Context
import com.example.appmoni.data.model.transaction.TransactionItem
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

object ExcelExportHelper {

    fun exportToExcel(
        context: Context,
        transactions: List<TransactionItem>,
        startDate: Long,
        endDate: Long
    ): File? {
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val strStart = sdf.format(Date(startDate))
            val strEnd = sdf.format(Date(endDate))

            val fileName = "Bao_Cao_Thu_Chi_Moni.csv"
            val file = File(context.cacheDir, fileName)

            val moneyFormatter = DecimalFormat("#,###").apply {
                decimalFormatSymbols = DecimalFormatSymbols(Locale.getDefault()).apply {
                    groupingSeparator = '.'
                }
            }

            // Lọc dữ liệu và tính tổng
            val expenses = transactions.filter { it.type == "expense" }
            val incomes = transactions.filter { it.type == "income" }

            val totalExpense = expenses.sumOf { it.amount }
            val totalIncome = incomes.sumOf { it.amount }

            val bom = "\uFEFF"
            val csvContent = StringBuilder()
            csvContent.append(bom)

            csvContent.append(",,BẢNG KÊ THU CHI\n")
            csvContent.append(",     Từ ngày $strStart đến ngày $strEnd\n")
            csvContent.append("\n")

            csvContent.append(",,,,Tổng thu,${moneyFormatter.format(totalIncome)}\n")
            csvContent.append(",,,,Tổng chi,${moneyFormatter.format(totalExpense)}\n")
            csvContent.append("\n")

            // Bảng chi tiêu
            if (expenses.isNotEmpty()) {
                csvContent.append("\"BẢNG CHI TIÊU\"\n")
                csvContent.append("STT,Ngày,Số tiền,Hạng mục,Tài khoản,Ghi chú\n")
                expenses.forEachIndexed { index, tx ->
                    csvContent.append(
                        "${index + 1}," +
                                "'${sdf.format(Date(tx.dateInMillis))}," +
                                "'${moneyFormatter.format(tx.amount)}," +
                                "\"${tx.categoryName}\"," +
                                "\"${tx.walletName}\"," +
                                "\"${tx.note.replace(",", " ")}\"\n"
                    )
                }
                csvContent.append("\n")
            }

            // Bảng thu nhập
            if (incomes.isNotEmpty()) {
                csvContent.append("\"BẢNG THU NHẬP\"\n")
                csvContent.append("STT,Ngày,Số tiền,Hạng mục,Tài khoản,Ghi chú\n")
                incomes.forEachIndexed { index, tx ->
                    csvContent.append(
                        "${index + 1}," +
                                "'${sdf.format(Date(tx.dateInMillis))}," +
                                "'${moneyFormatter.format(tx.amount)}," +
                                "\"${tx.categoryName}\"," +
                                "\"${tx.walletName}\"," +
                                "\"${tx.note.replace(",", " ")}\"\n"
                    )
                }
            }

            FileOutputStream(file).use { it.write(csvContent.toString().toByteArray()) }
            file
        } catch (e: Exception) {
            null
        }
    }
}
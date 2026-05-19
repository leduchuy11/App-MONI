package com.example.appmoni.data.repository.exchangeRate

import com.example.appmoni.R
import com.example.appmoni.data.model.exchangeRate.CurrencyItem

object CurrencyHelper {
    fun getSupportedCurrencies(): List<CurrencyItem> {
        return listOf(
            CurrencyItem("đ", "VND", "Việt Nam Đồng", R.drawable.ic_flag_vn),
            CurrencyItem("$", "USD", "United States Dollar", R.drawable.ic_flag_us),
            CurrencyItem("¥", "CNY", "Chinese Yuan", R.drawable.ic_flag_cn),
            CurrencyItem("€", "EUR", "Euro", R.drawable.ic_flag_eu),
            CurrencyItem("£", "GBP", "British Pound", R.drawable.ic_flag_uk),
            CurrencyItem("¥", "JPY", "Japanese Yen", R.drawable.ic_flag_jp),
            CurrencyItem("Fr.", "CHF", "Swiss Franc", R.drawable.ic_flag_ch),
            CurrencyItem("₩", "KRW", "South Korean Won", R.drawable.ic_flag_kr),
            CurrencyItem("฿", "THB", "Thai Baht", R.drawable.ic_flag_th),
            CurrencyItem("S$", "SGD", "Singapore Dollar", R.drawable.ic_flag_sg),
            CurrencyItem("RM", "MYR", "Malaysian Ringgit", R.drawable.ic_flag_my),
            CurrencyItem("Rp", "IDR", "Indonesian Rupiah", R.drawable.ic_flag_id),
            CurrencyItem("₱", "PHP", "Philippine Peso", R.drawable.ic_flag_ph),
            CurrencyItem("₭", "LAK", "Lao Kip", R.drawable.ic_flag_la),
            CurrencyItem("៛", "KHR", "Cambodian Riel", R.drawable.ic_flag_kh),
            CurrencyItem("K", "MMK", "Myanmar Kyat", R.drawable.ic_flag_mm),
            CurrencyItem("NT$", "TWD", "New Taiwan Dollar", R.drawable.ic_flag_tw),
            CurrencyItem("HK$", "HKD", "Hong Kong Dollar", R.drawable.ic_flag_hk),
            CurrencyItem("$", "MOP", "Macanese Pataca", R.drawable.ic_flag_mo),
            CurrencyItem("₹", "INR", "Indian Rupee", R.drawable.ic_flag_in),
            CurrencyItem("₨", "PKR", "Pakistani Rupee", R.drawable.ic_flag_pk),
            CurrencyItem("৳", "BDT", "Bangladeshi Taka", R.drawable.ic_flag_bd),
            CurrencyItem("A$", "AUD", "Australian Dollar", R.drawable.ic_flag_au),
            CurrencyItem("NZ$", "NZD", "New Zealand Dollar", R.drawable.ic_flag_nz),
            CurrencyItem("C$", "CAD", "Canadian Dollar", R.drawable.ic_flag_ca),
            CurrencyItem("$", "MXN", "Mexican Peso", R.drawable.ic_flag_mx),
            CurrencyItem("R$", "BRL", "Brazilian Real", R.drawable.ic_flag_br),
            CurrencyItem("AR$", "ARS", "Argentine Peso", R.drawable.ic_flag_ar),
            CurrencyItem("$", "CLP", "Chilean Peso", R.drawable.ic_flag_cl),
            CurrencyItem("$", "COP", "Colombian Peso", R.drawable.ic_flag_co),
            CurrencyItem("S/", "PEN", "Peruvian Sol", R.drawable.ic_flag_pe),
            CurrencyItem("₽", "RUB", "Russian Ruble", R.drawable.ic_flag_ru),
            CurrencyItem("kr", "SEK", "Swedish Krona", R.drawable.ic_flag_se),
            CurrencyItem("kr", "NOK", "Norwegian Krone", R.drawable.ic_flag_no),
            CurrencyItem("kr", "DKK", "Danish Krone", R.drawable.ic_flag_dk),
            CurrencyItem("zł", "PLN", "Polish Zloty", R.drawable.ic_flag_pl),
            CurrencyItem("Kč", "CZK", "Czech Koruna", R.drawable.ic_flag_cz),
            CurrencyItem("Ft", "HUF", "Hungarian Forint", R.drawable.ic_flag_hu),
            CurrencyItem("lei", "RON", "Romanian Leu", R.drawable.ic_flag_ro),
            CurrencyItem("₺", "TRY", "Turkish Lira", R.drawable.ic_flag_tr),
            CurrencyItem("د.إ", "AED", "United Arab Emirates Dirham", R.drawable.ic_flag_ae),
            CurrencyItem("﷼", "SAR", "Saudi Riyal", R.drawable.ic_flag_sa),
            CurrencyItem("₪", "ILS", "Israeli New Shekel", R.drawable.ic_flag_il),
            CurrencyItem("E£", "EGP", "Egyptian Pound", R.drawable.ic_flag_eg),
            CurrencyItem("R", "ZAR", "South African Rand", R.drawable.ic_flag_za),
            CurrencyItem("₦", "NGN", "Nigerian Naira", R.drawable.ic_flag_ng),
            CurrencyItem("KSh", "KES", "Kenyan Shilling", R.drawable.ic_flag_ke)
        )
    }
}
package com.example.appmoni.data.repository.wallet

import com.example.appmoni.data.model.wallet.FinancialInstitution

class InstitutionRepository {

    // --- DỮ LIỆU NGÂN HÀNG THẬT TẾ ---
    fun getRealBanks(): List<FinancialInstitution> {
        return listOf(
            FinancialInstitution("agribank", "ic_bank_agribank", "Agribank", "Ngân hàng Nông nghiệp và Phát triển nông thôn Việt Nam"),
            FinancialInstitution("abbank", "ic_bank_abbank", "ABBank", "Ngân hàng TMCP An Bình"),
            FinancialInstitution("acb", "ic_bank_acb", "ACB", "Ngân hàng TMCP Á Châu"),
            FinancialInstitution("acs", "ic_bank_acs", "ACS", "Công ty TNHH Thương mại ACS Việt Nam"),
            FinancialInstitution("anz", "ic_bank_anz", "ANZ", "Ngân hàng TNHH MTV ANZ Việt Nam"),
            FinancialInstitution("baoviet", "ic_bank_baoviet", "BAOVIET Bank", "Ngân hàng TMCP Bảo Việt"),
            FinancialInstitution("bidc", "ic_bank_bidc", "BIDC", "Ngân hàng Đầu tư và Phát triển Campuchia"),
            FinancialInstitution("bidv", "ic_bank_bidv", "BIDV", "Ngân hàng TMCP Đầu tư và Phát triển Việt Nam"),
            FinancialInstitution("bvbank", "ic_bank_bvbank", "BVBank", "Ngân hàng TMCP Bản Việt"),
            FinancialInstitution("bac_a", "ic_bank_bac_a", "Bac A Bank", "Ngân hàng TMCP Bắc Á"),
            FinancialInstitution("cake", "ic_bank_cake", "CAKE", "Ngân hàng Số CAKE by VPBank"),
            FinancialInstitution("cba", "ic_bank_cba", "CBA", "Văn phòng Đại diện Commonwealth Bank Of Australia"),
            FinancialInstitution("cbank", "ic_bank_cbank", "CBANK", "Ngân hàng Xây dựng"),
            FinancialInstitution("cimb", "ic_bank_cimb", "CIMB Việt Nam", "Ngân hàng TNHH MTV CIMB Việt Nam"),
            FinancialInstitution("citibank", "ic_bank_citibank", "Citibank", "Ngân hàng Citibank Việt Nam"),
            FinancialInstitution("coopbank", "ic_bank_coopbank", "Co-opbank", "Ngân hàng Hợp tác xã Việt Nam"),
            FinancialInstitution("daia", "ic_bank_daia", "DaiA Bank", "Ngân hàng TMCP Đại Á"),
            FinancialInstitution("donga", "ic_bank_donga", "DongA Bank", "Ngân hàng TMCP Đông Á"),
            FinancialInstitution("evn", "ic_bank_evn", "EVN Finance", "Công ty Tài chính Cổ phần Điện lực"),
            FinancialInstitution("eximbank", "ic_bank_eximbank", "EximBank", "Ngân hàng TMCP Xuất nhập khẩu Việt Nam"),
            FinancialInstitution("fecredit", "ic_bank_fecredit", "FE Credit", "Công ty Tài chính TNHH MTV Ngân Hàng Việt Nam"),

            FinancialInstitution("firstbank", "ic_bank_firstbank", "First Bank", "Ngân hàng First Commercial Bank"),
            FinancialInstitution("gpbank", "ic_bank_gpbank", "GPBank", "Ngân hàng TM TNHH MTV Dầu khí Toàn Cầu"),
            FinancialInstitution("hdsaison", "ic_bank_hdsaison", "HD SAISON", "Công ty Tài chính TNHH HD SAISON"),
            FinancialInstitution("hdbank", "ic_bank_hdbank", "HDBank", "Ngân hàng TMCP Phát triển Tp. Hồ Chí Minh"),
            FinancialInstitution("hsbc", "ic_bank_hsbc", "HSBC", "Ngân hàng TNHH MTV HSBC Việt Nam"),
            FinancialInstitution("homecredit", "ic_bank_homecredit", "Home Credit", "Công ty Tài chính TNHH MTV Home Credit Việt Nam"),
            FinancialInstitution("hongleong", "ic_bank_hongleong", "HongLeong Bank", "Ngân hàng Hong Leong Berhad"),
            FinancialInstitution("indovina", "ic_bank_indovina", "Indovina Bank", "Ngân hàng TNHH Indovina"),
            FinancialInstitution("jaccs", "ic_bank_jaccs", "JACCS", "Công ty Tài chính TNHH MTV Quốc tế Việt Nam JACCS"),
            FinancialInstitution("kienlong", "ic_bank_kienlong", "KienLong Bank", "Ngân hàng TMCP Kiên Long"),
            FinancialInstitution("lienvietpostbank", "ic_bank_lienvietpostbank", "LienVietPostBank", "Ngân hàng Bưu điện Liên Việt"),
            FinancialInstitution("lottefinance", "ic_bank_lottefinance", "LotteFinance Việt Nam", "Công ty Tài chính TNHH MTV LOTTE Việt Nam"),
            FinancialInstitution("mb", "ic_bank_mb", "MB", "Ngân hàng TMCP Quân đội"),
            FinancialInstitution("mdbank", "ic_bank_mdbank", "MDBank", "Ngân hàng TMCP Phát triển Mê Kông"),
            FinancialInstitution("mhb", "ic_bank_mhb", "MHB", "Ngân hàng TMCP Phát triển nhà Đồng bằng sông Cửu Long"),
            FinancialInstitution("msb", "ic_bank_msb", "MSB", "Ngân hàng TMCP Hàng hải Việt Nam"),
            FinancialInstitution("mcredit", "ic_bank_mcredit", "Mcredit", "Công ty Tài chính TNHH MB SHINSEI"),
            FinancialInstitution("miraeasset", "ic_bank_miraeasset", "Mirae Asset", "Công ty Tài chính TNHH MTV Mirae Asset Finance"),
            FinancialInstitution("ncb", "ic_bank_ncb", "NCB", "Ngân hàng Quốc dân"),
            FinancialInstitution("nam_a", "ic_bank_nam_a", "Nam A Bank", "Ngân hàng TMCP Nam Á"),
            FinancialInstitution("ocb", "ic_bank_ocb", "OCB", "Ngân hàng TMCP Phương Đông"),

            FinancialInstitution("oceanbank", "ic_bank_oceanbank", "OceanBank", "Ngân hàng TM TNHH MTV Đại Dương"),
            FinancialInstitution("pgbank", "ic_bank_pgbank", "PG Bank", "Ngân hàng TMCP Xăng dầu Petrolimex"),
            FinancialInstitution("pvcombank", "ic_bank_pvcombank", "PVcomBank", "Ngân hàng TMCP Đại Chúng Việt Nam"),
            FinancialInstitution("publicbank", "ic_bank_publicbank", "Public Bank Việt Nam", "Ngân hàng TNHH MTV Public Việt Nam"),
            FinancialInstitution("scb", "ic_bank_scb", "SCB", "Ngân hàng TMCP Sài Gòn"),
            FinancialInstitution("shb", "ic_bank_shb", "SHB", "Ngân hàng TMCP Sài Gòn – Hà Nội"),
            FinancialInstitution("sacombank", "ic_bank_sacombank", "Sacombank", "Ngân hàng TMCP Sài Gòn Thương Tín"),
            FinancialInstitution("saigonbank", "ic_bank_saigonbank", "Saigonbank", "Ngân hàng TMCP Sài Gòn Công Thương"),
            FinancialInstitution("seabank", "ic_bank_seabank", "SeABank", "Ngân hàng TMCP Đông Nam Á"),
            FinancialInstitution("shinhanbank", "ic_bank_shinhanbank", "Shinhan Bank", "Ngân hàng TNHH MTV Shinhan Việt Nam"),
            FinancialInstitution("shinhanfinance", "ic_bank_shinhanfinance", "Shinhan Finance", "Công ty Tài chính TNHH MTV Shinhan Việt Nam"),
            FinancialInstitution("standardchartered", "ic_bank_standardchartered", "Standard Chartered", "Ngân hàng TNHH MTV Standard Chartered Việt Nam"),
            FinancialInstitution("timo", "ic_bank_timo", "TIMO", "Ngân hàng Số Timo"),
            FinancialInstitution("tpbank", "ic_bank_tpbank", "TPBank", "Ngân hàng TMCP Tiên Phong"),
            FinancialInstitution("techcombank", "ic_bank_techcombank", "Techcombank", "Ngân hàng TMCP Kỹ Thương Việt Nam"),
            FinancialInstitution("uob", "ic_bank_uob", "UOB", "Ngân hàng TNHH MTV United Overseas Bank Việt Nam"),
            FinancialInstitution("vbsp", "ic_bank_vbsp", "VBSP", "Ngân hàng Chính sách Xã hội Việt Nam"),
            FinancialInstitution("vdb", "ic_bank_vdb", "VDB", "Ngân hàng Phát triển Việt Nam"),
            FinancialInstitution("vib", "ic_bank_vib", "VIB", "Ngân hàng TMCP Quốc Tế Việt Nam"),
            FinancialInstitution("vpbank", "ic_bank_vpbank", "VPBank", "Ngân hàng TMCP Việt Nam Thịnh Vượng"),
            FinancialInstitution("vrb", "ic_bank_vrb", "VRB", "Ngân hàng Liên doanh Việt – Nga"),
            FinancialInstitution("vietabank", "ic_bank_vietabank", "VietABank", "Ngân hàng TMCP Việt Á"),
            FinancialInstitution("vietcredit", "ic_bank_vietcredit", "VietCredit", "Công ty Tài chính cổ phần Tín Việt"),
            FinancialInstitution("vietbank", "ic_bank_vietbank", "Vietbank", "Ngân hàng TMCP Việt Nam Thương Tín"),
            FinancialInstitution("vietcombank", "ic_bank_vietcombank", "Vietcombank", "Ngân hàng TMCP Ngoại thương Việt Nam"),
            FinancialInstitution("vietinbank", "ic_bank_vietinbank", "Vietinbank", "Ngân hàng TMCP Công Thương Việt Nam"),
            FinancialInstitution("vinasiambank", "ic_bank_vinasiambank", "Vinasiam Bank", "Ngân Hàng Liên doanh Việt - Thái"),
            FinancialInstitution("wooribank", "ic_bank_wooribank", "Woori Bank", "Ngân hàng TNHH MTV Woori Việt Nam")
        )
    }

    fun getRealEWallets(): List<FinancialInstitution> {
        return listOf(
            FinancialInstitution("moca", "ic_wallet_moca", "Moca", "Ví điện tử Moca"),
            FinancialInstitution("momo", "ic_wallet_momo", "Momo", "Ví điện tử Momo"),
            FinancialInstitution("shopeepay", "ic_wallet_shopeepay", "ShopeePay", "Ví điện tử ShopeePay"),
            FinancialInstitution("viettelpay", "ic_wallet_viettelpay", "ViettelPay", "Ví điện tử ViettelPay"),
            FinancialInstitution("vnpay", "ic_wallet_vnpay", "VnPay", "Ví điện tử VnPay"),
            FinancialInstitution("zalopay", "ic_wallet_zalopay", "ZaloPay", "Ví điện tử ZaloPay")
        )
    }
}
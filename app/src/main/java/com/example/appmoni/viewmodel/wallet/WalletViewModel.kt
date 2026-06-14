package com.example.appmoni.viewmodel.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.appmoni.data.model.wallet.FinancialInstitution
import com.example.appmoni.data.model.wallet.WalletItem
import com.example.appmoni.data.repository.wallet.InstitutionRepository
import com.example.appmoni.data.repository.wallet.WalletRepository

class WalletViewModel : ViewModel() {
    private val repository = WalletRepository()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    private val _actionSuccess = MutableLiveData<String>()
    val actionSuccess: LiveData<String> get() = _actionSuccess

    // Biến lưu trữ danh sách ví
    private val _walletList = MutableLiveData<List<WalletItem>>()
    val walletList: LiveData<List<WalletItem>> get() = _walletList

    private val institutionRepository = InstitutionRepository()

    private val _institutionList = MutableLiveData<List<FinancialInstitution>>()
    val institutionList: LiveData<List<FinancialInstitution>> get() = _institutionList


    // --- LƯU TRẠNG THÁI MÀN HÌNH THÊM VÍ ---

    // 1. Lưu loại tài khoản (cash, bank, ewallet) và thông tin hiển thị của nó
    private val _selectedAccountType = MutableLiveData<String>("cash")
    val selectedAccountType: LiveData<String> get() = _selectedAccountType

    private val _accountTypeName = MutableLiveData<String>("Tiền mặt")
    val accountTypeName: LiveData<String> get() = _accountTypeName

    private val _accountTypeIcon = MutableLiveData<String>("ic_money")
    val accountTypeIcon: LiveData<String> get() = _accountTypeIcon

    // 2. Lưu Tổ chức tài chính (Ngân hàng / Ví điện tử) được chọn từ màn hình danh sách
    private val _selectedInstitutionName = MutableLiveData<String>("")
    val selectedInstitutionName: LiveData<String> get() = _selectedInstitutionName

    private val _selectedInstitutionIcon = MutableLiveData<String>("")
    val selectedInstitutionIcon: LiveData<String> get() = _selectedInstitutionIcon

    // GỌI REPOSITORY LẤY DATA
    fun loadWallets(userId: String, type: String = "spending") {
        _isLoading.value = true
        repository.listenToWalletsByType(userId, type,
            onResult = { list ->
                _walletList.value = list
                _isLoading.value = false
            },
            onError = { error ->
                _errorMessage.value = error.message
                _isLoading.value = false
            }
        )
    }



    // THÊM VÍ MỚI
    fun addWallet(userId: String, wallet: WalletItem) {
        _isLoading.value = true

        repository.addWallet(userId, wallet)

        // Phản hồi UI ngay lập tức
        _isLoading.value = false
        _actionSuccess.value = "Thêm tài khoản thành công!"
    }

    //Cập nhật Loại tài khoản (Tiền mặt / Ngân hàng / Ví điện tử) từ BottomSheet.
    fun updateAccountType(type: String, typeName: String, typeIcon: String) {
        _selectedAccountType.value = type
        _accountTypeName.value = typeName
        _accountTypeIcon.value = typeIcon
        _selectedInstitutionName.value = ""
        _selectedInstitutionIcon.value = ""
    }


    fun updateInstitutionInfo(name: String, icon: String) {
        _selectedInstitutionName.value = name
        _selectedInstitutionIcon.value = icon
    }

    fun loadInstitutions(type: String) {
        _institutionList.value = if (type == "bank") {
            institutionRepository.getRealBanks()
        } else {
            institutionRepository.getRealEWallets()
        }
    }

    // Hàm sửa 1 ví
    fun updateWalletInfo(userId: String, updatedWallet: WalletItem) {
        _isLoading.value = true

        repository.updateWallet(userId, updatedWallet)

        _isLoading.value = false
        _actionSuccess.value = "Cập nhật tài khoản thành công!"
    }

    // Hàm ngưng sử dụng 1 ví
    fun archiveWallet(userId: String, walletId: String) {
        repository.archiveWallet(userId, walletId)

        _actionSuccess.value = "Đã ngừng sử dụng tài khoản!"
    }

    // Hàm xóa ví nếu ví chưa có giao dịch nào
    fun deleteWalletSafely(userId: String, walletId: String) {
        _isLoading.value = true

        repository.checkHasTransactions(userId, walletId,
            onResult = { hasTransactions ->
                if (!hasTransactions) {
                    repository.deleteWallet(userId, walletId)
                        .addOnFailureListener {
                        }

                    _isLoading.value = false
                    _actionSuccess.value = "Đã xóa tài khoản!"

                } else {
                    _isLoading.value = false
                    _errorMessage.value = "Không thể xóa vì đã có giao dịch. Hãy chuyển sang Ngưng sử dụng hoặc xóa hết lịch sử các giao dịch trước để tránh sai lệch dữ liệu."
                }
            },
            onError = { e ->
                _isLoading.value = false
                _errorMessage.value = "Lỗi kiểm tra dữ liệu: ${e.message}"
            }
        )
    }

    fun clearActionSuccess() {
        _actionSuccess.value = ""
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
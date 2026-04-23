package com.example.appmoni.ui.main.wallet.manageSpending.addEditWallet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoni.R
import com.example.appmoni.data.model.wallet.FinancialInstitution

class InstitutionAdapter(
    private var institutionList: List<FinancialInstitution>,
    private val onItemClick: (FinancialInstitution) -> Unit
) : RecyclerView.Adapter<InstitutionAdapter.InstitutionViewHolder>() {

    class InstitutionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivLogo: ImageView = itemView.findViewById(R.id.iv_logo)
        val tvShortName: TextView = itemView.findViewById(R.id.tv_short_name)
        val tvFullName: TextView = itemView.findViewById(R.id.tv_full_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InstitutionViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_institution, parent, false)
        return InstitutionViewHolder(view)
    }

    override fun onBindViewHolder(holder: InstitutionViewHolder, position: Int) {
        val currentItem = institutionList[position]

        holder.tvShortName.text = currentItem.shortName
        holder.tvFullName.text = currentItem.fullName

        // Đổ dữ liệu icon (Lấy ID của drawable từ tên file string)
        val context = holder.itemView.context
        val iconResId =
            context.resources.getIdentifier(currentItem.iconName, "drawable", context.packageName)
        if (iconResId != 0) {
            holder.ivLogo.setImageResource(iconResId)
        } else {
            // Nếu không tìm thấy icon, set một icon mặc định
            holder.ivLogo.setImageResource(R.drawable.avatar_app)
        }

        // Bắt sự kiện click vào nguyên cả cái dòng đó
        holder.itemView.setOnClickListener {
            onItemClick(currentItem)
        }
    }

    override fun getItemCount(): Int {
        return institutionList.size
    }

    // Hàm này làm tính năng TÌM KIẾM (Search)
    fun updateData(newList: List<FinancialInstitution>) {
        institutionList = newList
        notifyDataSetChanged() // Cập nhật lại toàn bộ danh sách
    }
}
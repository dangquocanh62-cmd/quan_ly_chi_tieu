package com.quanlychitieuapp.utils

object AmountInputFilter {
    const val MAX_AMOUNT_DIGITS = 12

    /**
     * Lọc input string để chỉ giữ lại các ký tự số nguyên (0-9).
     * Loại bỏ tất cả các ký tự khác (chữ cái, dấu chấm, phẩy, v.v.).
     * Nếu chuỗi vượt quá maxLength, sẽ bị cắt bớt (truncate).
     */
    fun filter(input: String, maxLength: Int = MAX_AMOUNT_DIGITS): String {
        val filtered = input.filter { it in '0'..'9' }
        return if (filtered.length > maxLength) {
            filtered.substring(0, maxLength)
        } else {
            filtered
        }
    }
}

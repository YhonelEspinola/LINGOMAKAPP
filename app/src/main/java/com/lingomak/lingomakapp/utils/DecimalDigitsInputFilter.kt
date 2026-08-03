package com.lingomak.lingomakapp.utils

import android.text.InputFilter
import android.text.Spanned
import java.util.regex.Pattern

class DecimalDigitsInputFilter(maxDecimales: Int) : InputFilter {
    private val mPattern: Pattern = Pattern.compile("[0-9]{0,9}+(\\.[0-9]{0,$maxDecimales})?")

    override fun filter(
        source: CharSequence?,
        start: Int,
        end: Int,
        dest: Spanned?,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        val replacement = source?.subSequence(start, end).toString()
        val newVal = dest?.subSequence(0, dstart).toString() + replacement + dest?.subSequence(dend, dest.length).toString()
        
        val matcher = mPattern.matcher(newVal)
        if (matcher.matches()) return null
        
        return ""
    }
}

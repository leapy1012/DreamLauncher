package com.android.customize.overlay.ui.plus.view

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.android.customize.common.extension.color
import com.android.customize.common.extension.drawable
import com.android.customize.common.extension.px
import com.android.customize.common.extension.pxf
import com.android.launcher3.ExtendedEditText
import com.android.launcher3.R

@SuppressLint("NewApi")
class SearchBarContainer @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    val searchBar by lazy {
        ExtendedEditText(context).apply {
            id = generateViewId()
            gravity = Gravity.CENTER
            setSingleLine()
            setTextColor(color(R.color.search_keyword))
            setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                pxf(R.dimen.search_bar_keyword)
            )
            setHint(R.string.search_bar_placeholder)
            setHintTextColor(color(R.color.search_placeholder))
            setTextCursorDrawable(R.drawable.icc_search_cursor)
            setBackgroundResource(R.drawable.icc_search_bg)
            setCompoundDrawables(
                drawable(R.drawable.icc_search_icon),
                null, null, null
            )

            onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
                hint = if (hasFocus) null else
                    context.getString(R.string.search_bar_placeholder)
            }
            setOnBackKeyListener {
                if (TextUtils.isEmpty(text)) {
                    reset()
                    true
                } else {
                    false
                }
            }
        }
    }

    val cancel by lazy {
        TextView(context).apply {
            id = generateViewId()
            text = context.getString(android.R.string.cancel)
            setTextColor(color(R.color.search_keyword))
        }
    }

    init {
        val searchBarPadding = px(R.dimen.search_bar_padding)
        searchBar.setPadding(
            searchBarPadding, searchBar.paddingTop,
            searchBarPadding, searchBar.paddingBottom
        )

        val margin = px(R.dimen.search_bar_margin)
        addView(cancel, LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            topToTop = LayoutParams.PARENT_ID
            endToEnd = LayoutParams.PARENT_ID
            bottomToBottom = LayoutParams.PARENT_ID
            setMargins(0, margin, margin, 0)
        })

        addView(searchBar, LayoutParams(
            LayoutParams.MATCH_CONSTRAINT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            startToStart = LayoutParams.PARENT_ID
            topToTop = LayoutParams.PARENT_ID
            bottomToBottom = LayoutParams.PARENT_ID
            endToStart = cancel.id
            setMargins(margin, margin, margin, 0)
        })
    }

    fun setSearching(searching: Boolean) {
        if (searching) {
            searchBar.showKeyboard()
        } else {
            searchBar.reset()
            searchBar.hideKeyboard()
        }
    }
}
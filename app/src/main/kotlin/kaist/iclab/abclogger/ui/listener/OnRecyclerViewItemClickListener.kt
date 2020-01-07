package kaist.iclab.abclogger.ui.listener

import android.view.View

interface OnRecyclerViewItemClickListener<in T> {
    fun onItemClick(position: Int, item: T?, view: View)
}
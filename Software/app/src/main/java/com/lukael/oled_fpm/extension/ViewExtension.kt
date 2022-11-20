package com.lukael.oled_fpm.extension

import android.view.View

fun View.setVisibleOrGone(isVisible: Boolean) {
    this.visibility = if (isVisible) View.VISIBLE else View.GONE
}
fun View.setVisibleOrInvisible(isVisible: Boolean) {
    this.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
}
fun View.setVisible() {
    this.visibility = View.VISIBLE
}
fun View.setInvisible() {
    this.visibility = View.INVISIBLE
}
fun View.setGone() {
    this.visibility = View.GONE
}

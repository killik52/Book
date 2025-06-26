package com.example.myapplication

import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*
import android.widget.ImageView

// Extensions para EditText
fun EditText.textOrEmpty(): String = text?.toString() ?: ""

fun EditText.textOrNull(): String? = text?.toString()

fun EditText.setTextOrEmpty(value: String?) {
    setText(value ?: "")
}

// Extensions para Context
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.showLongToast(message: String) {
    showToast(message, Toast.LENGTH_LONG)
}

fun Context.getColorCompat(colorResId: Int): Int {
    return ContextCompat.getColor(this, colorResId)
}

// Extensions para View
fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.isVisible(): Boolean = visibility == View.VISIBLE

fun View.isGone(): Boolean = visibility == View.GONE

// Extensions para String
fun String.toCurrencyFormat(): String {
    val value = this.toDoubleOrNull() ?: 0.0
    return DecimalFormat("R$ #,##0.00", DecimalFormatSymbols(Locale("pt", "BR"))).format(value)
}

fun String.toCurrencyFormat(value: Double): String {
    return DecimalFormat("R$ #,##0.00", DecimalFormatSymbols(Locale("pt", "BR"))).format(value)
}

fun String.isValidEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

fun String.isValidCPF(): Boolean {
    val cpf = this.replace("[^0-9]".toRegex(), "")
    return cpf.length == 11
}

fun String.isValidCNPJ(): Boolean {
    val cnpj = this.replace("[^0-9]".toRegex(), "")
    return cnpj.length == 14
}

fun String.formatDate(inputFormat: String, outputFormat: String): String {
    return try {
        val input = SimpleDateFormat(inputFormat, Locale.getDefault())
        val output = SimpleDateFormat(outputFormat, Locale("pt", "BR"))
        val date = input.parse(this)
        date?.let { output.format(it) } ?: this
    } catch (e: Exception) {
        this
    }
}

// Extensions para Double
fun Double.toCurrencyString(): String {
    return DecimalFormat("R$ #,##0.00", DecimalFormatSymbols(Locale("pt", "BR"))).format(this)
}

// Extensions para FragmentActivity
fun FragmentActivity.showLoadingDialog(message: String = "Carregando..."): androidx.appcompat.app.AlertDialog {
    val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
        .setView(R.layout.dialog_loading)
        .setCancelable(false)
        .create()
    dialog.show()
    return dialog
}

// Extensions para validação
fun String.isValidName(): Boolean {
    return this.trim().length >= Constants.Validation.MIN_NAME_LENGTH && 
           this.trim().length <= Constants.Validation.MAX_NAME_LENGTH
}

fun String.isValidPhone(): Boolean {
    val phone = this.replace("[^0-9]".toRegex(), "")
    return phone.length >= 10 && phone.length <= 11
}

// Extensions para logging
fun String.logDebug(tag: String = "App") {
    android.util.Log.d(tag, this)
}

fun String.logError(tag: String = "App") {
    android.util.Log.e(tag, this)
}

fun String.logInfo(tag: String = "App") {
    android.util.Log.i(tag, this)
}

// Extensions para ImageView com Coil
fun ImageView.loadImage(url: String?, placeholder: Int? = null, error: Int? = null) {
    ImageLoader.loadImage(this, url, placeholder, error)
}

fun ImageView.loadCircularImage(url: String?, placeholder: Int? = null, error: Int? = null) {
    ImageLoader.loadCircularImage(this, url, placeholder, error)
}

fun ImageView.loadRoundedImage(url: String?, cornerRadius: Float = 8f, placeholder: Int? = null, error: Int? = null) {
    ImageLoader.loadRoundedImage(this, url, cornerRadius, placeholder, error)
}

fun ImageView.loadLocalImage(filePath: String?, placeholder: Int? = null, error: Int? = null) {
    ImageLoader.loadLocalImage(this, filePath, placeholder, error)
} 
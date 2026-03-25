package com.lenovo.quantum.test

import android.widget.Toast

actual fun showTip() {
    Toast.makeText(appContext, "please select a pdf file from the file picker", Toast.LENGTH_SHORT).show()
}
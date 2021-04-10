package com.valartech.loadinglayout

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.valartech.loadinglayout.LoadingLayout.Companion.COMPLETE
import com.valartech.loadinglayout.LoadingLayout.Companion.EMPTY
import com.valartech.loadinglayout.LoadingLayout.Companion.ERROR
import com.valartech.loadinglayout.LoadingLayout.Companion.LOADING
import com.valartech.loadinglayout.LoadingLayout.Companion.LOADING_OVERLAY
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loading.setOnClickListener { mainLoadingLayout.setState(LOADING) }
        loadingOverlay.setOnClickListener { mainLoadingLayout.setState(LOADING_OVERLAY) }
        complete.setOnClickListener { mainLoadingLayout.setState(COMPLETE) }
        empty.setOnClickListener { mainLoadingLayout.setState(EMPTY) }
        error.setOnClickListener { mainLoadingLayout.setState(ERROR) }

        test_button.setOnClickListener {
            Toast.makeText(this, "Test button pressed!", Toast.LENGTH_SHORT).show()
        }
    }
}

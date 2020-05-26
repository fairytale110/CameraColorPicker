package tech.nicesky.camera2colorpickerdemo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import tech.nicesky.library.PickerActivity

class DemoMainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn_pick.setOnClickListener {
            startActivity(Intent(this@DemoMainActivity, PickerActivity::class.java))
        }
    }
}

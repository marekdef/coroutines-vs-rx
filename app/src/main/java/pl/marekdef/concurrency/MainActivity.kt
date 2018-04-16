package pl.marekdef.concurrency

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

class MainActivity : AppCompatActivity() {
    companion object {
        val TAG = MainActivity::class.java.simpleName

        val okhttp = OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();

        val adorable = Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(BitmapConverter.create())
                .client(okhttp)
                .baseUrl("https://api.adorable.io")
                .build().create(AdorableAvatars::class.java)
    }

    val composite: CompositeDisposable = CompositeDisposable()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }


    override fun onResume() {
        super.onResume()

        composite.add(RxTextView.afterTextChangeEvents(editTextName)
                .filter { it.toString().trim().isNotEmpty() }
                .switchMap { adorable.avatar(it.editable().toString()).subscribeOn(Schedulers.io()) }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    Log.d(TAG, "Got bitmap size ${it.height} x ${it.width}")
                }
                .subscribe { bitmap ->
                    imageView.setImageBitmap(bitmap)
                })
    }

    override fun onPause() {
        super.onPause()
        composite.clear()
    }
}

package pl.marekdef.concurrency

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.widget.ImageView
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.experimental.CoroutineCallAdapterFactory
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fonts_main.*
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

class FontsActivity : AppCompatActivity() {
    companion object {
        val TAG = FontsActivity::class.java.simpleName

        val okhttp = OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .cache(Cache(Environment.getDownloadCacheDirectory(), 512))
                .build();

        val fonts = Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .addConverterFactory(BitmapConverter.create())
                .client(okhttp)
                .baseUrl("http://10.0.2.2:8080")
                .build().create(Fonts::class.java)
    }

    val compositeTextChange: CompositeDisposable = CompositeDisposable()
    val compositeFonts: CompositeDisposable = CompositeDisposable()

    val coroutines = mutableListOf<Deferred<Bitmap>>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fonts_main)
    }


    override fun onResume() {
        super.onResume()

        editTextName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                val text = s.toString()
                if (text.isEmpty())
                    return

                linearLayoutCoroutines.removeAllViews()
                coroutines.forEach { it.cancel() }

                text.toCharArray().forEach {
                    val imageView = ImageView(this@FontsActivity)
                    linearLayoutCoroutines.addView(
                            imageView
                    )
                    val job = fonts.fontDeferred(it)
                    coroutines.add(job)

                    launch(UI) {
                        imageView.setImageBitmap(job.await())
                    }
                }
            }

        })

        compositeTextChange.add(RxTextView.afterTextChangeEvents(editTextName)
                .filter { it.editable().toString().isNotEmpty() }
                .map { it.editable().toString() }
                .doOnNext {
                    linearLayoutRx.removeAllViews()
                    compositeFonts.clear()
                }.flatMap { Observable.fromIterable(it.asIterable()) }
                .doOnNext {
                    val imageView = ImageView(FontsActivity@this)
                    linearLayoutRx.addView(
                            imageView
                    )

                    compositeFonts.add(fonts.font(it).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe {
                        imageView.setImageBitmap(it)
                    })

                }
                .subscribe())

    }

    override fun onPause() {
        super.onPause()
        compositeTextChange.clear()
    }
}
package pl.marekdef.concurrency

import android.graphics.Bitmap
import android.os.Bundle
import android.os.StrictMode
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.experimental.CoroutineCallAdapterFactory
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import com.jakewharton.rxbinding2.widget.RxTextView
import hu.akarnokd.rxjava2.debug.RxJavaAssemblyTracking
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fonts_main.*
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import kotlin.system.measureNanoTime

class FontsActivity : AppCompatActivity() {
    companion object {
        val TAG = FontsActivity::class.java.simpleName

        val okhttp = OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
//                .cache(Cache(Environment.getDownloadCacheDirectory(), 1_024 * 1_024 * 5))
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

        RxJavaAssemblyTracking.enable()
        setContentView(R.layout.fonts_main)

        for (i in 1..25) {
            linearLayoutRx.addView(ImageView(this))
            linearLayoutCoroutines.addView(ImageView(this))
        }

        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyDeath().build())
    }


    override fun onResume() {
        super.onResume()

        setupRx()

//        setupCoroutine()
    }

    private fun setupRx() {
        compositeTextChange.add(RxTextView.afterTextChangeEvents(editTextName)
                .debounce(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .map { it.editable().toString() }
                .doOnNext {
                    resetViews(linearLayoutRx, it.length)
                }
                .doOnNext {
                    compositeFonts.clear()
                }
                .filter { it.isNotEmpty() }
                .flatMap {
                    Observable.fromIterable(it.toCharArray().asIterable())
                            .map { fonts.fontSingle(it).subscribeOn(Schedulers.io()) }
                            .concatMapSingle { it }
                            .zipWith(Observable.range(0, 25), BiFunction {b: Bitmap, idx: Int -> Pair(idx, b)})
//                            .zipWith(Observable.range(0, 25), BiFunction { c: Char, idx: Int -> Pair(idx, c) })
//                            .flatMapSingle { (index, char) ->
//                                fonts.fontSingle(char)
//                                        .subscribeOn(Schedulers.io())
//                                        .map { bitmap -> index to bitmap }
//                            }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    val imageView = linearLayoutRx.getChildAt(it.first) as ImageView

                    imageView.setImageBitmap(it.second)
                    imageView.visibility = View.VISIBLE
                })
    }

    private fun setupCoroutine() {
        editTextName.addTextChangedListener(object : TextWatcher {
            var runnable: Runnable? = null

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                val text = s.toString()

                runnable?.let {
                    linearLayoutCoroutines.handler.removeCallbacks(it)
                }

                runnable = Runnable({ coroutineTextChanged(text) })
                linearLayoutCoroutines.handler.postDelayed(runnable, 500);
            }
        })
    }

    private fun coroutineTextChanged(text: String) {
        resetViews(linearLayoutCoroutines, text.length)
        cancelPendingCoroutines()

        if (text.isEmpty())
            if (text.isEmpty())
                return

        text.toCharArray().forEachIndexed { index, char ->
            val job = fonts.fontDeferred(char)
            coroutines.add(job)

            launch(UI) {
                val imageView = linearLayoutCoroutines.getChildAt(index) as ImageView
                imageView.setImageBitmap(job.await())
                imageView.visibility = View.VISIBLE
            }
        }
    }

    private fun cancelPendingCoroutines() {
        coroutines.forEach { it.cancel() }
    }

    private fun resetViews(layout: LinearLayout, length: Int) {
        for (i in 1 until length) {
            (layout.getChildAt(i) as ImageView).setImageBitmap(null)
        }
        for (i in length..24) {
            layout.getChildAt(i).visibility = View.GONE
        }
    }

    override fun onPause() {
        super.onPause()
        disposeRx()

        cancelPendingCoroutines()
    }

    private fun disposeRx() {
        compositeTextChange.clear()
        compositeFonts.clear()
    }
}
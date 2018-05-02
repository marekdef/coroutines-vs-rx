package pl.marekdef.concurrency

import android.graphics.Bitmap
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.coroutines.experimental.Deferred
import retrofit2.http.GET
import retrofit2.http.Path

interface Fonts {
    @GET("/font/{font}")
    fun fontSingle(@Path("font") font: Char) : Single<Bitmap>


    @GET("/font/{font}")
    fun fontDeferred(@Path("font") font: Char) : Deferred<Bitmap>
}
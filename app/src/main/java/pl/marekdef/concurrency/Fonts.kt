package pl.marekdef.concurrency

import android.graphics.Bitmap
import io.reactivex.Observable
import kotlinx.coroutines.experimental.Deferred
import retrofit2.http.GET
import retrofit2.http.Path

interface Fonts {
    @GET("/font/{font}")
    fun font(@Path("font") font: Char) : Observable<Bitmap>


    @GET("/font/{font}")
    fun fontDeferred(@Path("font") font: Char) : Deferred<Bitmap>
}
package pl.marekdef.concurrency

import android.content.ContentValues.TAG
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import java.lang.reflect.Type

/**
 * Created by defecins on 28/03/2018.
 */

interface AdorableAvatars {
    /**
    https://api.adorable.io/avatars/285/marekdef%40gmail.com
     **/

    @GET("/avatars/285/{avatar}")
    fun avatar(@Path("avatar") avatar: String): Observable<Bitmap>
}


package pl.marekdef.concurrency;

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

class BitmapConverter : Converter<ResponseBody, Bitmap>, Converter.Factory() {
    companion object : Converter.Factory() {
        val TAG = BitmapConverter::class.java.simpleName
        fun create(): Converter.Factory {
            Log.d(TAG, "create()")
            return this
        }

        override fun responseBodyConverter(type: Type, annotations: Array<out Annotation>, retrofit: Retrofit): Converter<ResponseBody, *>? {
            Log.d(TAG, "responseBodyConverter($type, $annotations, $retrofit)")
            if (type.equals(Bitmap::class.java)) {
                return BitmapConverter()
            }
            return super.responseBodyConverter(type, annotations, retrofit)
        }
    }


    override fun convert(body: ResponseBody): Bitmap {
        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        return BitmapFactory.decodeStream(body.byteStream(), null,options )
    }

}
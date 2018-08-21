package zebrostudio.wallr100.domain

import io.reactivex.Observable
import io.reactivex.Single
import zebrostudio.wallr100.domain.model.PicturesModel

interface WallrRepository {

  fun authenticatePurchase(
    packageName: String,
    skuId: String,
    purchaseToken: String
  ): Single<Any>

  fun saveUserAsPro(): Boolean
  fun getUserProStatus(): Boolean

  fun getPictures(query: String): Observable<PicturesModel>

}
package zebrostudio.wallr100.domain

import io.reactivex.Completable
import io.reactivex.Single
import zebrostudio.wallr100.domain.model.SearchPicturesModel

interface WallrRepository {

  fun authenticatePurchase(
    packageName: String,
    skuId: String,
    purchaseToken: String
  ): Completable

  fun updateUserPurchaseStatus(): Boolean
  fun isUserPremium(): Boolean

  fun getPictures(query: String): Single<List<SearchPicturesModel>>

}
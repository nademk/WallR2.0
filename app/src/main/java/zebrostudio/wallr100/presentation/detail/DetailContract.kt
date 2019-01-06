package zebrostudio.wallr100.presentation.detail

import android.content.Intent
import zebrostudio.wallr100.presentation.BasePresenter
import zebrostudio.wallr100.presentation.BaseView
import zebrostudio.wallr100.presentation.adapters.ImageRecyclerViewPresenterImpl.ImageListType
import zebrostudio.wallr100.presentation.search.model.SearchPicturesPresenterEntity
import zebrostudio.wallr100.presentation.wallpaper.model.ImagePresenterEntity

interface DetailContract {

  interface DetailView : BaseView {
    fun getSearchImageDetails(): SearchPicturesPresenterEntity
    fun getWallpaperImageDetails(): ImagePresenterEntity
    fun showAuthorDetails(name: String, profileImageLink: String)
    fun showImage(lowQualityLink: String, highQualityLink: String)
    fun showImageLoadError()
    fun hasStoragePermission(): Boolean
    fun requestStoragePermission(actionType: ActionType)
    fun showPermissionRequiredMessage()
    fun showNoInternetToShareError()
    fun shareLink(shortLink: String)
    fun showWaitLoader(message: String)
    fun hideWaitLoader()
    fun redirectToBuyPro(requestCode: Int)
    fun showGenericErrorMessage()
  }

  interface DetailPresenter : BasePresenter<DetailView> {
    fun setImageType(imageType: ImageListType)
    fun notifyHighQualityImageLoadFailed()
    fun notifyQuickSetClick()
    fun notifyDownloadClick()
    fun notifyCrystallizeClick()
    fun notifyEditSetClick()
    fun notifyAddToCollectionClick()
    fun notifyShareClick()
    fun notifyPermissionRequestResult(
      requestCode: Int, permissions: Array<String>, grantResults: IntArray
    )
    fun notifyActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
  }
}
package zebrostudio.wallr100.presentation.detail

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.uber.autodispose.autoDisposable
import com.yalantis.ucrop.UCrop.REQUEST_CROP
import io.reactivex.Observer
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import zebrostudio.wallr100.R
import zebrostudio.wallr100.android.ui.buypro.PurchaseTransactionConfig
import zebrostudio.wallr100.android.utils.WallpaperSetter
import zebrostudio.wallr100.data.exception.ImageDownloadException
import zebrostudio.wallr100.domain.executor.PostExecutionThread
import zebrostudio.wallr100.domain.interactor.ImageOptionsUseCase
import zebrostudio.wallr100.domain.interactor.UserPremiumStatusUseCase
import zebrostudio.wallr100.domain.model.imagedownload.ImageDownloadModel
import zebrostudio.wallr100.presentation.adapters.ImageRecyclerViewPresenterImpl.ImageListType
import zebrostudio.wallr100.presentation.adapters.ImageRecyclerViewPresenterImpl.ImageListType.SEARCH
import zebrostudio.wallr100.presentation.detail.ActionType.ADD_TO_COLLECTION
import zebrostudio.wallr100.presentation.detail.ActionType.CRYSTALLIZE
import zebrostudio.wallr100.presentation.detail.ActionType.DOWNLOAD
import zebrostudio.wallr100.presentation.detail.ActionType.EDIT_SET
import zebrostudio.wallr100.presentation.detail.ActionType.QUICK_SET
import zebrostudio.wallr100.presentation.detail.ActionType.SHARE
import zebrostudio.wallr100.presentation.search.model.SearchPicturesPresenterEntity
import zebrostudio.wallr100.presentation.wallpaper.model.ImagePresenterEntity

class DetailPresenterImpl(
  private var context: Context,
  private var imageOptionsUseCase: ImageOptionsUseCase,
  private var userPremiumStatusUseCase: UserPremiumStatusUseCase,
  private val wallpaperSetter: WallpaperSetter,
  private val postExecutionThread: PostExecutionThread
) : DetailContract.DetailPresenter {

  internal lateinit var imageType: ImageListType
  internal lateinit var wallpaperImage: ImagePresenterEntity
  internal lateinit var searchImage: SearchPicturesPresenterEntity
  internal var isDownloadInProgress = false
  internal var isImageOperationInProgress = false
  internal var wallpaperHasBeenSet = false
  internal var isSlidingPanelExpanded = false
  internal var imageHasBeenCrystallized = false
  private val downloadCompletedValue: Long = 100
  private val showIndefiniteLoaderAtProgressValue: Long = 99
  private val downloadStartedValue: Long = 0
  private var downloadProgress: Long = 0
  private var detailView: DetailContract.DetailView? = null

  override fun attachView(view: DetailContract.DetailView) {
    detailView = view
  }

  override fun detachView() {
    imageHasBeenCrystallized = false
    detailView = null
  }

  override fun setImageType(imageType: ImageListType) {
    this.imageType = imageType
    if (imageType == SEARCH) {
      searchImage = detailView?.getSearchImageDetails()!!
    } else {
      wallpaperImage = detailView?.getWallpaperImageDetails()!!
    }
    decorateView()
  }

  override fun handleHighQualityImageLoadFailed() {
    detailView?.showImageLoadError()
  }

  override fun handleQuickSetClick() {
    if (detailView?.hasStoragePermission() == true) {
      if (detailView?.internetAvailability() == true) {
        quickSetWallpaper()
      } else {
        detailView?.showNoInternetError()
      }
    } else {
      detailView?.requestStoragePermission(QUICK_SET)
    }
  }

  override fun handleDownloadClick() {
    if (userPremiumStatusUseCase.isUserPremium()) {
      if (detailView?.hasStoragePermission() == true) {
        if (detailView?.internetAvailability() == true) {
          downloadWallpaper()
        } else {
          detailView?.showNoInternetError()
        }
      } else {
        detailView?.requestStoragePermission(DOWNLOAD)
      }
    } else {
      detailView?.redirectToBuyPro(DOWNLOAD.ordinal)
    }
  }

  override fun handleCrystallizeClick() {
    if (userPremiumStatusUseCase.isUserPremium()) {
      if (detailView?.hasStoragePermission() == true) {
        if (detailView?.internetAvailability() == true) {
          if (imageOptionsUseCase.isCrystallizeDescriptionDialogShown()) {
            if (!imageHasBeenCrystallized) {
              crystallizeWallpaper()
            } else {
              detailView?.showImageHasAlreadyBeenCrystallizedMessage()
            }
          } else {
            detailView?.showTryCrystallizeDescriptionDialog()
          }
        } else {
          detailView?.showNoInternetError()
        }
      } else {
        detailView?.requestStoragePermission(CRYSTALLIZE)
      }
    } else {
      detailView?.redirectToBuyPro(CRYSTALLIZE.ordinal)
    }
  }

  override fun handleEditSetClick() {
    if (detailView?.hasStoragePermission() == true) {
      if (detailView?.internetAvailability() == true) {
        editSetWallpaper()
      } else {
        detailView?.showNoInternetError()
      }
    } else {
      detailView?.requestStoragePermission(EDIT_SET)
    }
  }

  override fun handleAddToCollectionClick() {
    if (userPremiumStatusUseCase.isUserPremium()) {
      if (detailView?.hasStoragePermission() == true) {
        if (detailView?.internetAvailability() == true) {
          addWallpaperToCollection()
        } else {
          detailView?.showNoInternetError()
        }
      } else {
        detailView?.requestStoragePermission(ADD_TO_COLLECTION)
      }
    } else {
      detailView?.redirectToBuyPro(ADD_TO_COLLECTION.ordinal)
    }
  }

  override fun handleShareClick() {
    if (detailView?.internetAvailability() == true) {
      if (userPremiumStatusUseCase.isUserPremium()) {
        val link = if (imageType == SEARCH) {
          searchImage.imageQualityUrlPresenterEntity.largeImageLink
        } else {
          wallpaperImage.imageLink.large
        }
        imageOptionsUseCase.getImageShareableLinkSingle(link)
            .observeOn(postExecutionThread.scheduler)
            .autoDisposable(detailView?.getScope()!!)
            .subscribe({
              detailView?.hideWaitLoader()
              detailView?.shareLink(it)
            }, {
              detailView?.hideWaitLoader()
              detailView?.showGenericErrorMessage()
            })
      } else {
        detailView?.redirectToBuyPro(SHARE.ordinal)
      }
    } else {
      detailView?.showNoInternetToShareError()
    }
  }

  override fun handleBackButtonClick() {
    if (isDownloadInProgress) {
      imageOptionsUseCase.cancelFetchImageOperation()
      isDownloadInProgress = false
      detailView?.hideScreenBlur()
      detailView?.showDownloadWallpaperCancelledMessage()
    } else if (isImageOperationInProgress) {
      detailView?.showWallpaperOperationInProgressWaitMessage()
    } else {
      if (isSlidingPanelExpanded) {
        detailView?.collapseSlidingPanel()
      } else {
        imageOptionsUseCase.clearCachesCompletable()
            .observeOn(postExecutionThread.scheduler)
            .autoDisposable(detailView?.getScope()!!)
            .subscribe({
              detailView?.exitView()
            }, {
              detailView?.exitView()
            })
      }
    }
  }

  override fun handlePermissionRequestResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
    if (requestCode == QUICK_SET.ordinal ||
        requestCode == DOWNLOAD.ordinal ||
        requestCode == CRYSTALLIZE.ordinal ||
        requestCode == EDIT_SET.ordinal ||
        requestCode == ADD_TO_COLLECTION.ordinal
    ) {
      if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
        handlePermissionGranted(requestCode)
      } else {
        detailView?.showPermissionRequiredMessage()
      }
    }
  }

  override fun handleViewResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == DOWNLOAD.ordinal) {
      if (resultCode == PurchaseTransactionConfig.PURCHASE_SUCCESSFUL_RESULT_CODE) {
        handleDownloadClick()
      } else {
        detailView?.showUnsuccessfulPurchaseError()
      }
    } else if (requestCode == CRYSTALLIZE.ordinal) {
      if (resultCode == PurchaseTransactionConfig.PURCHASE_SUCCESSFUL_RESULT_CODE) {
        handleCrystallizeClick()
      } else {
        detailView?.showUnsuccessfulPurchaseError()
      }
    } else if (requestCode == ADD_TO_COLLECTION.ordinal) {
      if (resultCode == PurchaseTransactionConfig.PURCHASE_SUCCESSFUL_RESULT_CODE) {
        handleAddToCollectionClick()
      } else {
        detailView?.showUnsuccessfulPurchaseError()
      }
    } else if (requestCode == SHARE.ordinal) {
      if (resultCode == PurchaseTransactionConfig.PURCHASE_SUCCESSFUL_RESULT_CODE) {
        handleShareClick()
      } else {
        detailView?.showUnsuccessfulPurchaseError()
      }
    } else if (requestCode == REQUEST_CROP && resultCode == RESULT_OK) {
      detailView?.let {
        val cropResultUri = detailView?.getUriFromIntent(data!!)
        if (cropResultUri != null) {
          handleCropResult(cropResultUri)
        } else {
          detailView?.hideScreenBlur()
          detailView?.showGenericErrorMessage()
        }
      }
    } else {
      isDownloadInProgress = false
      isImageOperationInProgress = false
      detailView?.hideScreenBlur()
    }
  }

  override fun handleDownloadQualitySelectionEvent(
    downloadType: ImageListType,
    selectedIndex: Int
  ) {
    if (selectedIndex < 5) {
      val downloadLink = getDownloadLink(downloadType, selectedIndex)
      if (imageOptionsUseCase.isDownloadInProgress(downloadLink)) {
        detailView?.showDownloadAlreadyInProgressMessage()
      } else {
        imageOptionsUseCase.downloadImageCompletable(downloadLink)
            .observeOn(postExecutionThread.scheduler)
            .doOnSubscribe {
              detailView?.showDownloadStartedMessage()
            }
            .autoDisposable(detailView?.getScope()!!)
            .subscribe({
              detailView?.showDownloadCompletedSuccessMessage()
            }, {
              detailView?.showGenericErrorMessage()
            })
      }
    }
  }

  override fun handleCrystallizeDialogPositiveClick() {
    imageOptionsUseCase.setCrystallizeDescriptionShownOnce()
    handleCrystallizeClick()
  }

  override fun setPanelStateAsExpanded() {
    isSlidingPanelExpanded = true
  }

  override fun setPanelStateAsCollapsed() {
    isSlidingPanelExpanded = false
  }

  private fun decorateView() {
    if (imageType == SEARCH) {
      detailView?.showAuthorDetails(searchImage.userPresenterEntity.name,
          searchImage.userPresenterEntity.profileImageLink)
      detailView?.showImage(searchImage.imageQualityUrlPresenterEntity.smallImageLink,
          searchImage.imageQualityUrlPresenterEntity.largeImageLink)
    } else {
      detailView?.showAuthorDetails(wallpaperImage.author.name,
          wallpaperImage.author.profileImageLink)
      detailView?.showImage(wallpaperImage.imageLink.thumb, wallpaperImage.imageLink.large)
    }
  }

  private fun handlePermissionGranted(requestCode: Int) {
    when (requestCode) {
      QUICK_SET.ordinal -> handleQuickSetClick()
      DOWNLOAD.ordinal -> handleDownloadClick()
      CRYSTALLIZE.ordinal -> handleCrystallizeClick()
      EDIT_SET.ordinal -> handleEditSetClick()
      ADD_TO_COLLECTION.ordinal -> handleAddToCollectionClick()
    }
  }

  private fun quickSetWallpaper() {
    downloadProgress = downloadStartedValue
    detailView?.hideIndefiniteLoader()
    detailView?.blurScreenAndInitializeProgressPercentage()
    val imageDownloadLink = when (imageType) {
      SEARCH -> searchImage.imageQualityUrlPresenterEntity.largeImageLink
      else -> wallpaperImage.imageLink.large
    }
    imageOptionsUseCase.fetchImageBitmapObservable(imageDownloadLink)
        .doOnNext {
          if (it.progress == downloadCompletedValue) {
            wallpaperHasBeenSet = wallpaperSetter.setWallpaper(it.imageBitmap)
          }
        }
        .observeOn(postExecutionThread.scheduler)
        .autoDisposable(detailView?.getScope()!!)
        .subscribe(object : Observer<ImageDownloadModel> {
          override fun onComplete() {
            isDownloadInProgress = false
          }

          override fun onSubscribe(d: Disposable) {
            isDownloadInProgress = true
            wallpaperHasBeenSet = false
          }

          override fun onNext(it: ImageDownloadModel) {
            val progress = it.progress
            if (progress == showIndefiniteLoaderAtProgressValue) {
              isDownloadInProgress = false
              isImageOperationInProgress = true
              detailView?.updateProgressPercentage("$downloadCompletedValue%")
              val message =
                  context.getString(R.string.detail_activity_finalizing_wallpaper_messsage)
              detailView?.showIndefiniteLoaderWithAnimation(message)
            } else if (progress == downloadCompletedValue) {
              val message =
                  context.getString(R.string.detail_activity_finalizing_wallpaper_messsage)
              detailView?.showIndefiniteLoader(message)
              if (wallpaperHasBeenSet) {
                detailView?.showWallpaperSetSuccessMessage()
              } else {
                detailView?.showWallpaperSetErrorMessage()
              }
              isImageOperationInProgress = false
              detailView?.hideScreenBlur()
            } else {
              detailView?.updateProgressPercentage("$progress%")
            }
          }

          override fun onError(throwable: Throwable) {
            if (throwable is ImageDownloadException) {
              detailView?.showUnableToDownloadErrorMessage()
            } else {
              detailView?.showGenericErrorMessage()
            }
            detailView?.hideScreenBlur()
          }
        })
  }

  private fun downloadWallpaper() {
    if (imageType == SEARCH) {
      if (imageHasBeenCrystallized) {
        detailView?.showSearchTypeDownloadDialog(true)
      } else {
        detailView?.showSearchTypeDownloadDialog(false)
      }
    } else {
      if (imageHasBeenCrystallized) {
        detailView?.showWallpaperTypeDownloadDialog(true)
      } else {
        detailView?.showWallpaperTypeDownloadDialog(false)
      }
    }
  }

  private fun crystallizeWallpaper() {
    downloadProgress = downloadStartedValue
    detailView?.hideIndefiniteLoader()
    detailView?.blurScreenAndInitializeProgressPercentage()
    val imageDownloadLink = when (imageType) {
      SEARCH -> searchImage.imageQualityUrlPresenterEntity.largeImageLink
      else -> wallpaperImage.imageLink.large
    }
    imageOptionsUseCase.fetchImageBitmapObservable(imageDownloadLink)
        .observeOn(postExecutionThread.scheduler)
        .doOnNext {
          val progress = it.progress
          if (progress == showIndefiniteLoaderAtProgressValue) {
            isDownloadInProgress = false
            isImageOperationInProgress = true
            detailView?.updateProgressPercentage("$downloadCompletedValue%")
            val message =
                context.getString(R.string.detail_activity_crystallizing_wallpaper_message)
            detailView?.showIndefiniteLoaderWithAnimation(message)
          } else {
            detailView?.updateProgressPercentage("$progress%")
          }
        }
        .doOnSubscribe {
          isDownloadInProgress = true
          wallpaperHasBeenSet = false
        }
        .flatMapSingle {
          if (it.progress == downloadCompletedValue) {
            imageOptionsUseCase.crystallizeImageSingle()
                .observeOn(postExecutionThread.scheduler)
          } else {
            Single.just(Pair(false, null))
          }
        }
        .autoDisposable(detailView?.getScope()!!)
        .subscribe({
          if (it.first) {
            detailView?.showImage(it.second!!)
            detailView?.hideScreenBlur()
            detailView?.showCrystallizeSuccessMessage()
            imageHasBeenCrystallized = true
            isImageOperationInProgress = false
          }
        }, {
          if (it is ImageDownloadException) {
            detailView?.showUnableToDownloadErrorMessage()
          } else {
            detailView?.showGenericErrorMessage()
          }
          detailView?.hideScreenBlur()
        })

  }

  private fun editSetWallpaper() {
    downloadProgress = downloadStartedValue
    detailView?.hideIndefiniteLoader()
    detailView?.blurScreenAndInitializeProgressPercentage()
    val imageDownloadLink = when (imageType) {
      SEARCH -> searchImage.imageQualityUrlPresenterEntity.largeImageLink
      else -> wallpaperImage.imageLink.large
    }
    imageOptionsUseCase.fetchImageBitmapObservable(imageDownloadLink)
        .doOnNext {
          if (it.progress == downloadCompletedValue) {
            detailView?.startCroppingActivity(
                imageOptionsUseCase.getCroppingSourceUri(),
                imageOptionsUseCase.getCroppingDestinationUri(),
                wallpaperSetter.getDesiredMinimumWidth(),
                wallpaperSetter.getDesiredMinimumHeight()
            )
          }
        }
        .observeOn(postExecutionThread.scheduler)
        .autoDisposable(detailView?.getScope()!!)
        .subscribe(object : Observer<ImageDownloadModel> {
          override fun onComplete() {
            isDownloadInProgress = false
          }

          override fun onSubscribe(d: Disposable) {
            isDownloadInProgress = true
            wallpaperHasBeenSet = false
          }

          override fun onNext(it: ImageDownloadModel) {
            val progress = it.progress
            if (progress == showIndefiniteLoaderAtProgressValue) {
              isDownloadInProgress = false
              isImageOperationInProgress = true
              detailView?.updateProgressPercentage("$downloadCompletedValue%")
              val message =
                  context.getString(R.string.detail_activity_editing_tool_message)
              detailView?.showIndefiniteLoaderWithAnimation(message)
            } else {
              detailView?.updateProgressPercentage("$progress%")
            }
          }

          override fun onError(throwable: Throwable) {
            if (throwable is ImageDownloadException) {
              detailView?.showUnableToDownloadErrorMessage()
            } else {
              detailView?.showGenericErrorMessage()
            }
            detailView?.hideScreenBlur()
          }

        })
  }

  private fun addWallpaperToCollection() {
    // To be implemented later
  }

  private fun handleCropResult(cropResultUri: Uri) {
    var hasWallpaperBeenSet = false
    detailView?.blurScreen()
    detailView?.showIndefiniteLoader(
        context.getString(R.string.detail_activity_finalizing_wallpaper_messsage))
    imageOptionsUseCase.getBitmapFromUriSingle(cropResultUri)
        .doOnSuccess {
          hasWallpaperBeenSet = wallpaperSetter.setWallpaper(it)
        }
        .observeOn(postExecutionThread.scheduler)
        .autoDisposable(detailView?.getScope()!!)
        .subscribe({
          if (hasWallpaperBeenSet) {
            detailView?.showImage(it)
            detailView?.showWallpaperSetSuccessMessage()
          } else {
            detailView?.showWallpaperSetErrorMessage()
          }
          isImageOperationInProgress = false
          detailView?.hideScreenBlur()
        }, {
          detailView?.showGenericErrorMessage()
          isImageOperationInProgress = false
          detailView?.hideScreenBlur()
        })
  }

  private fun getDownloadLink(
    downloadType: ImageListType,
    selectedIndex: Int
  ): String {
    return if (downloadType == SEARCH) {
      if (selectedIndex == 0) {
        searchImage.imageQualityUrlPresenterEntity.rawImageLink
      } else if (selectedIndex == 1) {
        searchImage.imageQualityUrlPresenterEntity.largeImageLink
      } else if (selectedIndex == 2) {
        searchImage.imageQualityUrlPresenterEntity.regularImageLink
      } else if (selectedIndex == 3) {
        searchImage.imageQualityUrlPresenterEntity.thumbImageLink
      } else {
        searchImage.imageQualityUrlPresenterEntity.smallImageLink
      }
    } else {
      if (selectedIndex == 0) {
        wallpaperImage.imageLink.raw
      } else if (selectedIndex == 1) {
        wallpaperImage.imageLink.large
      } else if (selectedIndex == 2) {
        wallpaperImage.imageLink.medium
      } else if (selectedIndex == 3) {
        wallpaperImage.imageLink.thumb
      } else {
        wallpaperImage.imageLink.small
      }
    }
  }
}

enum class ActionType {
  QUICK_SET,
  DOWNLOAD,
  CRYSTALLIZE,
  EDIT_SET,
  ADD_TO_COLLECTION,
  SHARE
}

enum class ImageDownloadType {
  SUPER_HIGH,
  HIGH,
  MEDIUM,
  LOW,
  SUPER_LOW,
  CRYSTALLIZED_VERSION
}
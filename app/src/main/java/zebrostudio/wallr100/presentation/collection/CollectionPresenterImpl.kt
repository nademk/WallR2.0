package zebrostudio.wallr100.presentation.collection

import android.net.Uri
import com.uber.autodispose.autoDisposable
import zebrostudio.wallr100.R
import zebrostudio.wallr100.android.service.wallpaperChangerIntervals
import zebrostudio.wallr100.android.ui.minimal.SINGLE_ITEM_SIZE
import zebrostudio.wallr100.android.utils.ResourceUtils
import zebrostudio.wallr100.android.utils.WallpaperSetter
import zebrostudio.wallr100.domain.executor.PostExecutionThread
import zebrostudio.wallr100.domain.interactor.AutomaticWallpaperChangerIntervalUpdateResultState.INTERVAL_UPDATED
import zebrostudio.wallr100.domain.interactor.AutomaticWallpaperChangerIntervalUpdateResultState.SERVICE_RESTARTED
import zebrostudio.wallr100.domain.interactor.CollectionImagesUseCase
import zebrostudio.wallr100.domain.interactor.UserPremiumStatusUseCase
import zebrostudio.wallr100.domain.interactor.WidgetHintsUseCase
import zebrostudio.wallr100.presentation.collection.CollectionContract.CollectionPresenter
import zebrostudio.wallr100.presentation.collection.CollectionContract.CollectionView
import zebrostudio.wallr100.presentation.collection.Model.CollectionsPresenterEntity
import zebrostudio.wallr100.presentation.collection.mapper.CollectionImagesPresenterEntityMapper
import java.util.Collections
import java.util.TreeMap

const val MANUFACTURER_NAME_XIAOMI = "xiaomi"
const val SECURITY_PACKAGE_XIAOMI = "com.miui.securitycenter"
const val AUTOSTART_CLASS_NAME_XIAOMI = "com.miui.permcenter.autostart.AutoStartManagementActivity"
const val MANUFACTURER_NAME_OPPO = "oppo"
const val SECURITY_PACKAGE_OPPO = "com.coloros.safecenter"
const val AUTOSTART_CLASS_NAME_OPPO =
    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
const val MANUFACTURER_NAME_VIVO = "vivo"
const val SECURITY_PACKAGE_VIVO = "com.vivo.permissionmanager"
const val AUTOSTART_CLASS_NAME_VIVO = "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"

private const val MINIMUM_LIST_SIZE_REQUIRED_TO_SHOW_HINT = 2
private const val MINIMUM_NUMBER_OF_SELECTED_ITEMS = 1
private const val INDEX_OF_THIRTY_MINUTES_WALLPAPER_CHANGER_INTERVAL = 0
private const val INDEX_OF_THREE_DAYS_WALLPAPER_CHANGER_INTERVAL = 4

class CollectionPresenterImpl(
  private val widgetHintsUseCase: WidgetHintsUseCase,
  private val userPremiumStatusUseCase: UserPremiumStatusUseCase,
  private val collectionImagesUseCase: CollectionImagesUseCase,
  private val collectionImagesPresenterEntityMapper: CollectionImagesPresenterEntityMapper,
  private val wallpaperSetter: WallpaperSetter,
  private val resourceUtils: ResourceUtils,
  private val postExecutionThread: PostExecutionThread
) : CollectionPresenter {

  private var collectionView: CollectionView? = null

  override fun attachView(view: CollectionView) {
    collectionView = view
  }

  override fun detachView() {
    collectionView = null
  }

  override fun handleViewCreated() {
    if (isUserPremium() && isStoragePermissionAvailable()) {
      if (collectionImagesUseCase.isAutomaticWallpaperChangerRunning()) {
        collectionView?.showAutomaticWallpaperStateAsActive()
      } else {
        collectionView?.showAutomaticWallpaperStateAsInActive()
      }
      showPictures()
    }
  }

  override fun handleActivityResult() {
    handleViewCreated()
  }

  override fun handleImportFromLocalStorageClicked() {
    if (isUserPremium() && isStoragePermissionAvailable()) {
      collectionView?.showImagePicker()
    }
  }

  override fun handlePurchaseClicked() {
    collectionView?.redirectToBuyPro()
  }

  override fun handleReorderImagesHintHintDismissed() {
    widgetHintsUseCase.saveCollectionsImageReorderHintShown()
  }

  override fun handleItemMoved(
    fromPosition: Int,
    toPosition: Int,
    imagePathList: MutableList<CollectionsPresenterEntity>
  ) {
    val copyOfImageListPriorToReordering = mutableListOf<CollectionsPresenterEntity>()
    copyOfImageListPriorToReordering.addAll(imagePathList)
    if (fromPosition < toPosition) {
      for (i in fromPosition until toPosition) {
        Collections.swap(imagePathList, i, i + 1)
      }
    } else {
      for (i in fromPosition downTo toPosition + 1) {
        Collections.swap(imagePathList, i, i - 1)
      }
    }
    collectionView?.updateItemViewMovement(fromPosition, toPosition)
    collectionImagesUseCase.reorderImage(
        collectionImagesPresenterEntityMapper.mapFromPresenterEntity(imagePathList))
        .observeOn(postExecutionThread.scheduler)
        .map {
          collectionImagesPresenterEntityMapper.mapToPresenterEntity(it)
        }
        .autoDisposable(collectionView!!.getScope())
        .subscribe({
          collectionView?.setImagesList(it)
          collectionView?.showReorderSuccessMessage()
          copyOfImageListPriorToReordering.clear()
        }, {
          collectionView?.setImagesList(copyOfImageListPriorToReordering)
          collectionView?.updateChangesInEveryItemView()
          collectionView?.showUnableToReorderErrorMessage()
        })
  }

  override fun handleItemClicked(
    position: Int,
    imageList: List<CollectionsPresenterEntity>,
    selectedItemsMap: HashMap<Int, CollectionsPresenterEntity>
  ) {
    if (selectedItemsMap.isEmpty()) {
      collectionView?.hideAppBar()
    }
    if (selectedItemsMap.containsKey(position)) {
      selectedItemsMap.remove(position)
    } else {
      selectedItemsMap[position] = imageList[position]
    }
    updateSelectionChangesInCab(position, selectedItemsMap.size)

  }

  override fun notifyDragStarted() {
    hideCabIfActive()
  }

  override fun handleAutomaticWallpaperChangerEnabled() {
    collectionImagesUseCase.startAutomaticWallpaperChanger()
    collectionView?.getManufacturerName()?.let {
      if (it.equals(MANUFACTURER_NAME_XIAOMI, true)
          || it.equals(MANUFACTURER_NAME_OPPO, true)
          || it.equals(MANUFACTURER_NAME_VIVO, true)) {
        collectionView?.showAutoStartPermissionRequiredDialog()
      }
    }
  }

  override fun handleAutomaticWallpaperChangerDisabled() {
    collectionImagesUseCase.stopAutomaticWallpaperChanger()
  }

  override fun handleAutomaticWallpaperChangerIntervalMenuItemClicked() {
    collectionImagesUseCase.getAutomaticWallpaperChangerInterval().let {
      var isDialogShown = false
      wallpaperChangerIntervals.forEachIndexed { index, interval ->
        if (it == interval) {
          collectionView?.showWallpaperChangerIntervalDialog(index)
          isDialogShown = true
        } else if (index == INDEX_OF_THREE_DAYS_WALLPAPER_CHANGER_INTERVAL && !isDialogShown) {
          collectionImagesUseCase.setAutomaticWallpaperChangerInterval(
              wallpaperChangerIntervals[INDEX_OF_THREE_DAYS_WALLPAPER_CHANGER_INTERVAL])
          collectionView?.showWallpaperChangerIntervalDialog(
              INDEX_OF_THIRTY_MINUTES_WALLPAPER_CHANGER_INTERVAL)
        }
      }
    }
  }

  override fun updateWallpaperChangerInterval(choice: Int) {
    when (collectionImagesUseCase.setAutomaticWallpaperChangerInterval(
        wallpaperChangerIntervals[choice])) {
      INTERVAL_UPDATED -> collectionView?.showWallpaperChangerIntervalUpdatedSuccessMessage()
      SERVICE_RESTARTED -> collectionView?.showWallpaperChangerRestartedSuccessMessage()
    }
  }

  override fun handleImagePickerResult(uriList: List<Uri>) {
    if (uriList.isNotEmpty()) {
      collectionImagesUseCase.addImage(uriList)
          .map {
            collectionImagesPresenterEntityMapper.mapToPresenterEntity(it)
          }
          .observeOn(postExecutionThread.scheduler)
          .autoDisposable(collectionView!!.getScope())
          .subscribe({
            collectionView?.setImagesList(it)
            collectionView?.updateChangesInEveryItemView()
            if (it.isNotEmpty()) {
              showNonEmptyCollectionView()
            }
            showHintIfSuitable(it.size)
            uriList.size.let {
              if (it == SINGLE_ITEM_SIZE) {
                collectionView?.showSingleImageAddedSuccessfullyMessage()
              } else {
                collectionView?.showMultipleImagesAddedSuccessfullyMessage(it)
              }
            }
          }, {
            println(it.message)
            collectionView?.showGenericErrorMessage()
          })
    }
  }

  override fun handleSetWallpaperMenuItemClicked(selectedItemsMap: HashMap<Int, CollectionsPresenterEntity>) {
    collectionImagesUseCase.getImageBitmap(
        collectionImagesPresenterEntityMapper.mapFromPresenterEntity(
            listOf(selectedItemsMap.values.first())).first())
        .doOnSuccess {
          wallpaperSetter.setWallpaper(it)
        }
        .observeOn(postExecutionThread.scheduler)
        .doOnSubscribe {
          collectionView?.blurScreen()
          collectionView?.showIndefiniteLoaderWithMessage(
              resourceUtils.getStringResource(R.string.finalizing_wallpaper_messsage))
        }
        .autoDisposable(collectionView!!.getScope())
        .subscribe({
          hideCabIfActive()
          collectionView?.removeBlurFromScreen()
          collectionView?.showSetWallpaperSuccessMessage()
        }, {
          hideCabIfActive()
          collectionView?.removeBlurFromScreen()
          collectionView?.showGenericErrorMessage()
        })
  }

  override fun handleCrystallizeWallpaperMenuItemClicked(
    selectedItemsMap: HashMap<Int, CollectionsPresenterEntity>
  ) {
    collectionImagesUseCase.saveCrystallizedImage(
        collectionImagesPresenterEntityMapper.mapFromPresenterEntity(
            listOf(selectedItemsMap.values.first())).first())
        .observeOn(postExecutionThread.scheduler)
        .doOnSubscribe {
          collectionView?.blurScreen()
          collectionView?.showIndefiniteLoaderWithMessage(
              resourceUtils.getStringResource(R.string.crystallizing_wallpaper_wait_message)
          )
        }.autoDisposable(collectionView!!.getScope())
        .subscribe({
          hideCabIfActive()
          collectionView?.updateChangesInEveryItemView()
          collectionView?.removeBlurFromScreen()
          collectionView?.showCrystallizeWallpaperSuccessMessage()
        }, {
          hideCabIfActive()
          collectionView?.removeBlurFromScreen()
          collectionView?.showGenericErrorMessage()
        })
  }

  override fun handleDeleteWallpaperMenuItemClicked(
    imageList: MutableList<CollectionsPresenterEntity>,
    selectedItemsMap: HashMap<Int, CollectionsPresenterEntity>
  ) {
    val reversedSelectedItems = TreeMap<Int, CollectionsPresenterEntity>(Collections.reverseOrder())
    selectedItemsMap.keys.forEach {
      reversedSelectedItems[it] = selectedItemsMap[it]!!
    }
    mutableListOf<CollectionsPresenterEntity>().let { listOfDeletableImages ->
      reversedSelectedItems.keys.forEach {
        imageList.removeAt(it)
        listOfDeletableImages.add(selectedItemsMap[it]!!)
        selectedItemsMap.remove(it)
        collectionView?.removeItemView(it)
      }
      collectionImagesUseCase.deleteImages(
          collectionImagesPresenterEntityMapper.mapFromPresenterEntity(listOfDeletableImages))
          .map {
            collectionImagesPresenterEntityMapper.mapToPresenterEntity(it)
          }
          .observeOn(postExecutionThread.scheduler)
          .doOnSuccess {
            if (collectionImagesUseCase.isAutomaticWallpaperChangerRunning()) {
              collectionView?.showAutomaticWallpaperStateAsActive()
            } else {
              collectionView?.showAutomaticWallpaperStateAsInActive()
            }
          }
          .autoDisposable(collectionView!!.getScope())
          .subscribe({
            hideCabIfActive()
            collectionView?.setImagesList(it)
            if (it.isEmpty()) {
              showEmptyCollectionView()
            }
            listOfDeletableImages.size.let {
              if (it == SINGLE_ITEM_SIZE) {
                collectionView?.showSingleImageDeleteSuccessMessage()
              } else {
                collectionView?.showMultipleImageDeleteSuccessMessage(it)
              }
            }
          }, {
            hideCabIfActive()
            collectionView?.showUnableToDeleteErrorMessage()
          })
    }
  }

  override fun handleCabDestroyed() {
    collectionView?.clearAllSelectedItems()
    collectionView?.updateChangesInEveryItemView()
    collectionView?.enableToolbar()
    collectionView?.showAppBarWithDelay()
  }

  private fun isUserPremium(): Boolean {
    return if (userPremiumStatusUseCase.isUserPremium()) {
      true
    } else {
      collectionView?.showPurchaseProToContinueDialog()
      false
    }
  }

  private fun isStoragePermissionAvailable(): Boolean {
    return if (collectionView?.hasStoragePermission() == true) {
      true
    } else {
      collectionView?.requestStoragePermission()
      false
    }
  }

  private fun showPictures() {
    collectionImagesUseCase.getAllImages()
        .map {
          collectionImagesPresenterEntityMapper.mapToPresenterEntity(it)
        }
        .observeOn(postExecutionThread.scheduler)
        .autoDisposable(collectionView!!.getScope())
        .subscribe({
          if (it.isNotEmpty()) {
            collectionView?.setImagesList(it)
            showNonEmptyCollectionView()
            showHintIfSuitable(it.size)
            collectionView?.updateChangesInEveryItemView()
          } else {
            showEmptyCollectionView()
          }
        }, {
          collectionView?.showImagesAbsentLayout()
          collectionView?.showGenericErrorMessage()
        })
  }

  private fun showHintIfSuitable(listSize: Int) {
    if (listSize >= MINIMUM_LIST_SIZE_REQUIRED_TO_SHOW_HINT
        && !widgetHintsUseCase.isCollectionsImageReorderHintShown()) {
      collectionView?.showReorderImagesHintWithDelay()
    }
  }

  private fun updateSelectionChangesInCab(position: Int, selectedMapSize: Int) {
    collectionView?.updateChangesInItemView(position)
    if (selectedMapSize > MINIMUM_NUMBER_OF_SELECTED_ITEMS) {
      collectionView?.showMultipleImagesSelectedCab()
    } else if (selectedMapSize == MINIMUM_NUMBER_OF_SELECTED_ITEMS) {
      collectionView?.showSingleImageSelectedCab()
    } else {
      collectionView?.hideCab()
    }
  }

  private fun hideCabIfActive() {
    if (collectionView?.isCabActive() == true) {
      collectionView?.hideCab()
    }
  }

  private fun showEmptyCollectionView() {
    collectionView?.clearImages()
    collectionView?.clearAllSelectedItems()
    collectionView?.showImagesAbsentLayout()
    collectionView?.hideWallpaperChangerLayout()
  }

  private fun showNonEmptyCollectionView() {
    collectionView?.hideImagesAbsentLayout()
    collectionView?.showWallpaperChangerLayout()
  }

}
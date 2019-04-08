package zebrostudio.wallr100.presentation

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import com.uber.autodispose.lifecycle.TestLifecycleScopeProvider
import com.yalantis.ucrop.UCrop
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import zebrostudio.wallr100.R
import zebrostudio.wallr100.android.ui.buypro.PurchaseTransactionConfig
import zebrostudio.wallr100.android.ui.detail.colors.COLORS_DETAIL_MODE_INTENT_EXTRA_TAG
import zebrostudio.wallr100.android.ui.detail.colors.COLORS_DETAIL_MULTIPLE_TYPE_INTENT_EXTRA_TAG
import zebrostudio.wallr100.android.ui.detail.colors.COLORS_HEX_VALUE_LIST_INTENT_EXTRA_TAG
import zebrostudio.wallr100.android.ui.detail.colors.ColorsDetailMode.MULTIPLE
import zebrostudio.wallr100.android.ui.detail.colors.ColorsDetailMode.SINGLE
import zebrostudio.wallr100.android.utils.WallpaperSetter
import zebrostudio.wallr100.data.exception.AlreadyPresentInCollectionException
import zebrostudio.wallr100.domain.executor.PostExecutionThread
import zebrostudio.wallr100.domain.interactor.ColorImagesUseCase
import zebrostudio.wallr100.domain.interactor.UserPremiumStatusUseCase
import zebrostudio.wallr100.domain.model.CollectionsImageModel.EDITED
import zebrostudio.wallr100.domain.model.CollectionsImageModel.MINIMAL_COLOR
import zebrostudio.wallr100.presentation.detail.colors.ColorsActionType.ADD_TO_COLLECTION
import zebrostudio.wallr100.presentation.detail.colors.ColorsActionType.DOWNLOAD
import zebrostudio.wallr100.presentation.detail.colors.ColorsActionType.EDIT_SET
import zebrostudio.wallr100.presentation.detail.colors.ColorsActionType.LOAD_COLOR_WALLPAPER
import zebrostudio.wallr100.presentation.detail.colors.ColorsActionType.QUICK_SET
import zebrostudio.wallr100.presentation.detail.colors.ColorsActionType.SHARE
import zebrostudio.wallr100.presentation.detail.colors.ColorsDetailContract.ColorsDetailView
import zebrostudio.wallr100.presentation.detail.colors.ColorsDetailPresenterImpl
import zebrostudio.wallr100.presentation.minimal.MultiColorImageType.GRADIENT
import zebrostudio.wallr100.presentation.minimal.MultiColorImageType.MATERIAL
import zebrostudio.wallr100.presentation.minimal.MultiColorImageType.PLASMA
import java.util.UUID

@RunWith(MockitoJUnitRunner::class)
class ColorsDetailPresenterImplTest {

  @Mock lateinit var colorImagesUseCase: ColorImagesUseCase
  @Mock lateinit var userPremiumStatusUseCase: UserPremiumStatusUseCase
  @Mock lateinit var wallpaperSetter: WallpaperSetter
  @Mock lateinit var colorsDetailView: ColorsDetailView
  @Mock lateinit var postExecutionThread: PostExecutionThread
  @Mock lateinit var context: Context
  @Mock lateinit var mockBitmap: Bitmap
  @Mock lateinit var mockUri: Uri
  @Mock lateinit var mockIntent: Intent
  private lateinit var colorsDetailPresenterImpl: ColorsDetailPresenterImpl
  private val randomString = UUID.randomUUID().toString()
  private val randomInt = Math.random().toInt()

  @Before fun setup() {
    colorsDetailPresenterImpl =
        ColorsDetailPresenterImpl(context, postExecutionThread, userPremiumStatusUseCase,
            colorImagesUseCase, wallpaperSetter)

    colorsDetailPresenterImpl.attachView(colorsDetailView)

    val testScopeProvider = TestLifecycleScopeProvider.createInitial(
        TestLifecycleScopeProvider.TestLifecycle.STARTED)
    `when`(colorsDetailView.getScope()).thenReturn(testScopeProvider)
    stubPostExecutionThreadReturnsIoScheduler()
  }

  @Test(expected = IllegalStateException::class)
  fun `should throw IllegalStateException on setCalledIntent call failure due to null Intent extras`() {
    `when`(mockIntent.extras).thenReturn(null)

    colorsDetailPresenterImpl.setCalledIntent(mockIntent)

    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should request storage permission on setCalledIntent of type Solid call failure due to no permission`() {
    val list = listOf(randomString)
    val arrayList = ArrayList(list)
    `when`(mockIntent.extras).thenReturn(Bundle())
    `when`(mockIntent.getIntExtra(COLORS_DETAIL_MODE_INTENT_EXTRA_TAG,
        SINGLE.ordinal)).thenReturn(SINGLE.ordinal)
    `when`(mockIntent.getStringArrayListExtra(COLORS_HEX_VALUE_LIST_INTENT_EXTRA_TAG)).thenReturn(
        arrayList)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(false)

    colorsDetailPresenterImpl.setCalledIntent(mockIntent)

    assertEquals(mockIntent, colorsDetailPresenterImpl.intent)
    assertEquals(SINGLE, colorsDetailPresenterImpl.colorsDetailMode)
    assertEquals(arrayList, colorsDetailPresenterImpl.colorList)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).requestStoragePermission(LOAD_COLOR_WALLPAPER)
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should request storage permission on setCalledIntent of type Material call failure due to no permission`() {
    val list = listOf(randomString)
    val arrayList = ArrayList(list)
    `when`(mockIntent.extras).thenReturn(Bundle())
    `when`(mockIntent.getIntExtra(COLORS_DETAIL_MODE_INTENT_EXTRA_TAG,
        SINGLE.ordinal)).thenReturn(MULTIPLE.ordinal)
    `when`(mockIntent.getIntExtra(
        COLORS_DETAIL_MULTIPLE_TYPE_INTENT_EXTRA_TAG,
        MATERIAL.ordinal)).thenReturn(MATERIAL.ordinal)
    `when`(mockIntent.getStringArrayListExtra(COLORS_HEX_VALUE_LIST_INTENT_EXTRA_TAG)).thenReturn(
        arrayList)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(false)

    colorsDetailPresenterImpl.setCalledIntent(mockIntent)

    assertEquals(mockIntent, colorsDetailPresenterImpl.intent)
    assertEquals(MULTIPLE, colorsDetailPresenterImpl.colorsDetailMode)
    assertEquals(MATERIAL, colorsDetailPresenterImpl.multiColorImageType)
    assertEquals(arrayList, colorsDetailPresenterImpl.colorList)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).requestStoragePermission(LOAD_COLOR_WALLPAPER)
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should request storage permission on setCalledIntent of type Gradient call failure due to no permission`() {
    val list = listOf(randomString)
    val arrayList = ArrayList(list)
    `when`(mockIntent.extras).thenReturn(Bundle())
    `when`(mockIntent.getIntExtra(COLORS_DETAIL_MODE_INTENT_EXTRA_TAG,
        SINGLE.ordinal)).thenReturn(MULTIPLE.ordinal)
    `when`(mockIntent.getIntExtra(
        COLORS_DETAIL_MULTIPLE_TYPE_INTENT_EXTRA_TAG,
        MATERIAL.ordinal)).thenReturn(GRADIENT.ordinal)
    `when`(mockIntent.getStringArrayListExtra(COLORS_HEX_VALUE_LIST_INTENT_EXTRA_TAG)).thenReturn(
        arrayList)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(false)

    colorsDetailPresenterImpl.setCalledIntent(mockIntent)

    assertEquals(mockIntent, colorsDetailPresenterImpl.intent)
    assertEquals(MULTIPLE, colorsDetailPresenterImpl.colorsDetailMode)
    assertEquals(GRADIENT, colorsDetailPresenterImpl.multiColorImageType)
    assertEquals(arrayList, colorsDetailPresenterImpl.colorList)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).requestStoragePermission(LOAD_COLOR_WALLPAPER)
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should request storage permission on setCalledIntent of type Plasma call failure due to no permission`() {
    val list = listOf(randomString)
    val arrayList = ArrayList(list)
    `when`(mockIntent.extras).thenReturn(Bundle())
    `when`(mockIntent.getIntExtra(COLORS_DETAIL_MODE_INTENT_EXTRA_TAG,
        SINGLE.ordinal)).thenReturn(MULTIPLE.ordinal)
    `when`(mockIntent.getIntExtra(
        COLORS_DETAIL_MULTIPLE_TYPE_INTENT_EXTRA_TAG,
        MATERIAL.ordinal)).thenReturn(PLASMA.ordinal)
    `when`(mockIntent.getStringArrayListExtra(COLORS_HEX_VALUE_LIST_INTENT_EXTRA_TAG)).thenReturn(
        arrayList)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(false)

    colorsDetailPresenterImpl.setCalledIntent(mockIntent)

    assertEquals(mockIntent, colorsDetailPresenterImpl.intent)
    assertEquals(MULTIPLE, colorsDetailPresenterImpl.colorsDetailMode)
    assertEquals(PLASMA, colorsDetailPresenterImpl.multiColorImageType)
    assertEquals(arrayList, colorsDetailPresenterImpl.colorList)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).requestStoragePermission(LOAD_COLOR_WALLPAPER)
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should set text and show image on setCalledIntent of type Solid call success`() {
    val list = listOf(randomString)
    val arrayList = ArrayList(list)
    `when`(mockIntent.extras).thenReturn(Bundle())
    `when`(mockIntent.getIntExtra(COLORS_DETAIL_MODE_INTENT_EXTRA_TAG,
        SINGLE.ordinal)).thenReturn(SINGLE.ordinal)
    `when`(mockIntent.getStringArrayListExtra(COLORS_HEX_VALUE_LIST_INTENT_EXTRA_TAG)).thenReturn(
        arrayList)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(context.getString(R.string.colors_detail_activity_colors_style_name_solid)).thenReturn(
        randomString)
    `when`(colorImagesUseCase.getSingularColorBitmapSingle(randomString)).thenReturn(
        Single.just(mockBitmap))

    colorsDetailPresenterImpl.setCalledIntent(mockIntent)

    assertEquals(mockIntent, colorsDetailPresenterImpl.intent)
    assertEquals(SINGLE, colorsDetailPresenterImpl.colorsDetailMode)
    assertEquals(arrayList, colorsDetailPresenterImpl.colorList)
    assertEquals(false, colorsDetailPresenterImpl.areColorOperationsDisabled)
    verify(context).getString(R.string.colors_detail_activity_colors_style_name_solid)
    verifyNoMoreInteractions(context)
    verifyImageShown()
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should set text and show image load error on setCalledIntent of type Solid call failure`() {
    val list = listOf(randomString)
    val arrayList = ArrayList(list)
    `when`(mockIntent.extras).thenReturn(Bundle())
    `when`(mockIntent.getIntExtra(COLORS_DETAIL_MODE_INTENT_EXTRA_TAG,
        SINGLE.ordinal)).thenReturn(SINGLE.ordinal)
    `when`(mockIntent.getStringArrayListExtra(COLORS_HEX_VALUE_LIST_INTENT_EXTRA_TAG)).thenReturn(
        arrayList)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(context.getString(R.string.colors_detail_activity_colors_style_name_solid)).thenReturn(
        randomString)
    `when`(colorImagesUseCase.getSingularColorBitmapSingle(randomString)).thenReturn(
        Single.error(Exception()))

    colorsDetailPresenterImpl.setCalledIntent(mockIntent)

    assertEquals(mockIntent, colorsDetailPresenterImpl.intent)
    assertEquals(SINGLE, colorsDetailPresenterImpl.colorsDetailMode)
    assertEquals(arrayList, colorsDetailPresenterImpl.colorList)
    assertEquals(false, colorsDetailPresenterImpl.areColorOperationsDisabled)
    verify(context).getString(R.string.colors_detail_activity_colors_style_name_solid)
    verifyNoMoreInteractions(context)
    verifyImageNotShown()
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should set text and show image on setCalledIntent of type Material call success`() {
    val list = listOf(randomString)
    val arrayList = ArrayList(list)
    `when`(mockIntent.extras).thenReturn(Bundle())
    `when`(mockIntent.getIntExtra(COLORS_DETAIL_MODE_INTENT_EXTRA_TAG,
        SINGLE.ordinal)).thenReturn(MULTIPLE.ordinal)
    `when`(mockIntent.getIntExtra(COLORS_DETAIL_MULTIPLE_TYPE_INTENT_EXTRA_TAG,
        MATERIAL.ordinal)).thenReturn(MATERIAL.ordinal)
    `when`(mockIntent.getStringArrayListExtra(COLORS_HEX_VALUE_LIST_INTENT_EXTRA_TAG)).thenReturn(
        arrayList)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(
        context.getString(R.string.colors_detail_activity_colors_style_name_material)).thenReturn(
        randomString)
    `when`(colorImagesUseCase.getMultiColorMaterialSingle(list,
        MATERIAL)).thenReturn(Single.just(mockBitmap))

    colorsDetailPresenterImpl.setCalledIntent(mockIntent)

    assertEquals(mockIntent, colorsDetailPresenterImpl.intent)
    assertEquals(MULTIPLE, colorsDetailPresenterImpl.colorsDetailMode)
    assertEquals(MATERIAL, colorsDetailPresenterImpl.multiColorImageType)
    assertEquals(arrayList, colorsDetailPresenterImpl.colorList)
    assertEquals(false, colorsDetailPresenterImpl.areColorOperationsDisabled)
    verify(context).getString(R.string.colors_detail_activity_colors_style_name_material)
    verifyNoMoreInteractions(context)
    verifyImageShown()
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should set text and show image load error on setCalledIntent of type Material call failure`() {
    val list = listOf(randomString)
    val arrayList = ArrayList(list)
    `when`(mockIntent.extras).thenReturn(Bundle())
    `when`(mockIntent.getIntExtra(COLORS_DETAIL_MODE_INTENT_EXTRA_TAG,
        SINGLE.ordinal)).thenReturn(MULTIPLE.ordinal)
    `when`(mockIntent.getIntExtra(COLORS_DETAIL_MULTIPLE_TYPE_INTENT_EXTRA_TAG,
        MATERIAL.ordinal)).thenReturn(MATERIAL.ordinal)
    `when`(mockIntent.getStringArrayListExtra(COLORS_HEX_VALUE_LIST_INTENT_EXTRA_TAG)).thenReturn(
        arrayList)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(
        context.getString(R.string.colors_detail_activity_colors_style_name_material)).thenReturn(
        randomString)
    `when`(colorImagesUseCase.getMultiColorMaterialSingle(list,
        MATERIAL)).thenReturn(Single.error(Exception()))

    colorsDetailPresenterImpl.setCalledIntent(mockIntent)

    assertEquals(mockIntent, colorsDetailPresenterImpl.intent)
    assertEquals(MULTIPLE, colorsDetailPresenterImpl.colorsDetailMode)
    assertEquals(MATERIAL, colorsDetailPresenterImpl.multiColorImageType)
    assertEquals(arrayList, colorsDetailPresenterImpl.colorList)
    assertEquals(false, colorsDetailPresenterImpl.areColorOperationsDisabled)
    verify(context).getString(R.string.colors_detail_activity_colors_style_name_material)
    verifyNoMoreInteractions(context)
    verifyImageNotShown()
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should set text and show image on setCalledIntent of type Gradient call success`() {
    val list = listOf(randomString)
    val arrayList = ArrayList(list)
    `when`(mockIntent.extras).thenReturn(Bundle())
    `when`(mockIntent.getIntExtra(COLORS_DETAIL_MODE_INTENT_EXTRA_TAG,
        SINGLE.ordinal)).thenReturn(MULTIPLE.ordinal)
    `when`(mockIntent.getIntExtra(COLORS_DETAIL_MULTIPLE_TYPE_INTENT_EXTRA_TAG,
        MATERIAL.ordinal)).thenReturn(GRADIENT.ordinal)
    `when`(mockIntent.getStringArrayListExtra(COLORS_HEX_VALUE_LIST_INTENT_EXTRA_TAG)).thenReturn(
        arrayList)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(
        context.getString(R.string.colors_detail_activity_colors_style_name_gradient)).thenReturn(
        randomString)
    `when`(colorImagesUseCase.getMultiColorMaterialSingle(list,
        GRADIENT)).thenReturn(Single.just(mockBitmap))

    colorsDetailPresenterImpl.setCalledIntent(mockIntent)

    assertEquals(mockIntent, colorsDetailPresenterImpl.intent)
    assertEquals(MULTIPLE, colorsDetailPresenterImpl.colorsDetailMode)
    assertEquals(GRADIENT, colorsDetailPresenterImpl.multiColorImageType)
    assertEquals(arrayList, colorsDetailPresenterImpl.colorList)
    assertEquals(false, colorsDetailPresenterImpl.areColorOperationsDisabled)
    verify(context).getString(R.string.colors_detail_activity_colors_style_name_gradient)
    verifyNoMoreInteractions(context)
    verifyImageShown()
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should set text and show image load error on setCalledIntent of type gradient call failure`() {
    val list = listOf(randomString)
    val arrayList = ArrayList(list)
    `when`(mockIntent.extras).thenReturn(Bundle())
    `when`(mockIntent.getIntExtra(COLORS_DETAIL_MODE_INTENT_EXTRA_TAG,
        SINGLE.ordinal)).thenReturn(MULTIPLE.ordinal)
    `when`(mockIntent.getIntExtra(COLORS_DETAIL_MULTIPLE_TYPE_INTENT_EXTRA_TAG,
        MATERIAL.ordinal)).thenReturn(GRADIENT.ordinal)
    `when`(mockIntent.getStringArrayListExtra(COLORS_HEX_VALUE_LIST_INTENT_EXTRA_TAG)).thenReturn(
        arrayList)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(
        context.getString(R.string.colors_detail_activity_colors_style_name_gradient)).thenReturn(
        randomString)
    `when`(colorImagesUseCase.getMultiColorMaterialSingle(list,
        GRADIENT)).thenReturn(Single.error(Exception()))

    colorsDetailPresenterImpl.setCalledIntent(mockIntent)

    assertEquals(mockIntent, colorsDetailPresenterImpl.intent)
    assertEquals(MULTIPLE, colorsDetailPresenterImpl.colorsDetailMode)
    assertEquals(GRADIENT, colorsDetailPresenterImpl.multiColorImageType)
    assertEquals(arrayList, colorsDetailPresenterImpl.colorList)
    assertEquals(false, colorsDetailPresenterImpl.areColorOperationsDisabled)
    verify(context).getString(R.string.colors_detail_activity_colors_style_name_gradient)
    verifyNoMoreInteractions(context)
    verifyImageNotShown()
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should set text and show image on setCalledIntent of type Plasma call success`() {
    val list = listOf(randomString)
    val arrayList = ArrayList(list)
    `when`(mockIntent.extras).thenReturn(Bundle())
    `when`(mockIntent.getIntExtra(COLORS_DETAIL_MODE_INTENT_EXTRA_TAG,
        SINGLE.ordinal)).thenReturn(MULTIPLE.ordinal)
    `when`(mockIntent.getIntExtra(COLORS_DETAIL_MULTIPLE_TYPE_INTENT_EXTRA_TAG,
        MATERIAL.ordinal)).thenReturn(PLASMA.ordinal)
    `when`(mockIntent.getStringArrayListExtra(COLORS_HEX_VALUE_LIST_INTENT_EXTRA_TAG)).thenReturn(
        arrayList)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(
        context.getString(R.string.colors_detail_activity_colors_style_name_plasma)).thenReturn(
        randomString)
    `when`(colorImagesUseCase.getMultiColorMaterialSingle(list,
        PLASMA)).thenReturn(Single.just(mockBitmap))

    colorsDetailPresenterImpl.setCalledIntent(mockIntent)

    assertEquals(mockIntent, colorsDetailPresenterImpl.intent)
    assertEquals(MULTIPLE, colorsDetailPresenterImpl.colorsDetailMode)
    assertEquals(PLASMA, colorsDetailPresenterImpl.multiColorImageType)
    assertEquals(arrayList, colorsDetailPresenterImpl.colorList)
    assertEquals(false, colorsDetailPresenterImpl.areColorOperationsDisabled)
    verify(context).getString(R.string.colors_detail_activity_colors_style_name_plasma)
    verifyNoMoreInteractions(context)
    verifyImageShown()
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should set text and show image load error on setCalledIntent of type Plasma call failure`() {
    val list = listOf(randomString)
    val arrayList = ArrayList(list)
    `when`(mockIntent.extras).thenReturn(Bundle())
    `when`(mockIntent.getIntExtra(COLORS_DETAIL_MODE_INTENT_EXTRA_TAG,
        SINGLE.ordinal)).thenReturn(MULTIPLE.ordinal)
    `when`(mockIntent.getIntExtra(COLORS_DETAIL_MULTIPLE_TYPE_INTENT_EXTRA_TAG,
        MATERIAL.ordinal)).thenReturn(PLASMA.ordinal)
    `when`(mockIntent.getStringArrayListExtra(COLORS_HEX_VALUE_LIST_INTENT_EXTRA_TAG)).thenReturn(
        arrayList)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(
        context.getString(R.string.colors_detail_activity_colors_style_name_plasma)).thenReturn(
        randomString)
    `when`(colorImagesUseCase.getMultiColorMaterialSingle(list,
        PLASMA)).thenReturn(Single.error(Exception()))

    colorsDetailPresenterImpl.setCalledIntent(mockIntent)

    assertEquals(mockIntent, colorsDetailPresenterImpl.intent)
    assertEquals(MULTIPLE, colorsDetailPresenterImpl.colorsDetailMode)
    assertEquals(PLASMA, colorsDetailPresenterImpl.multiColorImageType)
    assertEquals(arrayList, colorsDetailPresenterImpl.colorList)
    assertEquals(false, colorsDetailPresenterImpl.areColorOperationsDisabled)
    verify(context).getString(R.string.colors_detail_activity_colors_style_name_plasma)
    verifyNoMoreInteractions(context)
    verifyImageNotShown()
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should set text and show image on handlePermissionRequestResult call success with request code Load color wallpaper`() {
    val list = listOf(randomString)
    val arrayList = ArrayList(list)
    colorsDetailPresenterImpl.intent = mockIntent
    `when`(mockIntent.extras).thenReturn(Bundle())
    `when`(mockIntent.getIntExtra(COLORS_DETAIL_MODE_INTENT_EXTRA_TAG,
        SINGLE.ordinal)).thenReturn(MULTIPLE.ordinal)
    `when`(mockIntent.getIntExtra(COLORS_DETAIL_MULTIPLE_TYPE_INTENT_EXTRA_TAG,
        MATERIAL.ordinal)).thenReturn(MATERIAL.ordinal)
    `when`(mockIntent.getStringArrayListExtra(COLORS_HEX_VALUE_LIST_INTENT_EXTRA_TAG)).thenReturn(
        arrayList)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(
        context.getString(R.string.colors_detail_activity_colors_style_name_material)).thenReturn(
        randomString)
    `when`(colorImagesUseCase.getMultiColorMaterialSingle(list,
        MATERIAL)).thenReturn(Single.just(mockBitmap))

    colorsDetailPresenterImpl.handlePermissionRequestResult(LOAD_COLOR_WALLPAPER.ordinal,
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE),
        intArrayOf(PackageManager.PERMISSION_GRANTED))

    assertEquals(mockIntent, colorsDetailPresenterImpl.intent)
    assertEquals(MULTIPLE, colorsDetailPresenterImpl.colorsDetailMode)
    assertEquals(MATERIAL, colorsDetailPresenterImpl.multiColorImageType)
    assertEquals(arrayList, colorsDetailPresenterImpl.colorList)
    assertEquals(false, colorsDetailPresenterImpl.areColorOperationsDisabled)
    verify(context).getString(R.string.colors_detail_activity_colors_style_name_material)
    verifyNoMoreInteractions(context)
    verifyImageShown()
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should show permission required and exit view on handlePermissionRequestResult call failure due to permission denied with load color wallpaper request code`() {
    colorsDetailPresenterImpl.handlePermissionRequestResult(LOAD_COLOR_WALLPAPER.ordinal,
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE),
        intArrayOf(PackageManager.PERMISSION_DENIED))

    verify(colorsDetailView).exitView()
    verify(colorsDetailView).showPermissionRequiredMessage()
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test fun `should set panel expanded to true on setPanelStateAsExpanded call success`() {
    colorsDetailPresenterImpl.setPanelStateAsExpanded()

    assertTrue(colorsDetailPresenterImpl.isPanelExpanded)
  }

  @Test fun `should set panel expanded to false on setPanelStateAsCollapsed call success`() {
    colorsDetailPresenterImpl.setPanelStateAsCollapsed()

    assertFalse(colorsDetailPresenterImpl.isPanelExpanded)
  }

  @Test
  fun `should collapse panel on handleImageViewClicked call success and panel was expanded`() {
    colorsDetailPresenterImpl.isPanelExpanded = true

    colorsDetailPresenterImpl.handleImageViewClicked()

    verify(colorsDetailView).collapsePanel()
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should show full screen image on handleImageViewClicked call success and panel was expanded`() {
    colorsDetailPresenterImpl.isPanelExpanded = false

    colorsDetailPresenterImpl.handleImageViewClicked()

    verify(colorsDetailView).showFullScreenImage()
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should show operation disabled message on handleQuickSetClick call failure due to image still being loaded`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = true

    colorsDetailPresenterImpl.handleQuickSetClick()

    verify(colorsDetailView).showColorOperationsDisabledMessage()
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should request storage permission on handleQuickSetClick call failure due to missing permission`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(false)

    colorsDetailPresenterImpl.handleQuickSetClick()

    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).requestStoragePermission(QUICK_SET)
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should show wallpaper set error message on handleQuickSetClick call failure`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(colorImagesUseCase.getBitmapSingle()).thenReturn(Single.error(java.lang.Exception()))
    `when`(context.getString(R.string.finalizing_wallpaper_messsage)).thenReturn(randomString)

    colorsDetailPresenterImpl.handleQuickSetClick()

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verify(colorsDetailView).showWallpaperSetErrorMessage()
    verifyNoMoreInteractions(colorsDetailView)
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should set wallpaper on handleQuickSetClick call success`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(colorImagesUseCase.getBitmapSingle()).thenReturn(Single.just(mockBitmap))
    `when`(context.getString(R.string.finalizing_wallpaper_messsage)).thenReturn(randomString)

    colorsDetailPresenterImpl.handleQuickSetClick()

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verify(colorsDetailView).showWallpaperSetSuccessMessage()
    verifyNoMoreInteractions(colorsDetailView)
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should set wallpaper on handlePermissionRequestResult call success with quick set request code`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(colorImagesUseCase.getBitmapSingle()).thenReturn(Single.just(mockBitmap))
    `when`(context.getString(R.string.finalizing_wallpaper_messsage)).thenReturn(randomString)

    colorsDetailPresenterImpl.handlePermissionRequestResult(QUICK_SET.ordinal,
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE),
        intArrayOf(PackageManager.PERMISSION_GRANTED))

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verify(colorsDetailView).showWallpaperSetSuccessMessage()
    verifyNoMoreInteractions(colorsDetailView)
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should show permission required on handlePermissionRequestResult call failure due to permission denied with quick set request code`() {
    colorsDetailPresenterImpl.handlePermissionRequestResult(QUICK_SET.ordinal,
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE),
        intArrayOf(PackageManager.PERMISSION_DENIED))

    verify(colorsDetailView).showPermissionRequiredMessage()
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should show operation disabled message on handleDownloadClick call failure due to image still being loaded`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = true

    colorsDetailPresenterImpl.handleDownloadClick()

    verify(colorsDetailView).showColorOperationsDisabledMessage()
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should redirect to buy pro on handleDownloadClick call failure due to non pro user`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(false)

    colorsDetailPresenterImpl.handleDownloadClick()

    verify(userPremiumStatusUseCase).isUserPremium()
    verifyNoMoreInteractions(userPremiumStatusUseCase)
    verify(colorsDetailView).redirectToBuyPro(DOWNLOAD.ordinal)
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should request storage permission on handleDownloadClick call failure due to missing permission`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(true)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(false)

    colorsDetailPresenterImpl.handleDownloadClick()


    verify(userPremiumStatusUseCase).isUserPremium()
    verifyNoMoreInteractions(userPremiumStatusUseCase)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).requestStoragePermission(DOWNLOAD)
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test fun `should show error message on handleDownloadCLick call failure`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(true)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(colorImagesUseCase.downloadImage()).thenReturn(Completable.error(Exception()))
    `when`(context.getString(R.string.detail_activity_crystallizing_wallpaper_please_wait_message))
        .thenReturn(randomString)

    colorsDetailPresenterImpl.handleDownloadClick()

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(userPremiumStatusUseCase).isUserPremium()
    verifyNoMoreInteractions(userPremiumStatusUseCase)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verify(colorsDetailView).showGenericErrorMessage()
    verifyNoMoreInteractions(colorsDetailView)
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should download wallpaper on handlePermissionRequestResult call success with download request code`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(true)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(colorImagesUseCase.downloadImage()).thenReturn(Completable.error(Exception()))
    `when`(context.getString(R.string.detail_activity_crystallizing_wallpaper_please_wait_message))
        .thenReturn(randomString)

    colorsDetailPresenterImpl.handlePermissionRequestResult(DOWNLOAD.ordinal,
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE),
        intArrayOf(PackageManager.PERMISSION_GRANTED))

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(userPremiumStatusUseCase).isUserPremium()
    verifyNoMoreInteractions(userPremiumStatusUseCase)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verify(colorsDetailView).showGenericErrorMessage()
    verifyNoMoreInteractions(colorsDetailView)
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should show permission required on handlePermissionRequestResult call failure due to permission denied with download request code`() {
    colorsDetailPresenterImpl.handlePermissionRequestResult(DOWNLOAD.ordinal,
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE),
        intArrayOf(PackageManager.PERMISSION_DENIED))

    verify(colorsDetailView).showPermissionRequiredMessage()
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should show unsuccessful purchase error on handleViewResult call failure with download request code`() {
    colorsDetailPresenterImpl.handleViewResult(DOWNLOAD.ordinal, Activity.RESULT_CANCELED,
        mockIntent)

    verify(colorsDetailView).showUnsuccessfulPurchaseError()
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should download wallpaper on handleViewResult call success with download request code`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(true)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(colorImagesUseCase.downloadImage()).thenReturn(Completable.error(Exception()))
    `when`(context.getString(R.string.detail_activity_crystallizing_wallpaper_please_wait_message))
        .thenReturn(randomString)

    colorsDetailPresenterImpl.handleViewResult(DOWNLOAD.ordinal,
        PurchaseTransactionConfig.PURCHASE_SUCCESSFUL_RESULT_CODE, mockIntent)

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(userPremiumStatusUseCase).isUserPremium()
    verifyNoMoreInteractions(userPremiumStatusUseCase)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verify(colorsDetailView).showGenericErrorMessage()
    verifyNoMoreInteractions(colorsDetailView)
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should show operation disabled message on handleEditSetClick call failure due to image still being loaded`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = true

    colorsDetailPresenterImpl.handleEditSetClick()

    verify(colorsDetailView).showColorOperationsDisabledMessage()
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should request storage permission on handleEditSetClick call failure due to missing permission`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(false)

    colorsDetailPresenterImpl.handleEditSetClick()

    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).requestStoragePermission(EDIT_SET)
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test fun `should start cropping activity on handleEditSetClick call success`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(context.getString(R.string.detail_activity_editing_tool_message)).thenReturn(
        randomString)
    `when`(colorImagesUseCase.getCacheSourceUri()).thenReturn(mockUri)
    `when`(colorImagesUseCase.getCroppingDestinationUri()).thenReturn(mockUri)
    `when`(wallpaperSetter.getDesiredMinimumWidth()).thenReturn(randomInt)
    `when`(wallpaperSetter.getDesiredMinimumHeight()).thenReturn(randomInt)

    colorsDetailPresenterImpl.handleEditSetClick()

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).startCroppingActivity(
        colorImagesUseCase.getCacheSourceUri(),
        colorImagesUseCase.getCroppingDestinationUri(),
        wallpaperSetter.getDesiredMinimumWidth(),
        wallpaperSetter.getDesiredMinimumHeight())
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should show permission required on handlePermissionRequestResult call failure due to permission denied with edit set request code`() {
    colorsDetailPresenterImpl.handlePermissionRequestResult(EDIT_SET.ordinal,
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE),
        intArrayOf(PackageManager.PERMISSION_DENIED))

    verify(colorsDetailView).showPermissionRequiredMessage()
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test fun `should show error on handleViewResult call failure due to null crop uri`() {
    `when`(colorsDetailView.getUriFromIntent(mockIntent)).thenReturn(null)

    colorsDetailPresenterImpl.handleViewResult(UCrop.REQUEST_CROP, Activity.RESULT_OK, mockIntent)

    verify(colorsDetailView).getUriFromIntent(mockIntent)
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verify(colorsDetailView).showGenericErrorMessage()
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test fun `should show error on handleViewResult call failure of request type crop`() {
    `when`(colorsDetailView.getUriFromIntent(mockIntent)).thenReturn(mockUri)
    `when`(context.getString(R.string.finalizing_wallpaper_messsage)).thenReturn(randomString)
    `when`(colorImagesUseCase.getBitmapFromUriSingle(mockUri)).thenReturn(Single.error(Exception()))

    colorsDetailPresenterImpl.handleViewResult(UCrop.REQUEST_CROP, Activity.RESULT_OK, mockIntent)

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(colorsDetailView).getUriFromIntent(mockIntent)
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verify(colorsDetailView).showGenericErrorMessage()
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should show unable to set wallpaper error on handleViewResult call success of request type crop`() {
    `when`(colorsDetailView.getUriFromIntent(mockIntent)).thenReturn(mockUri)
    `when`(context.getString(R.string.finalizing_wallpaper_messsage)).thenReturn(randomString)
    `when`(colorImagesUseCase.getBitmapFromUriSingle(mockUri)).thenReturn(Single.just(mockBitmap))
    `when`(wallpaperSetter.setWallpaper(mockBitmap)).thenReturn(false)

    colorsDetailPresenterImpl.handleViewResult(UCrop.REQUEST_CROP, Activity.RESULT_OK, mockIntent)

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    assertEquals(EDITED, colorsDetailPresenterImpl.lastImageOperationType)
    verify(colorsDetailView).getUriFromIntent(mockIntent)
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).showImage(mockBitmap)
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verify(colorsDetailView).showWallpaperSetErrorMessage()
    verifyNoMoreInteractions(colorsDetailView)
    verify(wallpaperSetter).setWallpaper(mockBitmap)
    verifyNoMoreInteractions(wallpaperSetter)
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test fun `should set wallpaper on handleViewResult call success of request type crop`() {
    `when`(colorsDetailView.getUriFromIntent(mockIntent)).thenReturn(mockUri)
    `when`(context.getString(R.string.finalizing_wallpaper_messsage)).thenReturn(randomString)
    `when`(colorImagesUseCase.getBitmapFromUriSingle(mockUri)).thenReturn(Single.just(mockBitmap))
    `when`(wallpaperSetter.setWallpaper(mockBitmap)).thenReturn(true)

    colorsDetailPresenterImpl.handleViewResult(UCrop.REQUEST_CROP, Activity.RESULT_OK, mockIntent)

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    assertEquals(EDITED, colorsDetailPresenterImpl.lastImageOperationType)
    verify(colorsDetailView).getUriFromIntent(mockIntent)
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).showImage(mockBitmap)
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verify(colorsDetailView).showWallpaperSetSuccessMessage()
    verifyNoMoreInteractions(colorsDetailView)
    verify(wallpaperSetter).setWallpaper(mockBitmap)
    verifyNoMoreInteractions(wallpaperSetter)
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should show operation disabled message on handleAddToCollectionClick call failure due to image still being loaded`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = true

    colorsDetailPresenterImpl.handleAddToCollectionClick()

    verify(colorsDetailView).showColorOperationsDisabledMessage()
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should redirect to buy pro on handleAddToCollectionClick call failure due to non pro user`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(false)

    colorsDetailPresenterImpl.handleAddToCollectionClick()

    verify(userPremiumStatusUseCase).isUserPremium()
    verifyNoMoreInteractions(userPremiumStatusUseCase)
    verify(colorsDetailView).redirectToBuyPro(ADD_TO_COLLECTION.ordinal)
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should request storage permission on handleAddToCollectionClick call failure due to missing permission`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(true)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(false)

    colorsDetailPresenterImpl.handleAddToCollectionClick()


    verify(userPremiumStatusUseCase).isUserPremium()
    verifyNoMoreInteractions(userPremiumStatusUseCase)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).requestStoragePermission(ADD_TO_COLLECTION)
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should show generic error on handleAddToCollectionClick call failure`() {
    val list = mutableListOf(randomString)
    colorsDetailPresenterImpl.colorList = list
    colorsDetailPresenterImpl.lastImageOperationType = MINIMAL_COLOR
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(true)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(colorImagesUseCase.saveToCollectionsCompletable(list.toString(),
        MINIMAL_COLOR)).thenReturn(
        Completable.error(Exception()))
    `when`(context.getString(R.string.adding_image_to_collections_message)).thenReturn(randomString)

    colorsDetailPresenterImpl.handleAddToCollectionClick()

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(userPremiumStatusUseCase).isUserPremium()
    verifyNoMoreInteractions(userPremiumStatusUseCase)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).showGenericErrorMessage()
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verifyNoMoreInteractions(colorsDetailView)
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should show already present error in collection message on handleAddToCollectionClick call failure`() {
    val list = mutableListOf(randomString)
    colorsDetailPresenterImpl.colorList = list
    colorsDetailPresenterImpl.lastImageOperationType = MINIMAL_COLOR
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(true)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(colorImagesUseCase.saveToCollectionsCompletable(list.toString(),
        MINIMAL_COLOR)).thenReturn(
        Completable.error(AlreadyPresentInCollectionException()))
    `when`(context.getString(R.string.adding_image_to_collections_message)).thenReturn(randomString)

    colorsDetailPresenterImpl.handleAddToCollectionClick()

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(userPremiumStatusUseCase).isUserPremium()
    verifyNoMoreInteractions(userPremiumStatusUseCase)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).showAlreadyPresentInCollectionErrorMessage()
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verifyNoMoreInteractions(colorsDetailView)
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should add to collection on handleAddToCollectionClick call success`() {
    val list = mutableListOf(randomString)
    colorsDetailPresenterImpl.colorList = list
    colorsDetailPresenterImpl.lastImageOperationType = MINIMAL_COLOR
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(true)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(colorImagesUseCase.saveToCollectionsCompletable(list.toString(),
        MINIMAL_COLOR)).thenReturn(
        Completable.complete())
    `when`(context.getString(R.string.adding_image_to_collections_message)).thenReturn(randomString)

    colorsDetailPresenterImpl.handleAddToCollectionClick()

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(userPremiumStatusUseCase).isUserPremium()
    verifyNoMoreInteractions(userPremiumStatusUseCase)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).showAddToCollectionSuccessMessage()
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verifyNoMoreInteractions(colorsDetailView)
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should show unsuccessful purchase error on handleViewResult call failure with add to collection request codee`() {
    colorsDetailPresenterImpl.handleViewResult(ADD_TO_COLLECTION.ordinal, Activity.RESULT_CANCELED,
        mockIntent)

    verify(colorsDetailView).showUnsuccessfulPurchaseError()
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should add to collection on handleViewResult call success with add to collection request code`() {
    val list = mutableListOf(randomString)
    colorsDetailPresenterImpl.colorList = list
    colorsDetailPresenterImpl.lastImageOperationType = MINIMAL_COLOR
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(true)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(colorImagesUseCase.saveToCollectionsCompletable(list.toString(),
        MINIMAL_COLOR)).thenReturn(
        Completable.complete())
    `when`(context.getString(R.string.adding_image_to_collections_message)).thenReturn(randomString)

    colorsDetailPresenterImpl.handleViewResult(ADD_TO_COLLECTION.ordinal,
        PurchaseTransactionConfig.PURCHASE_SUCCESSFUL_RESULT_CODE, mockIntent)

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(userPremiumStatusUseCase).isUserPremium()
    verifyNoMoreInteractions(userPremiumStatusUseCase)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).showAddToCollectionSuccessMessage()
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verifyNoMoreInteractions(colorsDetailView)
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should add to collection on handlePermissionRequestResult call success with add to collection request code`() {
    val list = mutableListOf(randomString)
    colorsDetailPresenterImpl.colorList = list
    colorsDetailPresenterImpl.lastImageOperationType = MINIMAL_COLOR
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(true)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(colorImagesUseCase.saveToCollectionsCompletable(list.toString(),
        MINIMAL_COLOR)).thenReturn(
        Completable.complete())
    `when`(context.getString(R.string.adding_image_to_collections_message)).thenReturn(randomString)

    colorsDetailPresenterImpl.handlePermissionRequestResult(ADD_TO_COLLECTION.ordinal,
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE),
        intArrayOf(PackageManager.PERMISSION_GRANTED))

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(userPremiumStatusUseCase).isUserPremium()
    verifyNoMoreInteractions(userPremiumStatusUseCase)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).showAddToCollectionSuccessMessage()
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verifyNoMoreInteractions(colorsDetailView)
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should show permission required on handlePermissionRequestResult call failure due to permission denied with add to collection request code`() {
    colorsDetailPresenterImpl.handlePermissionRequestResult(ADD_TO_COLLECTION.ordinal,
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE),
        intArrayOf(PackageManager.PERMISSION_DENIED))

    verify(colorsDetailView).showPermissionRequiredMessage()
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should show operation disabled message on handleShareClick call failure due to image still being loaded`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = true

    colorsDetailPresenterImpl.handleShareClick()

    verify(colorsDetailView).showColorOperationsDisabledMessage()
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should redirect to buy pro on handleShareClick call failure due to non pro user`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(false)

    colorsDetailPresenterImpl.handleShareClick()

    verify(userPremiumStatusUseCase).isUserPremium()
    verifyNoMoreInteractions(userPremiumStatusUseCase)
    verify(colorsDetailView).redirectToBuyPro(SHARE.ordinal)
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should request storage permission on handleShareClick call failure due to missing permission`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(true)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(false)

    colorsDetailPresenterImpl.handleShareClick()


    verify(userPremiumStatusUseCase).isUserPremium()
    verifyNoMoreInteractions(userPremiumStatusUseCase)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).requestStoragePermission(SHARE)
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test fun `should show generic error on handleShareClick call failure`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(true)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(colorImagesUseCase.getCacheImageUri()).thenReturn(Single.error(Exception()))
    `when`(context.getString(R.string.preparing_shareable_wallpaper_message)).thenReturn(
        randomString)

    colorsDetailPresenterImpl.handleShareClick()

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(userPremiumStatusUseCase).isUserPremium()
    verifyNoMoreInteractions(userPremiumStatusUseCase)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verify(colorsDetailView).showGenericErrorMessage()
    verifyNoMoreInteractions(colorsDetailView)
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test fun `should share image on handleShareClick call success`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(true)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(colorImagesUseCase.getCacheImageUri()).thenReturn(Single.just(mockUri))
    `when`(context.getString(R.string.preparing_shareable_wallpaper_message)).thenReturn(
        randomString)

    colorsDetailPresenterImpl.handleShareClick()

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(userPremiumStatusUseCase).isUserPremium()
    verifyNoMoreInteractions(userPremiumStatusUseCase)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).showShareIntent(mockUri)
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verifyNoMoreInteractions(colorsDetailView)
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should show unsuccessful purchase error on handleViewResult call failure with share request codee`() {
    colorsDetailPresenterImpl.handleViewResult(SHARE.ordinal, Activity.RESULT_CANCELED,
        mockIntent)

    verify(colorsDetailView).showUnsuccessfulPurchaseError()
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should add to collection on handleViewResult call success with add to share request code`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(true)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(colorImagesUseCase.getCacheImageUri()).thenReturn(Single.just(mockUri))
    `when`(context.getString(R.string.preparing_shareable_wallpaper_message)).thenReturn(
        randomString)

    colorsDetailPresenterImpl.handleViewResult(SHARE.ordinal,
        PurchaseTransactionConfig.PURCHASE_SUCCESSFUL_RESULT_CODE, mockIntent)

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(userPremiumStatusUseCase).isUserPremium()
    verifyNoMoreInteractions(userPremiumStatusUseCase)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).showShareIntent(mockUri)
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verifyNoMoreInteractions(colorsDetailView)
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should add to collection on handlePermissionRequestResult call success with share request code`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(true)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(colorImagesUseCase.getCacheImageUri()).thenReturn(Single.just(mockUri))
    `when`(context.getString(R.string.preparing_shareable_wallpaper_message)).thenReturn(
        randomString)

    colorsDetailPresenterImpl.handlePermissionRequestResult(SHARE.ordinal,
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE),
        intArrayOf(PackageManager.PERMISSION_GRANTED))

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(userPremiumStatusUseCase).isUserPremium()
    verifyNoMoreInteractions(userPremiumStatusUseCase)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).showShareIntent(mockUri)
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verifyNoMoreInteractions(colorsDetailView)
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should show permission required on handlePermissionRequestResult call failure due to permission denied with share request code`() {
    colorsDetailPresenterImpl.handlePermissionRequestResult(SHARE.ordinal,
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE),
        intArrayOf(PackageManager.PERMISSION_DENIED))

    verify(colorsDetailView).showPermissionRequiredMessage()
    verifyNoMoreInteractions(colorsDetailView)
  }

  @After fun tearDown() {
    colorsDetailPresenterImpl.detachView()
  }

  private fun stubPostExecutionThreadReturnsIoScheduler() {
    whenever(postExecutionThread.scheduler).thenReturn(Schedulers.trampoline())
  }

  private fun verifyPostExecutionThreadSchedulerCall(times: Int = 1) {
    verify(postExecutionThread, times(times)).scheduler
    verifyNoMoreInteractions(postExecutionThread)
  }

  private fun verifyImageShown() {
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).showImageTypeText(randomString)
    verify(colorsDetailView).disableColorOperations()
    verify(colorsDetailView).showMainImageWaitLoader()
    verify(colorsDetailView).showImage(mockBitmap)
    verify(colorsDetailView).hideMainImageWaitLoader()
    verify(colorsDetailView).enableColorOperations()
    verifyNoMoreInteractions(colorsDetailView)
  }

  private fun verifyImageNotShown() {
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).showImageTypeText(randomString)
    verify(colorsDetailView).disableColorOperations()
    verify(colorsDetailView).showMainImageWaitLoader()
    verify(colorsDetailView).showImageLoadError()
    verify(colorsDetailView).enableColorOperations()
    verifyNoMoreInteractions(colorsDetailView)
  }
}
package zebrostudio.wallr100.presentation

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.uber.autodispose.lifecycle.TestLifecycleScopeProvider
import com.uber.autodispose.lifecycle.TestLifecycleScopeProvider.TestLifecycle.*
import io.reactivex.Completable
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import zebrostudio.wallr100.android.ui.buypro.PremiumTransactionType.*
import zebrostudio.wallr100.data.exception.InvalidPurchaseException
import zebrostudio.wallr100.data.exception.UnableToVerifyPurchaseException
import zebrostudio.wallr100.domain.interactor.AuthenticatePurchaseUseCase
import zebrostudio.wallr100.domain.interactor.UserPremiumStatusUseCase
import zebrostudio.wallr100.presentation.buypro.BuyProContract
import zebrostudio.wallr100.presentation.buypro.BuyProPresenterImpl
import java.lang.Exception
import java.util.UUID

@RunWith(MockitoJUnitRunner::class)
class BuyProPresenterImplTest {

  @Mock lateinit var buyProView: BuyProContract.BuyProView
  @Mock lateinit var authenticatePurchaseUseCase: AuthenticatePurchaseUseCase
  @Mock lateinit var userPremiumStatusUseCase: UserPremiumStatusUseCase
  private lateinit var buyProPresenterImpl: BuyProPresenterImpl
  private lateinit var testScopeProvider: TestLifecycleScopeProvider
  private val packageName = UUID.randomUUID().toString()
  private val skuId = UUID.randomUUID().toString()
  private val purchaseToken = UUID.randomUUID().toString()

  @Before fun setup() {
    buyProPresenterImpl = BuyProPresenterImpl(authenticatePurchaseUseCase, userPremiumStatusUseCase)
    buyProPresenterImpl.attachView(buyProView)
    testScopeProvider = TestLifecycleScopeProvider.createInitial(STARTED)

    Mockito.`when`(buyProView.getScope()).thenReturn(testScopeProvider)
  }

  @Test fun `should showGenericVerificationError if iab not ready and purchase clicked`() {
    stubIabNotReady()
    buyProPresenterImpl.notifyPurchaseClicked()

    verify(buyProView).isIabReady()
    verify(buyProView).showGenericVerificationError()
    verifyNoMoreInteractions(buyProView)
  }

  @Test
  fun `should showNoInternetErrorMessage if iab ready and purchase clicked but no internet`() {
    stubIabReady()
    stubInternetNotAvailable()
    buyProPresenterImpl.notifyPurchaseClicked()

    verify(buyProView).isIabReady()
    verify(buyProView).isInternetAvailable()
    verify(buyProView).showNoInternetErrorMessage(PURCHASE)
    verifyNoMoreInteractions(buyProView)
  }

  @Test
  fun `should launchPurchase if iab ready and purchase clicked and internet available`() {
    stubIabReady()
    stubInternetAvailable()
    buyProPresenterImpl.notifyPurchaseClicked()

    verify(buyProView).isIabReady()
    verify(buyProView).isInternetAvailable()
    verify(buyProView).showWaitLoader(PURCHASE)
    verify(buyProView).launchPurchase()
    verifyNoMoreInteractions(buyProView)
  }

  @Test fun `should showGenericVerificationError if iab not ready and restore clicked`() {
    stubIabNotReady()
    buyProPresenterImpl.notifyRestoreClicked()

    verify(buyProView).isIabReady()
    verify(buyProView).showGenericVerificationError()
    verifyNoMoreInteractions(buyProView)
  }

  @Test fun `should showNoInternetErrorMessage if iab ready and restore clicked but no internet`() {
    stubIabReady()
    stubInternetNotAvailable()
    buyProPresenterImpl.notifyRestoreClicked()

    verify(buyProView).isIabReady()
    verify(buyProView).isInternetAvailable()
    verify(buyProView).showNoInternetErrorMessage(RESTORE)
    verifyNoMoreInteractions(buyProView)
  }

  @Test
  fun `should launchRestore if iab ready and restore clicked and internet available`() {
    stubIabReady()
    stubInternetAvailable()
    buyProPresenterImpl.notifyRestoreClicked()

    verify(buyProView).isIabReady()
    verify(buyProView).isInternetAvailable()
    verify(buyProView).showWaitLoader(RESTORE)
    verify(buyProView).launchRestore()
    verifyNoMoreInteractions(buyProView)
  }

  @Test
  fun `should showSuccessfulTransactionMessage and finishWithResult on purchase verification success`() {
    stubSuccessfulUpdateUserPurchaseStatus()
    buyProPresenterImpl.handleSuccessfulVerification(PURCHASE)

    verify(buyProView).showSuccessfulTransactionMessage(PURCHASE)
    verify(buyProView).finishWithResult()
  }

  @Test
  fun `should showGenericVerificationError on unsuccessful purchase verification`() {
    stubUnsuccessfulUpdateUserPurchaseStatus()
    buyProPresenterImpl.handleSuccessfulVerification(PURCHASE)

    verify(buyProView).showGenericVerificationError()
    verifyNoMoreInteractions(buyProView)
  }

  @Test
  fun `should showSuccessfulTransactionMessage and finishWithResult on restore verification success`() {
    stubSuccessfulUpdateUserPurchaseStatus()
    buyProPresenterImpl.handleSuccessfulVerification(RESTORE)

    verify(buyProView).showSuccessfulTransactionMessage(RESTORE)
    verify(buyProView).finishWithResult()
    verifyNoMoreInteractions(buyProView)
  }

  @Test fun `should showGenericVerificationError on unsuccessful restore verification`() {
    stubUnsuccessfulUpdateUserPurchaseStatus()
    buyProPresenterImpl.handleSuccessfulVerification(RESTORE)

    verify(buyProView).showGenericVerificationError()
    verifyNoMoreInteractions(buyProView)
  }

  @Test fun `should show invalid purchase exception on verifying purchase`() {
    `when`(authenticatePurchaseUseCase.buildUseCaseCompletable(packageName, skuId,
        purchaseToken)).thenReturn(Completable.error(InvalidPurchaseException()))

    buyProPresenterImpl.verifyPurchase(packageName, skuId, purchaseToken, PURCHASE)

    verify(buyProView).getScope()
    verify(buyProView).showInvalidPurchaseError()
    verify(buyProView).dismissWaitLoader()
    verifyNoMoreInteractions(buyProView)
  }

  @Test fun `should show unable to verify purchase exception on verifying purchase`() {
    `when`(authenticatePurchaseUseCase.buildUseCaseCompletable(packageName, skuId,
        purchaseToken)).thenReturn(Completable.error(UnableToVerifyPurchaseException()))

    buyProPresenterImpl.verifyPurchase(packageName, skuId, purchaseToken, PURCHASE)

    verify(buyProView).getScope()
    verify(buyProView).showUnableToVerifyPurchaseError()
    verify(buyProView).dismissWaitLoader()
    verifyNoMoreInteractions(buyProView)
  }

  @Test fun `should show generic purchase exception on verifying purchase`() {
    `when`(authenticatePurchaseUseCase.buildUseCaseCompletable(packageName, skuId,
        purchaseToken)).thenReturn(Completable.error(Exception()))

    buyProPresenterImpl.verifyPurchase(packageName, skuId, purchaseToken, PURCHASE)

    verify(buyProView).getScope()
    verify(buyProView).showGenericVerificationError()
    verify(buyProView).dismissWaitLoader()
    verifyNoMoreInteractions(buyProView)
  }

  @Test fun `should call handle successful verification on successfully verifying purchase`() {
    `when`(authenticatePurchaseUseCase.buildUseCaseCompletable(packageName, skuId,
        purchaseToken)).thenReturn(Completable.complete())
    `when`(userPremiumStatusUseCase.updateUserPurchaseStatus()).thenReturn(true)

    buyProPresenterImpl.verifyPurchase(packageName, skuId, purchaseToken, PURCHASE)

    verify(buyProView).getScope()
    verify(buyProView).dismissWaitLoader()
    verify(buyProView).showSuccessfulTransactionMessage(PURCHASE)
    verify(buyProView).finishWithResult()
    verifyNoMoreInteractions(buyProView)
  }

  @Test fun `should call handle successful verification on successfully verifying restore`() {
    `when`(authenticatePurchaseUseCase.buildUseCaseCompletable(packageName, skuId,
        purchaseToken)).thenReturn(Completable.complete())
    `when`(userPremiumStatusUseCase.updateUserPurchaseStatus()).thenReturn(true)

    buyProPresenterImpl.verifyPurchase(packageName, skuId, purchaseToken, RESTORE)

    verify(buyProView).getScope()
    verify(buyProView).dismissWaitLoader()
    verify(buyProView).showSuccessfulTransactionMessage(RESTORE)
    verify(buyProView).finishWithResult()
    verifyNoMoreInteractions(buyProView)
  }

  @After fun tearDown() {
    buyProPresenterImpl.detachView()
  }

  private fun stubIabReady() {
    `when`(buyProView.isIabReady()).thenReturn(true)
  }

  private fun stubIabNotReady() {
    `when`(buyProView.isIabReady()).thenReturn(false)
  }

  private fun stubInternetAvailable() {
    `when`(buyProView.isInternetAvailable()).thenReturn(true)
  }

  private fun stubInternetNotAvailable() {
    `when`(buyProView.isInternetAvailable()).thenReturn(false)
  }

  private fun stubSuccessfulUpdateUserPurchaseStatus() {
    `when`(userPremiumStatusUseCase.updateUserPurchaseStatus()).thenReturn(true)
  }

  private fun stubUnsuccessfulUpdateUserPurchaseStatus() {
    `when`(userPremiumStatusUseCase.updateUserPurchaseStatus()).thenReturn(false)
  }
}
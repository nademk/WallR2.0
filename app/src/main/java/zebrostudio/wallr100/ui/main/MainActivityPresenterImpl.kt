package zebrostudio.wallr100.ui.main

import android.os.Handler

class MainActivityPresenterImpl : MainContract.MainPresenter {

  var backPressedOnce: Boolean = false
  private var mainView: MainContract.MainView? = null
  private var isGuillotineMenuOpen = false

  override fun attachView(view: MainContract.MainView) {
    mainView = view
  }

  override fun detachView() {
    mainView = null
  }

  override fun handleBackPress() {
    if (isGuillotineMenuOpen) {
      mainView?.closeGuillotineMenu()
    } else {
      if (backPressedOnce) {
        mainView?.exitApp()
      } else {
        backPressedOnce = true
        mainView?.showExitConfirmation()
        Handler().postDelayed({ backPressedOnce = false }, 2000)
      }
    }
  }

  override fun notifyNavigationMenuOpened() {
    isGuillotineMenuOpen = true
  }

  override fun notifyNavigationMenuClosed() {
    isGuillotineMenuOpen = false
  }

}
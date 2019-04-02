package zebrostudio.wallr100.android.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import zebrostudio.wallr100.android.di.scopes.PerActivity
import zebrostudio.wallr100.android.ui.buypro.BuyProActivity
import zebrostudio.wallr100.android.ui.buypro.BuyProModule
import zebrostudio.wallr100.android.ui.detail.colors.ColorsDetailActivity
import zebrostudio.wallr100.android.ui.detail.colors.ColorsDetailModule
import zebrostudio.wallr100.android.ui.detail.images.DetailActivity
import zebrostudio.wallr100.android.ui.detail.images.DetailActivityModule
import zebrostudio.wallr100.android.ui.expandimage.FullScreenImageActivity
import zebrostudio.wallr100.android.ui.expandimage.FullScreenImageModule
import zebrostudio.wallr100.android.ui.main.MainActivity
import zebrostudio.wallr100.android.ui.main.MainActivityModule
import zebrostudio.wallr100.android.ui.search.SearchActivity
import zebrostudio.wallr100.android.ui.search.SearchActivityModule

@Module
abstract class ActivityBuilder {

  @PerActivity
  @ContributesAndroidInjector(modules = [(MainActivityModule::class), (FragmentProvider::class)])
  abstract fun mainActivity(): MainActivity

  @PerActivity
  @ContributesAndroidInjector(modules = [(BuyProModule::class)])
  abstract fun buyProActivity(): BuyProActivity

  @PerActivity
  @ContributesAndroidInjector(modules = [(SearchActivityModule::class)])
  abstract fun searchActivity(): SearchActivity

  @PerActivity
  @ContributesAndroidInjector(modules = [(DetailActivityModule::class)])
  abstract fun detailActivity(): DetailActivity

  @PerActivity
  @ContributesAndroidInjector(modules = [ColorsDetailModule::class])
  abstract fun colorsDetailActivity(): ColorsDetailActivity

  @PerActivity
  @ContributesAndroidInjector(modules = [(FullScreenImageModule::class)])
  abstract fun fullScreenImageActivity(): FullScreenImageActivity

}

package zebrostudio.wallr100.ui.main

import dagger.Module
import dagger.Provides



@Module
class MainActivityModule {

  @Provides
  internal fun provideMainPresenter(): MainContract.MainPresenter = MainActivityPresenterImpl()

}
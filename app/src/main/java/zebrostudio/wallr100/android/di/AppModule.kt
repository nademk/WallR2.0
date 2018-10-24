package zebrostudio.wallr100.android.di

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import zebrostudio.wallr100.domain.executor.PostExecutionThread
import zebrostudio.wallr100.android.AndroidMainThread
import zebrostudio.wallr100.android.di.scopes.PerApplication
import zebrostudio.wallr100.data.SharedPrefsHelper
import zebrostudio.wallr100.data.SharedPrefsHelperImpl
import zebrostudio.wallr100.data.WallrDataRepository
import zebrostudio.wallr100.data.api.FirebaseDatabaseHelper
import zebrostudio.wallr100.data.api.RemoteAuthServiceFactory
import zebrostudio.wallr100.data.api.RemoteAuthServiceFactoryImpl
import zebrostudio.wallr100.data.api.UnsplashClientFactory
import zebrostudio.wallr100.data.api.UnsplashClientFactoryImpl
import zebrostudio.wallr100.data.mapper.PictureEntityMapper
import zebrostudio.wallr100.domain.WallrRepository
import zebrostudio.wallr100.domain.interactor.AuthenticatePurchaseInteractor
import zebrostudio.wallr100.domain.interactor.AuthenticatePurchaseUseCase
import zebrostudio.wallr100.domain.interactor.SearchPicturesInteractor
import zebrostudio.wallr100.domain.interactor.SearchPicturesUseCase
import zebrostudio.wallr100.domain.interactor.UserPremiumStatusInteractor
import zebrostudio.wallr100.domain.interactor.UserPremiumStatusUseCase
import zebrostudio.wallr100.presentation.adapters.ImageRecyclerItemContract
import zebrostudio.wallr100.presentation.adapters.ImageRecyclerViewPresenterImpl

@Module
class AppModule {

  @Provides
  @PerApplication
  fun provideContext(application: Application): Context {
    return application
  }

  @Provides
  @PerApplication
  fun provideSharedPrefsHelper(context: Context): SharedPrefsHelper = SharedPrefsHelperImpl(context)

  @Provides
  @PerApplication
  fun provideFirebaseDatabaseHelper(): FirebaseDatabaseHelper = FirebaseDatabaseHelper()

  @Provides
  @PerApplication
  fun provideAndroidMainThread(): PostExecutionThread = AndroidMainThread()

  @Provides
  @PerApplication
  fun provideRemoteAuthServiceFactory(): RemoteAuthServiceFactory =
      RemoteAuthServiceFactoryImpl()

  @Provides
  @PerApplication
  fun provideUnsplashClientFactory(): UnsplashClientFactory = UnsplashClientFactoryImpl()

  @Provides
  fun providePictureEntityMapper(): PictureEntityMapper = PictureEntityMapper()

  @Provides
  @PerApplication
  fun provideWallrRepository(
    retrofitFirebaseAuthFactory: RemoteAuthServiceFactory,
    unsplashClientFactory: UnsplashClientFactory,
    sharedPrefsHelper: SharedPrefsHelper,
    pictureEntityMapper: PictureEntityMapper,
    firebaseDatabaseHelper: FirebaseDatabaseHelper
  ): WallrRepository = WallrDataRepository(retrofitFirebaseAuthFactory,
      unsplashClientFactory,
      sharedPrefsHelper,
      pictureEntityMapper,
      firebaseDatabaseHelper)

  @Provides
  @PerApplication
  fun provideAuthenticatePurchaseUseCase(
    wallrRepository: WallrRepository,
    postExecutionThread: PostExecutionThread
  ): AuthenticatePurchaseUseCase = AuthenticatePurchaseInteractor(wallrRepository,
      postExecutionThread)

  @Provides
  @PerApplication
  fun provideUserPremiumStatusUseCase(
    wallrRepository: WallrRepository
  ): UserPremiumStatusUseCase = UserPremiumStatusInteractor(wallrRepository)

  @Provides
  @PerApplication
  fun provideSearchPicturesUseCase(
    wallrRepository: WallrRepository,
    postExecutionThread: PostExecutionThread
  ): SearchPicturesUseCase = SearchPicturesInteractor(wallrRepository, postExecutionThread)

  @Provides
  fun provideImageRecyclerViewPresenter()
      : ImageRecyclerItemContract.ImageRecyclerViewPresenter = ImageRecyclerViewPresenterImpl()

}
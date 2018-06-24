package zebrostudio.wallr100.ui.explore

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import zebrostudio.wallr100.R
import zebrostudio.wallr100.ui.basefragment.BaseFragment
import javax.inject.Inject

class ExploreFragment : BaseFragment(), ExploreContract.ExploreView {

  @Inject
  internal lateinit var presenter: ExplorePresenterImpl

  companion object {
    val EXPLORE_TAG = "Explore"
    val TOPPICKS_TAG = "Top Picks"
    val CATEGORIES_TAG = "Categories"

    fun newInstance(): ExploreFragment {
      return ExploreFragment()
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_layout, container, false)
  }

  override fun onResume() {
    super.onResume()
    presenter.attachView(this)
  }

  override fun onDestroy() {
    presenter.detachView()
    super.onDestroy()
  }

}
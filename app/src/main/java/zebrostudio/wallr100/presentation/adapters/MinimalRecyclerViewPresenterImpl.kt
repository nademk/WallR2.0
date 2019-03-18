package zebrostudio.wallr100.presentation.adapters

import zebrostudio.wallr100.android.ui.adapters.MinimalViewHolder
import zebrostudio.wallr100.presentation.adapters.MinimalRecyclerItemContract.MinimalRecyclerViewPresenter
import zebrostudio.wallr100.presentation.minimal.MinimalContract.MinimalPresenter

const val INITIAL_OFFSET = 1

class MinimalRecyclerViewPresenterImpl : MinimalRecyclerViewPresenter {

  private var minimalPresenter: MinimalPresenter? = null
  private var colorList = mutableListOf<String>()

  override fun attachMinimalPresenter(presenter: MinimalPresenter) {
    minimalPresenter = presenter
  }

  override fun detachMinimalPresenter() {
    minimalPresenter = null
  }

  override fun appendList(colorList: List<String>) {
    this.colorList.addAll(colorList)
  }

  override fun appendColor(color: String) {
    colorList.add(color)
  }

  override fun getItemCount(): Int {
    return colorList.size + INITIAL_OFFSET
  }

  override fun onBindRepositoryRowViewAtPosition(holder: MinimalViewHolder, position: Int) {
    if (position != 0) {
      holder.showAddImageLayout()
    } else {
      holder.hideAddImageLayout()
      holder.setImageViewColor(colorList[position - INITIAL_OFFSET])
      holder.attachLongClickListener()
    }
    holder.attachClickListener()
  }

  override fun handleClick(
    position: Int,
    itemView: MinimalRecyclerItemContract.MinimalRecyclerViewItem
  ) {

  }

  override fun handleImageLongClick(
    position: Int,
    itemView: MinimalRecyclerItemContract.MinimalRecyclerViewItem
  ) {

  }

}
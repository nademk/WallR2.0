package zebrostudio.wallr100.ui.collection;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;
import zebrostudio.wallr100.R;
import zebrostudio.wallr100.ui.main.MainActivity;
import zebrostudio.wallr100.utils.FragmentTags;
import zebrostudio.wallr100.utils.UiCustomizationHelper;

public class CollectionFragment extends DaggerFragment {

    @Inject
    MainActivity mMainActivity;
    @Inject
    UiCustomizationHelper mUiCustomizationHelper;

    @Inject
    public CollectionFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_collection, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public void onResume() {
        super.onResume();
        setUpUi();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void setUpUi() {
        mMainActivity.setTitle(FragmentTags.COLLECTIONS_FRAGMENT_TAG);
        mMainActivity.setTitlePadding(0,0,0,0);
        mUiCustomizationHelper.hideSearchOption();
        mUiCustomizationHelper.hideMultiSelectOption();
        mUiCustomizationHelper.hideSmartTabLayout();
        mUiCustomizationHelper.showCollectionSwitchLayout();
        mUiCustomizationHelper.hideMinimalBottomPanel();
    }

}
package io.eodc.planit.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.eodc.planit.R;
import io.eodc.planit.adapter.ClassesAdapter;
import io.eodc.planit.db.PlannerContract;
import io.eodc.planit.listener.OnClassListChangeListener;

/**
 * The last slide of the onboarding carousel, where the user initially adds their classes
 *
 * @author 2n
 */
public class OnboardingAddClassesFragment extends OnboardingFragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        OnClassListChangeListener {

    @BindView(R.id.no_classes)
    ImageView mIvNoClasses;
    @BindView(R.id.rv_classes)
    RecyclerView mRvClasses;
    @BindView(R.id.btn_add)
    Button mBtnAdd;
    @BindView(R.id.tv_no_classes)
    TextView mTvNoClasses;
    private OnClassListChangeListener listener;

    public static OnboardingAddClassesFragment newInstance(OnClassListChangeListener l) {
        OnboardingAddClassesFragment fragment = new OnboardingAddClassesFragment();
        fragment.swapListeners(l);

        return fragment;
    }

    @OnClick(R.id.btn_add)
    void addClass() {
        if (getFragmentManager() != null) {
            ModifyClassFragment.newInstance(this, ModifyClassFragment.FLAG_NEW_CLASS, 0)
                    .show(getFragmentManager(), null);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_onboarding_classes, container, false);
        ButterKnife.bind(this, v);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) // Hack that works for some reason, idk why.
            getLoaderManager().restartLoader(0, null, this);
    }

    /**
     * Swaps the class list change listener
     */
    private void swapListeners(OnClassListChangeListener l) {
        this.listener = l;
    }

    @Override
    public void onClassListChange(int count) {
        listener.onClassListChange(count);
        updateNoClassIndicators(count);
        getLoaderManager().restartLoader(0, null, this);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        return new CursorLoader(requireContext(), PlannerContract.ClassColumns.CONTENT_URI,
                null, null, null, null);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        mRvClasses.setAdapter(new ClassesAdapter(data, getContext(), this));
        mRvClasses.setLayoutManager(new LinearLayoutManager(getContext()));
        updateNoClassIndicators(data.getCount());
        if (data.getCount() > 0)
            listener.onClassListChange(data.getCount());
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
    }

    private void updateNoClassIndicators(int count) {
        if (count > 0) {
            mTvNoClasses.setVisibility(View.GONE);
            mIvNoClasses.setVisibility(View.GONE);
        } else {
            mTvNoClasses.setVisibility(View.VISIBLE);
            mIvNoClasses.setVisibility(View.VISIBLE);
        }
    }
}

package io.eodc.planit.fragment;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.prolificinteractive.materialcalendarview.OnMonthChangedListener;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.eodc.planit.R;
import io.eodc.planit.adapter.AssignmentViewHolder;
import io.eodc.planit.adapter.AssignmentsAdapter;
import io.eodc.planit.db.Assignment;
import io.eodc.planit.db.Class;
import io.eodc.planit.helper.AssignmentTouchHelper;
import io.eodc.planit.model.AssignmentListViewModel;
import io.eodc.planit.model.ClassListViewModel;

/**
 * Fragment that displays a month's assignments, as well as the information on those assignments.
 *
 * @author 2n
 */
public class CalendarFragment extends Fragment implements
        OnDateSelectedListener,
        OnMonthChangedListener,
        DayViewDecorator {

    @BindView(R.id.calendar)            MaterialCalendarView    mCalendar;
    @BindView(R.id.rv_day_assignments)  RecyclerView            mRvDaysAssignments;
    @BindView(R.id.text_done)         TextView                mTvAllDone;

    private AssignmentListViewModel     mAssignmentListViewModel;
    private List<DateTime>              mDateHasAssignmentList;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ViewModelProviders.of(this)
                .get(ClassListViewModel.class)
                .getClasses()
                .observe(this, this::onClassesGet);

        mAssignmentListViewModel = ViewModelProviders.of(this)
                .get(AssignmentListViewModel.class);

        mAssignmentListViewModel
                .getAllAssignments()
                .observe(this, this::onDateRangeAssignmentsChange);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCalendar.setSelectedDate(new Date());
        mCalendar.setOnMonthChangedListener(this);
        mCalendar.setOnDateChangedListener(this);
    }

    private void onClassesGet(List<Class> classes) {
        if (getContext() != null) {
            AssignmentsAdapter adapter = new AssignmentsAdapter(getContext(), classes, false);
            mRvDaysAssignments.setAdapter(adapter);
            mRvDaysAssignments.setLayoutManager(new LinearLayoutManager(getContext()));

            mAssignmentListViewModel.getAssignmentsDueOnDay(new DateTime(mCalendar.getSelectedDate().getDate()))
                    .observe(this, this::onSingleDayAssignmentsChange);

            ItemTouchHelper.SimpleCallback touchSimpleCallback = new AssignmentTouchHelper(
                    0,
                    ItemTouchHelper.RIGHT,
                    this::onDismiss);
            ItemTouchHelper touchHelper = new ItemTouchHelper(touchSimpleCallback);
            touchHelper.attachToRecyclerView(mRvDaysAssignments);
        }
    }

    private void onDismiss(AssignmentViewHolder holder) {
        RecyclerView.Adapter adapter = mRvDaysAssignments.getAdapter();
        if (adapter != null) {
            adapter.notifyItemRemoved(holder.getAdapterPosition());
        }
        new Thread(() -> mAssignmentListViewModel.removeAssignments(holder.getAssignment())).start();
    }

    private void onSingleDayAssignmentsChange(List<Assignment> assignments) {
        if (mRvDaysAssignments.getAdapter() != null) {
            AssignmentsAdapter adapter = (AssignmentsAdapter) mRvDaysAssignments.getAdapter();
            if (assignments != null && assignments.size() > 0) {
                mTvAllDone.setVisibility(View.GONE);
                mRvDaysAssignments.setVisibility(View.VISIBLE);
                adapter.swapAssignmentsList(assignments);
            } else {
                mRvDaysAssignments.setVisibility(View.GONE);
                mTvAllDone.setVisibility(View.VISIBLE);
                adapter.swapAssignmentsList(null);
            }
        }
    }

    private void onDateRangeAssignmentsChange(List<Assignment> assignments) {
        mDateHasAssignmentList = new ArrayList<>();
        if (assignments != null && assignments.size() > 0) {
            mDateHasAssignmentList.add(assignments.get(0).getDueDate());
            for (int i = 0; i < assignments.size(); ++i) {
                Optional<Assignment> nextAssign = Iterables.tryFind(assignments,
                        assignment -> {
                            DateTime mostRecent = mDateHasAssignmentList.get(mDateHasAssignmentList.size() - 1);
                            return mostRecent.isBefore(assignment.getDueDate());
                        });
                if (nextAssign.isPresent()) {
                    mDateHasAssignmentList.add(nextAssign.get().getDueDate());
                }
            }
        }
        mCalendar.addDecorator(this);
    }

    @Override
    public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
        mAssignmentListViewModel.getAssignmentsDueOnDay(new DateTime(date.getDate()))
                .observe(this, this::onSingleDayAssignmentsChange);
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        for (DateTime date : mDateHasAssignmentList) {
            if (new DateTime(day.getDate()).equals(date)) return true;
        }
        return false;
    }

    @Override
    public void decorate(DayViewFacade view) { view.addSpan(new DotSpan(5f)); }

    @Override
    public void onMonthChanged(MaterialCalendarView widget, CalendarDay date) {

    }
}

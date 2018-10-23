package io.eodc.planit.behavior

import android.content.Context
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.core.view.ViewCompat
import android.util.AttributeSet
import android.view.View

/**
 * Behavior of the Floating Action Button. Not actually instantiated programmatically, but rather
 * used in the XML declaration of the FAB.
 *
 * @author 2n
 */

class FabHideBehavior(context: Context, attrs: AttributeSet) : androidx.coordinatorlayout.widget.CoordinatorLayout.Behavior<FloatingActionButton>(context, attrs) {

    private val mLastSnackbarUpdate: Long = 0

    override fun onStartNestedScroll(coordinatorLayout: androidx.coordinatorlayout.widget.CoordinatorLayout, child: FloatingActionButton, directTargetChild: View, target: View, axes: Int, type: Int): Boolean {
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL || super.onStartNestedScroll(coordinatorLayout,
                child, directTargetChild, target, axes, type)
    }

    override fun onNestedScroll(coordinatorLayout: androidx.coordinatorlayout.widget.CoordinatorLayout, child: FloatingActionButton, target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, type: Int) {
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type)
        if (dyConsumed > 0 && child.visibility == View.VISIBLE) {
            child.hide(object : FloatingActionButton.OnVisibilityChangedListener() {
                override fun onHidden(fab: FloatingActionButton?) {
                    super.onHidden(fab)
                    fab!!.visibility = View.INVISIBLE
                }
            })
            // } else if (dyUnconsumed < 0 && child.visibility != View.VISIBLE) {
        } else if (dyConsumed < 0 && child.visibility != View.VISIBLE) {
            child.show()
        }
    }
}

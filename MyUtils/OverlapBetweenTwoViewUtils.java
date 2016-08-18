package highwin.zgs.myapplication;

import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.graphics.Matrix;

/**
 * 两个View是否重叠在一起,直接调用doViewsOverlap方法就可以
 * 使用有个前提，必须是在两个view都绘制完时调用才有效，不然方法的返回值都会是true,
 * 可以考滤监听view绘制完成后才调用方法
 */
public class OverlapBetweenTwoViewUtils {
    private static final Rect mTempRect1 = new Rect();
    private static final Rect mTempRect2 = new Rect();
    private static final ThreadLocal<Matrix> sMatrix = new ThreadLocal<>();
    private static final ThreadLocal<RectF> sRectF = new ThreadLocal<>();
    private static final Matrix IDENTITY = new Matrix();

    private static final ViewGroupUtilsImpl IMPL;

    static {
        final int version = Build.VERSION.SDK_INT;
        if (version >= 11) {
            IMPL = new ViewGroupUtilsImplHoneycomb();
        } else {
            IMPL = new ViewGroupUtilsImplBase();
        }
    }

    /**
     * @param viewGroup 两个view的父View
     * @param first
     * @param second
     * @return true表示两个view有重叠，false表示两个view没有重叠
     */
    public static boolean doViewsOverlap(ViewGroup viewGroup, View first, View second) {
        if (first.getVisibility() == View.VISIBLE && second.getVisibility() == View.VISIBLE) {
            final Rect firstRect = mTempRect1;
            getChildRect(viewGroup, first, first.getParent() != viewGroup, firstRect);
            final Rect secondRect = mTempRect2;
            getChildRect(viewGroup, second, second.getParent() != viewGroup, secondRect);

            return !(firstRect.left > secondRect.right || firstRect.top > secondRect.bottom
                    || firstRect.right < secondRect.left || firstRect.bottom < secondRect.top);
        }
        return false;
    }

    public static void getChildRect(ViewGroup viewGroup, View child, boolean transform, Rect out) {
        if (child.isLayoutRequested() || child.getVisibility() == View.GONE) {
            out.set(0, 0, 0, 0);
            return;
        }
        if (transform) {
            getDescendantRect(viewGroup, child, out);
        } else {
            out.set(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
        }
    }

    static void getDescendantRect(ViewGroup viewGroup, View descendant, Rect out) {
        getDescendantRectViewGroup(viewGroup, descendant, out);
    }


    private interface ViewGroupUtilsImpl {
        void offsetDescendantRect(ViewGroup parent, View child, Rect rect);
    }

    private static class ViewGroupUtilsImplBase implements ViewGroupUtilsImpl {
        @Override
        public void offsetDescendantRect(ViewGroup parent, View child, Rect rect) {
            parent.offsetDescendantRectToMyCoords(child, rect);
            rect.offset(child.getScrollX(), child.getScrollY());
        }
    }

    private static class ViewGroupUtilsImplHoneycomb implements ViewGroupUtilsImpl {
        @Override
        public void offsetDescendantRect(ViewGroup parent, View child, Rect rect) {
            offsetDescendantRectHoneyComb(parent, child, rect);
        }
    }


    public static void offsetDescendantRect(ViewGroup parent, View descendant, Rect rect) {
        IMPL.offsetDescendantRect(parent, descendant, rect);
    }

    public static void getDescendantRectViewGroup(ViewGroup parent, View descendant, Rect out) {
        out.set(0, 0, descendant.getWidth(), descendant.getHeight());
        offsetDescendantRect(parent, descendant, out);
    }


    public static void offsetDescendantRectHoneyComb(ViewGroup group, View child, Rect rect) {
        Matrix m = sMatrix.get();
        if (m == null) {
            m = new Matrix();
            sMatrix.set(m);
        } else {
            m.set(IDENTITY);
        }

        offsetDescendantMatrix(group, child, m);

        RectF rectF = sRectF.get();
        if (rectF == null) {
            rectF = new RectF();
        }
        rectF.set(rect);
        m.mapRect(rectF);
        rect.set((int) (rectF.left + 0.5f), (int) (rectF.top + 0.5f),
                (int) (rectF.right + 0.5f), (int) (rectF.bottom + 0.5f));
    }

    public static void offsetDescendantMatrix(ViewParent target, View view, Matrix m) {
        final ViewParent parent = view.getParent();
        if (parent instanceof View && parent != target) {
            final View vp = (View) parent;
            offsetDescendantMatrix(target, vp, m);
            m.preTranslate(-vp.getScrollX(), -vp.getScrollY());
        }

        m.preTranslate(view.getLeft(), view.getTop());

        if (!view.getMatrix().isIdentity()) {
            m.preConcat(view.getMatrix());
        }
    }


}

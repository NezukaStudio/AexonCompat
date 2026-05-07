package com.aexon.toast;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;
import android.util.TypedValue;
import android.graphics.Color;

import com.aexon.annotation.NonNull;
import com.aexon.annotation.Nullable;
import com.aexon.R;

public class AexonToastOutline extends LinearLayout {
	
	private Paint strokePaint;
	private Paint backgroundPaint;
	private Path path;
	private Path segmentPath;
	private PathMeasure pathMeasure;
	private float pathLength;
	private float cornerRadius;
	private float progress = 1f;
	private ValueAnimator animator;
	private int backgroundColor;
	
	public AexonToastOutline(@NonNull Context context) {
		super(context);
		init(context, 0);
	}
	
	public AexonToastOutline(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		init(context, 0);
	}
	
	public AexonToastOutline(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context, 0);
	}
	
	private void init(@NonNull Context context, float cornerRadius) {
		this.cornerRadius = cornerRadius;
		this.backgroundColor = Color.BLACK;
		setWillNotDraw(false);
		
		strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		strokePaint.setStyle(Paint.Style.STROKE);
		strokePaint.setStrokeCap(Paint.Cap.ROUND);
		strokePaint.setStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1.5f, context.getResources().getDisplayMetrics()));
		strokePaint.setColor(context.getResources().getColor(R.color.colorOutlineVariant));
		
		backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		backgroundPaint.setStyle(Paint.Style.FILL);
		backgroundPaint.setColor(backgroundColor);
		
		path = new Path();
		segmentPath = new Path();
	}
	
	public void setCornerRadius(float cornerRadius) {
		this.cornerRadius = cornerRadius;
		path.reset();
		applyClipToOutline();
		invalidate();
	}
	
	public void setStrokeColor(int color) {
		strokePaint.setColor(color);
		invalidate();
	}
	
	@Override
	public void setBackgroundColor(int color) {
		this.backgroundColor = color;
		backgroundPaint.setColor(color);
		invalidate();
	}
	
	private void applyClipToOutline() {
		final float radius = cornerRadius > 0 ? cornerRadius : 16 * getResources().getDisplayMetrics().density;
		setOutlineProvider(new ViewOutlineProvider() {
			@Override
			public void getOutline(@NonNull View view, @NonNull Outline outline) {
				outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
			}
		});
		setClipToOutline(true);
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		
		applyClipToOutline();
		
		path.reset();
		
		float sw = strokePaint.getStrokeWidth() / 2f;
		float left = sw;
		float top = sw;
		float right = w - sw;
		float bottom = h - sw;
		
		float cr = cornerRadius > 0 ? cornerRadius : 16 * getResources().getDisplayMetrics().density;
		
		path.moveTo(right, h / 2f);
		path.lineTo(right, bottom - cr);
		RectF br = new RectF(right - cr * 2, bottom - cr * 2, right, bottom);
		path.arcTo(br, 0, 90, false);
		
		path.lineTo(left + cr, bottom);
		RectF bl = new RectF(left, bottom - cr * 2, left + cr * 2, bottom);
		path.arcTo(bl, 90, 90, false);
		
		path.lineTo(left, top + cr);
		RectF tl = new RectF(left, top, left + cr * 2, top + cr * 2);
		path.arcTo(tl, 180, 90, false);
		
		path.lineTo(right - cr, top);
		RectF tr = new RectF(right - cr * 2, top, right, top + cr * 2);
		path.arcTo(tr, 270, 90, false);
		
		path.lineTo(right, h / 2f);
		
		pathMeasure = new PathMeasure(path, false);
		pathLength = pathMeasure.getLength();
	}
	
	@Override
	protected void dispatchDraw(@NonNull Canvas canvas) {
		super.dispatchDraw(canvas);
		if (pathLength > 0 && progress > 0f) {
			float visibleLength = pathLength * progress;
			float startDistance = (pathLength - visibleLength) / 2f;
			float stopDistance = startDistance + visibleLength;
			
			segmentPath.reset();
			pathMeasure.getSegment(startDistance, stopDistance, segmentPath, true);
			canvas.drawPath(segmentPath, strokePaint);
		}
	}
	
	public void startProgressAnimation(int duration, @Nullable OnToastFinishListener listener) {
		if (animator != null) {
			animator.cancel();
		}
		
		progress = 1f;
		invalidate();
		
		animator = ValueAnimator.ofFloat(1f, 0f);
		animator.setDuration(duration);
		animator.setInterpolator(new LinearInterpolator());
		animator.addUpdateListener(animation -> {
			progress = (float) animation.getAnimatedValue();
			invalidate();
		});
		animator.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(@NonNull Animator animation) {
				if (listener != null) listener.onFinish();
			}
		});
		animator.start();
	}
	
	public interface OnToastFinishListener {
		void onFinish();
	}
}
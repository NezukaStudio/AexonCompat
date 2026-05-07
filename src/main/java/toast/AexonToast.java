package com.aexon.toast;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aexon.annotation.NonNull;
import com.aexon.annotation.Nullable;
import com.aexon.R;

public class AexonToast {
	
	public static final int LENGTH_SHORT = 2000;
	public static final int LENGTH_LONG = 3500;
	
	@NonNull
	private Context context;
	@Nullable
	private String title;
	@Nullable
	private String message;
	private int duration = LENGTH_SHORT;
	
	@Nullable
	private Integer titleColor;
	@Nullable
	private Integer messageColor;
	@Nullable
	private Integer backgroundColor;
	@Nullable
	private Integer outlineColor;
	
	@Nullable
	private Typeface typeface;
	private int fontResId = 0;
	@Nullable
	private String fontPath;
	
	@Nullable
	private Toast toast;
	@NonNull
	private Handler handler;
	
	private AexonToast(@NonNull Context context) {
		this.context = context;
		this.handler = new Handler(Looper.getMainLooper());
	}
	
	@NonNull
	public static AexonToast make(@NonNull Context context) {
		return new AexonToast(context);
	}
	
	@NonNull
	public AexonToast title(@NonNull String title) {
		this.title = title;
		return this;
	}
	
	@NonNull
	public AexonToast message(@NonNull String message) {
		this.message = message;
		return this;
	}
	
	@NonNull
	public AexonToast duration(int duration) {
		this.duration = duration;
		return this;
	}
	
	@NonNull
	public AexonToast setTitleColor(int color) {
		this.titleColor = color;
		return this;
	}
	
	@NonNull
	public AexonToast setMessageColor(int color) {
		this.messageColor = color;
		return this;
	}
	
	@NonNull
	public AexonToast setBackgroundColor(int color) {
		this.backgroundColor = color;
		return this;
	}
	
	@NonNull
	public AexonToast setOutlineColor(int color) {
		this.outlineColor = color;
		return this;
	}
	
	@NonNull
	public AexonToast setTypeface(@Nullable Typeface typeface) {
		this.typeface = typeface;
		this.fontResId = 0;
		this.fontPath = null;
		return this;
	}
	
	@NonNull
	public AexonToast setTypeface(int fontResId) {
		this.fontResId = fontResId;
		this.typeface = null;
		this.fontPath = null;
		return this;
	}
	
	@NonNull
	public AexonToast setTypeface(@NonNull String fontPath) {
		this.fontPath = fontPath;
		this.typeface = null;
		this.fontResId = 0;
		return this;
	}
	
	public void show() {
    if (context == null) return;

    handler.post(new Runnable() {
        @Override
        public void run() {
            createAndShowToast();
        }
    });
  }
	
	@SuppressLint("ClickableViewAccessibility")
	private void createAndShowToast() {
		if (toast != null) toast.cancel();
		
		FrameLayout rootLayout = new FrameLayout(context);
		rootLayout.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		rootLayout.setClickable(true);
		rootLayout.setFocusable(true);
		
		View overlay = new View(context);
		overlay.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		overlay.setBackgroundColor(Color.TRANSPARENT);
		
		AexonToastOutline container = (AexonToastOutline) LayoutInflater.from(context).inflate(R.layout.aexon_toast, rootLayout, false);
		FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		containerParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
		containerParams.bottomMargin = dpToPx(80);
		container.setLayoutParams(containerParams);
		
		TextView titleView = container.findViewById(R.id.title);
		TextView messageView = container.findViewById(R.id.message);
		
		if (title != null && !title.isEmpty()) {
			titleView.setText(title);
			titleView.setVisibility(View.VISIBLE);
			
			if (titleColor != null) {
				titleView.setTextColor(titleColor);
			}
			
			applyTypeface(titleView);
		} else {
			titleView.setVisibility(View.GONE);
		}
		
		messageView.setText(message != null ? message : "");
		
		if (messageColor != null) {
			messageView.setTextColor(messageColor);
		}
		
		applyTypeface(messageView);
		
		if (backgroundColor != null) {
			container.setBackgroundColor(backgroundColor);
		}
		
		if (outlineColor != null) {
			container.setStrokeColor(outlineColor);
		}
		
		rootLayout.addView(overlay);
		rootLayout.addView(container);
		
		toast = new Toast(context);
		toast.setView(rootLayout);
		toast.setDuration(Toast.LENGTH_LONG);
		toast.setGravity(Gravity.FILL, 0, 0);
		toast.show();
		
		container.setTranslationY(dpToPx(80));
		container.setAlpha(0f);
		container.setScaleX(0.9f);
		container.setScaleY(0.9f);
		
		overlay.animate().alpha(1f).setDuration(400).setInterpolator(new DecelerateInterpolator(1.5f));
		container.animate().translationY(0f).alpha(1f).scaleX(1f).scaleY(1f).setDuration(400).setInterpolator(new DecelerateInterpolator(1.5f)).withEndAction(() -> {
			container.startProgressAnimation(duration, () -> {
				overlay.animate().alpha(0f).setDuration(350).setInterpolator(new AccelerateInterpolator(1.8f));
				
				container.animate().translationY(-dpToPx(60)).alpha(0f).scaleX(0.85f).scaleY(0.85f).setDuration(350).setInterpolator(new AccelerateInterpolator(1.8f)).withEndAction(() -> {
					if (toast != null) toast.cancel();
				});
			});
		});
	}
	
	private void applyTypeface(@NonNull TextView textView) {
		if (typeface != null) {
			textView.setTypeface(typeface);
		} else if (fontResId != 0) {
			try {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					Typeface loadedFont = context.getResources().getFont(fontResId);
					textView.setTypeface(loadedFont);
				} else {
					TypedArray typedArray = context.getTheme().obtainStyledAttributes(fontResId, new int[]{android.R.attr.fontFamily});
					String fontFamily = typedArray.getString(0);
					typedArray.recycle();
					if (fontFamily != null) {
						Typeface loadedFont = Typeface.create(fontFamily, Typeface.NORMAL);
						textView.setTypeface(loadedFont);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (fontPath != null) {
			try {
				Typeface loadedFont = Typeface.createFromAsset(context.getAssets(), fontPath);
				textView.setTypeface(loadedFont);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private int dpToPx(float dp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
	}
}
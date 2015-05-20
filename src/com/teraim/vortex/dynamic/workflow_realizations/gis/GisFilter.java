package com.teraim.vortex.dynamic.workflow_realizations.gis;

import android.graphics.Bitmap;
import android.graphics.Paint.Style;

public interface GisFilter {

	String getExpression();

	String getLabel();

	Bitmap getBitmap();

	float getRadius();

	String getColor();

	Style getStyle();

	boolean isCircle();

	boolean isActive();

}

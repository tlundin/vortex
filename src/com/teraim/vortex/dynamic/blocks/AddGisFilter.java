package com.teraim.vortex.dynamic.blocks;

import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.teraim.vortex.R;
import com.teraim.vortex.dynamic.types.GisLayer;
import com.teraim.vortex.dynamic.workflow_abstracts.Drawable;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Context;
import com.teraim.vortex.dynamic.workflow_realizations.gis.FullGisObjectConfiguration.PolyType;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisFilter;
import com.teraim.vortex.dynamic.workflow_realizations.gis.WF_Gis_Map;
import com.teraim.vortex.utils.RuleExecutor.TokenizedItem;
import com.teraim.vortex.utils.Tools;

public class AddGisFilter extends Block implements GisFilter {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3888638684411710898L;
	String id, nName, label, targetObjectType,targetLayer,
	expression, imgSource, 
	color;
	float radius;
	Style fillType;
	PolyType polyType;
	boolean hasWidget=true,isActive=true;
	private WF_Gis_Map myGis;
	public AddGisFilter(String id, String nName, String label, String targetObjectType,String targetLayer,
			String expression, String imgSource, 
			String radius, String color, String polyType, String fillType,
			boolean hasWidget) {
		super();
		this.id = id;
		this.nName = nName;
		this.label = label;
		this.targetObjectType = targetObjectType;
		this.targetLayer = targetLayer;
		this.expression = expression;
		this.imgSource = imgSource;
		this.color = color;
		this.polyType=PolyType.circle;
		this.radius=2.5f;
		if (Tools.isNumeric(radius))
			this.radius = Float.parseFloat(radius);
		this.fillType=Paint.Style.FILL;
		if  (fillType !=null) {
			if (fillType.equalsIgnoreCase("STROKE"))
				this.fillType = Paint.Style.STROKE;
			else if (fillType.equalsIgnoreCase("FILL_AND_STROKE"))
				this.fillType = Paint.Style.FILL_AND_STROKE;
		}


		if (polyType!=null) {
			if (polyType.toUpperCase().equals("SQUARE")||polyType.equals("RECT")||polyType.equals("RECTANGLE"))
				this.polyType=PolyType.rect;
		}		
		this.hasWidget = hasWidget;



	}

	//Collect the gisobjects affected by the filter. Apply the filtering.
	public void create(WF_Context myContext) {
			myGis = myContext.getCurrentGis();
			if (myGis!=null) {
			final GisLayer gisLayer = myGis.getGis().getLayer(targetLayer);
			if (gisLayer!=null) {
			if (hasWidget) {
				Log.d("vortex","Filter "+nName+" has a widget");
				LinearLayout layersL = (LinearLayout)myGis.getWidget().findViewById(R.id.FiltersL);
				LayoutInflater li = LayoutInflater.from(myContext.getContext());
				View filtersRow = li.inflate(R.layout.filters_row, null);
				TextView filterNameT = (TextView)filtersRow.findViewById(R.id.filterName);
				CheckBox lShow = (CheckBox)filtersRow.findViewById(R.id.cbShow);
				filterNameT.setText(this.getLabel());
				lShow.setChecked(isActive);
				lShow.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {						
						setActive(isChecked);
						isActive = isChecked;
						myGis.getGis().invalidate();
					}

					
				});
				
				gisLayer.addObjectFilter(targetObjectType, this);

				layersL.addView(filtersRow);
				layersL.setVisibility(View.VISIBLE);
			}
			} else {
				o.addRow("");
				o.addRedText("Cannot add GisFilter in Block "+blockId+". Cannot find the Layer. Make sure this block comes AFTER the AddGisLayer Block");
			}
		}
	}
	
	private void setActive(boolean isChecked) {
		this.isActive=isChecked;
	}

	@Override
	public String getExpression() {
		return expression;
	}

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public Bitmap getBitmap() {
		return null;
	}

	@Override
	public float getRadius() {
		return radius;
	}

	@Override
	public String getColor() {
		return color;
	}

	@Override
	public Style getStyle() {		
		return fillType;
	}

	@Override
	public boolean isCircle() {
		return polyType == PolyType.circle;
	}

	@Override
	public boolean isActive() {
		return isActive;
	}

	private List<TokenizedItem> myT = null;
	@Override
	public boolean hasCachedFilterResult() {
		return myT!=null;
	}

	@Override
	public void setTokens(List<TokenizedItem> myTokens) {
		myT=myTokens;
	}

	@Override
	public List<TokenizedItem> getTokens() {
		return myT;
	}
	
	



}

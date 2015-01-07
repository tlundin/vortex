package com.teraim.vortex.dynamic.blocks;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart.Type;
import org.achartengine.model.CategorySeries;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.SimpleSeriesRenderer;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.R;
import com.teraim.vortex.dynamic.workflow_abstracts.Event;
import com.teraim.vortex.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.vortex.dynamic.workflow_abstracts.EventListener;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Container;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Context;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Widget;

public class RoundChartBlock extends Block implements EventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4030652478782165890L;
	String label, container,
	type="Pie", axisTitle="", margins="0 0 0 0",startAngle = "0";
	boolean displayValues=true,isVisible=true,percentage=false;
	float textSize;
	private WF_Widget myWidget;
	GraphicalView pie;
	
	public RoundChartBlock(String id, String label, String container,
			String type, String axisTitle, String textSize, String margins,
			String startAngle, boolean displayValues, boolean percentage,
			boolean isVisible) {
		super();
		this.blockId = id;
		this.label = label;
		this.container = container;
		this.type = type;
		this.axisTitle = axisTitle;
		this.textSize = textSize==null?10:Float.parseFloat(textSize);
		this.margins = margins;
		this.startAngle = startAngle;
		this.displayValues = displayValues;
		this.isVisible = isVisible;
		this.percentage = percentage;
	
	}

	public void create(WF_Context myContext) {
		o = GlobalState.getInstance(myContext.getContext()).getLogger();
		WF_Container myContainer = (WF_Container)myContext.getContainer("root");
//		WF_Container myPieContainer = (WF_Container)myContext.getContainer("pie");

		pie = createPie(myContext.getContext());
		
		View mX = LayoutInflater.from(myContext.getContext()).inflate(R.layout.chart_wrapper,myContainer.getViewGroup());
		LinearLayout mChart = (LinearLayout)mX.findViewById(R.id.myChart);
		if (myContainer !=null) {
			myWidget = new WF_Widget(blockId,pie,isVisible,myContext);	
			LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) pie.getLayoutParams();
			layoutParams.height=300;
			myContainer.add(myWidget);
			TextView tv = new TextView(myContext.getContext());
		
			tv.setText("Hooopla");
			//myContainer.add(new WF_Widget("tst",tv,isVisible,myContext));
			Log.d("nils","Added pie chart "+myWidget.getId()+" to container "+myContainer.getId());
			myContext.addEventListener(this, EventType.onAttach);
		} else {
			o.addRow("");
			o.addRedText("Failed to add listEntriesblock - could not find the container "+container);
		}

	}
	

	private GraphicalView createPie(Context ctx){

		//View mChart = LayoutInflater.from(ctx).inflate(R.layout.chart_wrapper,null);
        // Pie Chart Section Names
        String[] code = new String[] {
            "Eclair & Older", "Froyo", "Gingerbread", "Honeycomb",
            "IceCream Sandwich", "Jelly Bean"
        };

        // Pie Chart Section Value
        double[] distribution = { 3.9, 12.9, 55.8, 1.9, 23.7, 1.8 } ;

        // Color of each Pie Chart Sections
        int[] colors = { Color.BLUE, Color.MAGENTA, Color.GREEN, Color.CYAN, Color.RED,
                        Color.YELLOW };

        // Instantiating CategorySeries to plot Pie Chart
        CategorySeries distributionSeries = new CategorySeries("PIe");
        for(int i=0 ;i < distribution.length;i++){
            // Adding a slice with its values and name to the Pie Chart
            distributionSeries.add(code[i], distribution[i]);
        }

        // Instantiating a renderer for the Pie Chart
        DefaultRenderer defaultRenderer  = new DefaultRenderer();
        for(int i = 0 ;i<distribution.length;i++){
            SimpleSeriesRenderer seriesRenderer = new SimpleSeriesRenderer();
            seriesRenderer.setColor(colors[i]);
            seriesRenderer.setDisplayChartValues(true);
            // Adding a renderer for a slice
            defaultRenderer.addSeriesRenderer(seriesRenderer);
        }

        defaultRenderer.setChartTitle(label);
        defaultRenderer.setChartTitleTextSize(textSize+10);
        //defaultRenderer.setZoomButtonsVisible(true);
        defaultRenderer.setBackgroundColor(Color.GRAY);
        defaultRenderer.setDisplayValues(true);
        defaultRenderer.setInScroll(true);


     // Getting a reference to LinearLayout of the MainActivity Layout
        //LinearLayout chartContainer = (LinearLayout) findViewById(R.id.chart_container);


        // Creating a Line Chart
       // ((LinearLayout)mChart.findViewById(R.id.chart)).addView(ChartFactory.getPieChartView(ctx, distributionSeries , defaultRenderer),new LayoutParams
       // 		(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

   
        //Log.w("In Chart", mChart.toString() +"");
        return ChartFactory.getPieChartView(ctx, distributionSeries , defaultRenderer);

    }

	@Override
	public void onEvent(Event e) {
		Log.d("vortex","got onAttach event in pieblock");
		//pie.repaint();
	}
	
	

}

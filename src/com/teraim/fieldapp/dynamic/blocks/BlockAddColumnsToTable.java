package com.teraim.fieldapp.dynamic.blocks;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Table;
import com.teraim.fieldapp.utils.Tools;

public class BlockAddColumnsToTable extends Block {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3041902713022605254L;
	private String name=null,target=null,label=null,type=null,colKey=null,width=null;
	
	public BlockAddColumnsToTable(String id, String target, String label,
			 String colKey, String type, String width) {
		this.blockId=id;
		this.target=target;
		this.colKey=colKey;
		this.type=type;
		this.width=width;
		this.label=label;
	}
	
	
	public void create(WF_Context myContext) {
		WF_Table myTable = myContext.getTable(target);
		o = GlobalState.getInstance().getLogger();
		if (myTable==null) {
			Log.e("vortex","Did not find target table "+target+" in blockAddColumn, create");
			o.addRow("");
			o.addRedText("Did not find target table "+target+" when trying to add column "+name+". Operation cancelled");
			return;
		}
		
		//Is this AddColumn or add several columns?
		List<String> labels = null;
		List<String> columnKeyL = null;
		//If label is null, a 
		if (label!=null) {
			labels = new ArrayList<String>();
			String[] labelsA = label.split(",");
			for (String l:labelsA)
				labels.add(l.trim());
			
			if (colKey!=null) {
				columnKeyL = new ArrayList<String>();
				//split, and parse elements.
				String[] colKeysA = colKey.split(",");
				for (String key:colKeysA) {
				//check for "-" interval.
				key = key.trim();
				if (key.contains("-")) {
					Log.d("vortex","found potential range in colkey: "+key);
					String[] rangeA = key.split("-");
					if (rangeA!=null && rangeA.length==2) {
						String n1 = rangeA[0].trim(); String n2 = rangeA[1].trim();
						Log.d("vortex","found likely range: "+n1+"-"+n2);
						if (Tools.isNumeric(n1)&&Tools.isNumeric(n2)) {
							int start = Integer.parseInt(rangeA[0]);
							int end = Integer.parseInt(rangeA[1]);
							for (int i = start;i<=end;i++)
								columnKeyL.add(i+"");
						} else {
							Log.e("vortex","non numeric values!!");
						}
					}
				} else
					columnKeyL.add(key);
				}
				Log.d("vortex","Colkey: "+columnKeyL.toString()+" \nLabels: "+labels.toString());
			}
		}
		myTable.addColumns(labels,columnKeyL,type,width);
	}

}

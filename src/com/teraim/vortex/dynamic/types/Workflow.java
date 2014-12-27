package com.teraim.vortex.dynamic.types;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.app.Fragment;
import android.util.Log;

import com.teraim.vortex.dynamic.blocks.Block;
import com.teraim.vortex.dynamic.blocks.PageDefineBlock;
import com.teraim.vortex.dynamic.blocks.StartBlock;

//Workflow
public class Workflow implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8806673639097744371L;
	private List<Block> blocks;
	private String name,label,applicationName,applicationVersion;

	public enum Unit {
		percentage,
		dm,
		m,
		cm,
		meter,
		cl,
		ml,
		m2,
		dl2,
		cl2,
		m3,
		dm3,
		deg,
		mm,
		st,
		m2_ha,
		antal,
		antal_ha,
		år,
		nd

	};
	

	public Workflow() {
		
	}
	
	public Workflow(String bundleVersion, String bundleName) {
		this.applicationVersion=bundleVersion;
		this.applicationName=bundleName;
	}
	
	
	public List<Block> getBlocks() {
		return blocks;
	}
	
	public List<Block> getCopyOfBlocks() {
		if (blocks==null)
			return null;
		List<Block> ret = new ArrayList<Block>(blocks);
		return ret;
	}
	
	public void addBlocks(List<Block> _blocks) {
		blocks = _blocks;
	}

	public String getName() {
		if (name==null) {
			if (blocks!=null && blocks.size()>0)
				name = ((StartBlock)blocks.get(0)).getName();

		}
		return name;
	}
	
	public String getLabel() {
		if (label==null) {
			if (blocks!=null && blocks.size()>1 && blocks.get(1) instanceof PageDefineBlock)
					label = ((PageDefineBlock)blocks.get(1)).getPageLabel();
		}
		return label;
	}

	
	public Fragment createFragment() {
		Fragment f = null;
		try {
			Class<?> cs = Class.forName("com.teraim.vortex.dynamic.templates."+getType());
			f = (Fragment)cs.newInstance();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e2) {
			e2.printStackTrace();
		} catch (IllegalAccessException e3) {
			e3.printStackTrace();
		}
	return f;
}

public String getType() {
	for (Block b:blocks) {
		if (b instanceof PageDefineBlock) {
			PageDefineBlock bl = (PageDefineBlock)b;
			return bl.getPageType();
		}
	}
	Log.e("NILS","Could not find PageDefineBlock for workflow "+this.getName()+" Will default to Default type");
	return "DefaultTemplate";
}

public String getApplication() {
	return applicationName;
}

public String getApplicationVersion() {
	return applicationVersion;
}



}

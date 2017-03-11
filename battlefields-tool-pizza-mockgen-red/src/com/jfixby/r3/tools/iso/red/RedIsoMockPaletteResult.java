
package com.jfixby.r3.tools.iso.red;

import com.jfixby.r3.ext.api.scene2d.srlz.Scene2DPackage;
import com.jfixby.r3.tools.api.iso.IsoMockPaletteResult;
import com.jfixby.scarabei.api.assets.ID;
import com.jfixby.scarabei.api.collections.Collection;
import com.jfixby.scarabei.api.collections.Collections;
import com.jfixby.scarabei.api.collections.List;
import com.jfixby.scarabei.api.file.File;
import com.jfixby.scarabei.api.log.L;

public class RedIsoMockPaletteResult implements IsoMockPaletteResult {

	private File output_folder;
	List<ID> dependencies = Collections.newList();
	private File raster_out;
	private ID namespace;
	private File struct_file;
	private Scene2DPackage structures;

	@Override
	public void print () {
		L.d("---RedIsoMockPaletteResult-------------");
		L.d("   output_folder", output_folder);
		dependencies.print("produced assets");
	}

	public void setOutputFolder (File output_folder) {
		this.output_folder = output_folder;
	}

	public void addDependency (ID tile_id) {
		dependencies.add(tile_id);
	}

	@Override
	public File getRasterOutputFolder () {
		return raster_out;
	}

	public void setRasterOutput (File raster_output) {
		this.raster_out = raster_output;
	}

	@Override
	public ID getNamespace () {
		return namespace;
	}

	public void setNamespace (ID namespace) {
		this.namespace = namespace;
	}

	public void setSceneStructuresFile (File struct_file) {
		this.struct_file = struct_file;
	}

	public void setScene2DPackage (Scene2DPackage structures) {
		this.structures = structures;
	}

	@Override
	public Scene2DPackage getScene2DPackage () {
		return structures;
	}

	@Override
	public Collection<ID> getAssetsUsed () {
		return dependencies;
	}

}

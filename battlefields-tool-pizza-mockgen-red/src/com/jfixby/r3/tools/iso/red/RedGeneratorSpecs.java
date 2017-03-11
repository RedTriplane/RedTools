
package com.jfixby.r3.tools.iso.red;

import com.jfixby.r3.ext.api.patch18.palette.Fabric;
import com.jfixby.r3.tools.api.iso.GeneratorParams;
import com.jfixby.scarabei.api.collections.Collections;
import com.jfixby.scarabei.api.collections.Map;
import com.jfixby.scarabei.api.collections.Mapping;
import com.jfixby.scarabei.api.color.Color;
import com.jfixby.scarabei.api.file.File;
import com.jfixby.utl.pizza.api.PizzaPalette;

public class RedGeneratorSpecs implements GeneratorParams {

	private File mock_palette_folder;
	private int padding;

	final Map<Fabric, Color> fabric_colors = Collections.newMap();
	private PizzaPalette palette;

	@Override
	public void setOutputFolder (File mock_palette_folder) {
		this.mock_palette_folder = mock_palette_folder;
	}

	@Override
	public File getOutputFolder () {
		return mock_palette_folder;
	}

	@Override
	public void setPadding (int pixels) {
		this.padding = pixels;
	}

	@Override
	public int getPadding () {
		return padding;
	}

	@Override
	public void setFabricColor (Fabric fabric, Color color) {
		fabric_colors.put(fabric, color);
	}

	@Override
	public Mapping<Fabric, Color> getFabricColors () {
		return fabric_colors;
	}

	@Override
	public void setPizzaPalette (PizzaPalette palette) {
		this.palette = palette;
	}

	@Override
	public PizzaPalette getPizzaPalette () {
		return palette;
	}

}

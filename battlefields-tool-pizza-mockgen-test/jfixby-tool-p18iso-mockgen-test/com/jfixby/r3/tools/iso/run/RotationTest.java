
package com.jfixby.r3.tools.iso.run;

import com.github.wrebecca.bleed.RebeccaTextureBleeder;
import com.jfixby.r3.ext.api.patch18.P18;
import com.jfixby.r3.ext.p18t.red.RedP18Terrain;
import com.jfixby.r3.ext.red.terrain.RedTerrain;
import com.jfixby.r3.tools.api.iso.IsoMockPaletteGenerator;
import com.jfixby.r3.tools.iso.red.RedIsoMockPaletteGenerator2;
import com.jfixby.scarabei.adopted.gdx.GdxSimpleTriangulator;
import com.jfixby.scarabei.adopted.gdx.json.GdxJson;
import com.jfixby.scarabei.api.angles.Angles;
import com.jfixby.scarabei.api.desktop.ImageAWT;
import com.jfixby.scarabei.api.desktop.ScarabeiDesktop;
import com.jfixby.scarabei.api.floatn.Float2;
import com.jfixby.scarabei.api.geometry.Geometry;
import com.jfixby.scarabei.api.geometry.projections.RotateProjection;
import com.jfixby.scarabei.api.json.Json;
import com.jfixby.scarabei.api.log.L;
import com.jfixby.scarabei.api.math.SimpleTriangulator;
import com.jfixby.scarabei.red.desktop.image.RedImageAWT;
import com.jfixby.texture.slicer.api.TextureSlicer;
import com.jfixby.texture.slicer.red.RedTextureSlicer;
import com.jfixby.tools.bleed.api.TextureBleed;
import com.jfixby.tools.gdx.texturepacker.GdxTexturePacker;
import com.jfixby.tools.gdx.texturepacker.api.TexturePacker;
import com.jfixby.util.iso.api.Isometry;
import com.jfixby.util.iso.red.RedIsometry;
import com.jfixby.util.p18t.api.P18Terrain;
import com.jfixby.util.patch18.red.RedP18;
import com.jfixby.util.terain.test.api.palette.Terrain;
import com.jfixby.utl.pizza.api.Pizza;
import com.jfixby.utl.pizza.red.RedPizza;

public class RotationTest {

	public static void main (final String[] args) {

		ScarabeiDesktop.deploy();
		P18Terrain.installComponent(new RedP18Terrain());
		P18.installComponent(new RedP18());
		Terrain.installComponent(new RedTerrain());
		Pizza.installComponent(new RedPizza());
		SimpleTriangulator.installComponent(new GdxSimpleTriangulator());
		Isometry.installComponent(new RedIsometry());
		TexturePacker.installComponent(new GdxTexturePacker());
		TextureSlicer.installComponent(new RedTextureSlicer());
		Json.installComponent(new GdxJson());
		TextureBleed.installComponent(new RebeccaTextureBleeder());
		ImageAWT.installComponent(new RedImageAWT());
		// IsoMockPaletteGenerator
		// .installComponent(new RedIsoMockPaletteGenerator());
		IsoMockPaletteGenerator.installComponent(new RedIsoMockPaletteGenerator2());

		final Float2 ex = Geometry.newFloat2(1, 0);
		final RotateProjection r0 = Geometry.getProjectionFactory().newRotate();
		r0.setRotation(Angles.g45());
		r0.project(ex);
		L.d("rotate", ex);

	}

}

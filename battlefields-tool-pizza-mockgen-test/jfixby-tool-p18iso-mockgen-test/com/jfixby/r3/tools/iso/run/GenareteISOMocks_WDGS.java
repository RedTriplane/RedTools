
package com.jfixby.r3.tools.iso.run;

import java.io.IOException;

import com.github.wrebecca.bleed.RebeccaTextureBleeder;
import com.jfixby.examples.wdgs.WDGS_P18_Palette;
import com.jfixby.examples.wdgs.WDGS_Pizza_Palette;
import com.jfixby.r3.ext.api.patch18.P18;
import com.jfixby.r3.ext.api.scene2d.srlz.Scene2DPackage;
import com.jfixby.r3.ext.api.scene2d.srlz.SceneStructure;
import com.jfixby.r3.ext.p18t.red.RedP18Terrain;
import com.jfixby.r3.ext.red.terrain.RedTerrain;
import com.jfixby.r3.tools.api.iso.GeneratorParams;
import com.jfixby.r3.tools.api.iso.IsoMockPaletteGenerator;
import com.jfixby.r3.tools.api.iso.IsoMockPaletteResult;
import com.jfixby.r3.tools.iso.red.RedIsoMockPaletteGenerator2;
import com.jfixby.rana.api.pkg.StandardPackageFormats;
import com.jfixby.rana.api.pkg.fs.PackageDescriptor;
import com.jfixby.red.engine.core.resources.PackageUtils;
import com.jfixby.scarabei.adopted.gdx.GdxSimpleTriangulator;
import com.jfixby.scarabei.adopted.gdx.json.GdxJson;
import com.jfixby.scarabei.api.assets.ID;
import com.jfixby.scarabei.api.assets.Names;
import com.jfixby.scarabei.api.collections.Collection;
import com.jfixby.scarabei.api.collections.Collections;
import com.jfixby.scarabei.api.collections.List;
import com.jfixby.scarabei.api.color.Colors;
import com.jfixby.scarabei.api.desktop.ImageAWT;
import com.jfixby.scarabei.api.desktop.ScarabeiDesktop;
import com.jfixby.scarabei.api.file.File;
import com.jfixby.scarabei.api.file.LocalFileSystem;
import com.jfixby.scarabei.api.io.IO;
import com.jfixby.scarabei.api.java.ByteArray;
import com.jfixby.scarabei.api.json.Json;
import com.jfixby.scarabei.api.math.SimpleTriangulator;
import com.jfixby.scarabei.red.desktop.image.RedImageAWT;
import com.jfixby.texture.slicer.api.TextureSlicer;
import com.jfixby.texture.slicer.red.RedTextureSlicer;
import com.jfixby.tools.bleed.api.TextureBleed;
import com.jfixby.tools.gdx.texturepacker.GdxTexturePacker;
import com.jfixby.tools.gdx.texturepacker.api.AtlasPackingResult;
import com.jfixby.tools.gdx.texturepacker.api.Packer;
import com.jfixby.tools.gdx.texturepacker.api.TexturePacker;
import com.jfixby.tools.gdx.texturepacker.api.TexturePackingSpecs;
import com.jfixby.util.iso.api.Isometry;
import com.jfixby.util.iso.red.RedIsometry;
import com.jfixby.util.p18t.api.P18Terrain;
import com.jfixby.util.patch18.red.RedP18;
import com.jfixby.util.terain.test.api.palette.Terrain;
import com.jfixby.utl.pizza.api.Pizza;
import com.jfixby.utl.pizza.red.RedPizza;

public class GenareteISOMocks_WDGS {

	private static final String TARGE_BANK_FOLDER_PATH = "D:/[DEV]/[GIT]/StarCraft/sc-assets/assets/com.jfixby.sc.assets.local/tank-0";

	public static void main (final String[] args) throws IOException {
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

		final GeneratorParams specs = IsoMockPaletteGenerator.newIsoMockPaletteGeneratorParams();

		final File output_folder = LocalFileSystem.ApplicationHome().child("iso-output");

		final File mock_palette_folder = output_folder.child("wdgs");

		specs.setOutputFolder(mock_palette_folder);

		specs.setPizzaPalette(WDGS_Pizza_Palette.PALETTE);

		specs.setFabricColor(WDGS_P18_Palette.GRASS, Colors.GREEN());
		specs.setFabricColor(WDGS_P18_Palette.DIRT, Colors.BROWN());
		specs.setFabricColor(WDGS_P18_Palette.WATER, Colors.BLUE());
		specs.setFabricColor(WDGS_P18_Palette.SNOW, Colors.WHITE());

		specs.setPadding(64);

		final IsoMockPaletteResult result = IsoMockPaletteGenerator.generate(specs);

		result.print();

		final File bank_folder = LocalFileSystem.newFile(TARGE_BANK_FOLDER_PATH);
		bank_folder.makeFolder();
		packScenes(result, bank_folder);
		packRaster(result, bank_folder);

	}

	private static void packScenes (final IsoMockPaletteResult result, final File bank_folder) throws IOException {
		final Scene2DPackage struct = result.getScene2DPackage();
		final String package_name = result.getNamespace().child(Scene2DPackage.SCENE2D_PACKAGE_FILE_EXTENSION).toString();
		final String file_name = package_name;

		final File package_folder = bank_folder.child(package_name);
		final File package_content_folder = package_folder.child(PackageDescriptor.PACKAGE_CONTENT_FOLDER);
		package_content_folder.makeFolder();
		final File package_root_file = package_content_folder.child(file_name);

		final List<ID> packed = Collections.newList();

		final Collection<ID> dependencies = result.getAssetsUsed();

		for (int i = 0; i < struct.structures.size(); i++) {
			final SceneStructure structure = struct.structures.get(i);
			final ID asset_id = Names.newID(structure.structure_name);
			packed.add(asset_id);
		}

		final ByteArray data = IO.serialize(struct);
		package_root_file.writeBytes(data);

		PackageUtils.producePackageDescriptor(package_folder, Scene2DPackage.SCENE2D_PACKAGE_FORMAT, "1.0", packed, dependencies,
			file_name);
	}

	private static void packRaster (final IsoMockPaletteResult result, final File bank_folder) throws IOException {

		final File raster = result.getRasterOutputFolder();
		// L.d("raster", raster);

		final TexturePackingSpecs specs = TexturePacker.newPackingSpecs();
		final String package_name = result.getNamespace().child("raster").toString();
		specs.setOutputAtlasFileName(package_name);

		final File package_folder = bank_folder.child(package_name);
		// L.d("package_folder", package_folder);
		// Sys.exit();

		final File package_content_folder = package_folder.child(PackageDescriptor.PACKAGE_CONTENT_FOLDER);
		package_content_folder.makeFolder();
		specs.setOutputAtlasFolder(package_content_folder);
		specs.setInputRasterFolder(raster);

		final Packer packer = TexturePacker.newPacker(specs);
		final AtlasPackingResult atlas_result = packer.pack();

		atlas_result.print();

		final File altas_file = atlas_result.getAtlasOutputFile();
		final String atlas_name = altas_file.getName();

		final Collection<ID> packed = atlas_result.listPackedAssets();
		packed.print("packed");

		PackageUtils.producePackageDescriptor(package_folder, StandardPackageFormats.libGDX.Atlas, "1.0", packed,
			Collections.newList(), atlas_name);

	}

}

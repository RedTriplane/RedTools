
package com.jfixby.r3.tools.iso.run;

import java.io.IOException;

import com.jfixby.examples.wdgs.WDGS_P18_Palette;
import com.jfixby.examples.wdgs.WDGS_Pizza_Palette;
import com.jfixby.r3.ext.api.scene2d.srlz.Scene2DPackage;
import com.jfixby.r3.ext.api.scene2d.srlz.SceneStructure;
import com.jfixby.r3.tools.api.iso.GeneratorParams;
import com.jfixby.r3.tools.api.iso.IsoMockPaletteGenerator;
import com.jfixby.r3.tools.api.iso.IsoMockPaletteResult;
import com.jfixby.r3.tools.iso.red.RedIsoMockPaletteGenerator2;
import com.jfixby.rana.api.pkg.StandardPackageFormats;
import com.jfixby.rana.api.pkg.fs.PackageDescriptor;
import com.jfixby.red.engine.core.resources.PackageUtils;
import com.jfixby.scarabei.api.assets.ID;
import com.jfixby.scarabei.api.assets.Names;
import com.jfixby.scarabei.api.collections.Collection;
import com.jfixby.scarabei.api.collections.Collections;
import com.jfixby.scarabei.api.collections.List;
import com.jfixby.scarabei.api.color.Colors;
import com.jfixby.scarabei.api.desktop.ScarabeiDesktop;
import com.jfixby.scarabei.api.file.File;
import com.jfixby.scarabei.api.file.LocalFileSystem;
import com.jfixby.scarabei.api.json.Json;
import com.jfixby.tools.gdx.texturepacker.api.AtlasPackingResult;
import com.jfixby.tools.gdx.texturepacker.api.Packer;
import com.jfixby.tools.gdx.texturepacker.api.TexturePacker;
import com.jfixby.tools.gdx.texturepacker.api.TexturePackingSpecs;
import com.jfixby.util.iso.api.Isometry;
import com.jfixby.util.iso.red.RedIsometry;

public class GenareteISOMocks_WDGS {

	public static void main (final String[] args) throws IOException {
		ScarabeiDesktop.deploy();

		Isometry.installComponent(new RedIsometry());
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

		final File bank_folder = LocalFileSystem.newFile("D:\\[DATA]\\[RED-ASSETS]\\TintoAssets\\tinto-assets")
			.child("bank-florida");
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

		final String data = Json.serializeToString(struct).toString();
		package_root_file.writeString(data);

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

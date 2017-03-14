
package com.jfixby.r3.tools.iso.red;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import com.jfixby.r3.ext.api.patch18.Patch18;
import com.jfixby.r3.ext.api.patch18.palette.Fabric;
import com.jfixby.r3.ext.api.patch18.palette.FabricsRelation;
import com.jfixby.r3.ext.api.scene2d.srlz.LayerElement;
import com.jfixby.r3.ext.api.scene2d.srlz.LayerElementFactory;
import com.jfixby.r3.ext.api.scene2d.srlz.Scene2DPackage;
import com.jfixby.r3.ext.api.scene2d.srlz.SceneStructure;
import com.jfixby.r3.tools.api.iso.GeneratorParams;
import com.jfixby.r3.tools.api.iso.IsoMockPaletteGeneratorComponent;
import com.jfixby.r3.tools.api.iso.IsoMockPaletteResult;
import com.jfixby.scarabei.api.arrays.Arrays;
import com.jfixby.scarabei.api.assets.ID;
import com.jfixby.scarabei.api.collections.Collection;
import com.jfixby.scarabei.api.collections.Collections;
import com.jfixby.scarabei.api.collections.EditableCollection;
import com.jfixby.scarabei.api.collections.List;
import com.jfixby.scarabei.api.collections.Mapping;
import com.jfixby.scarabei.api.collections.Set;
import com.jfixby.scarabei.api.debug.Debug;
import com.jfixby.scarabei.api.desktop.ImageAWT;
import com.jfixby.scarabei.api.file.File;
import com.jfixby.scarabei.api.floatn.Float2;
import com.jfixby.scarabei.api.floatn.Float3;
import com.jfixby.scarabei.api.floatn.ReadOnlyFloat2;
import com.jfixby.scarabei.api.geometry.ClosedPolygonalChain;
import com.jfixby.scarabei.api.geometry.Geometry;
import com.jfixby.scarabei.api.geometry.PolyTriangulation;
import com.jfixby.scarabei.api.geometry.Rectangle;
import com.jfixby.scarabei.api.geometry.Triangle;
import com.jfixby.scarabei.api.json.Json;
import com.jfixby.scarabei.api.log.L;
import com.jfixby.util.iso.api.IsoTransform;
import com.jfixby.util.p18t.api.P18TerrainTypeVariation;
import com.jfixby.util.p18t.api.P18TerrainTypeVariationsList;
import com.jfixby.util.terain.test.api.palette.TerrainType;
import com.jfixby.utl.pizza.api.PizzaPalette;

public class RedIsoMockPaletteGenerator implements IsoMockPaletteGeneratorComponent {

	@Override
	public GeneratorParams newIsoMockPaletteGeneratorParams () {
		return new RedGeneratorSpecs();
	}

	@Override
	public IsoMockPaletteResult generate (final GeneratorParams specs) throws IOException {
		final RedIsoMockPaletteResult result = new RedIsoMockPaletteResult();

		Debug.checkNull("params", specs);
		final File output_folder = Debug.checkNull("output_folder", specs.getOutputFolder());
		result.setOutputFolder(output_folder);

		final File raster_output = output_folder.child("raster");
		raster_output.makeFolder();
		raster_output.clearFolder();
		result.setRasterOutput(raster_output);

		final PizzaPalette pizza_palette = Debug.checkNull("getPizzaPalette", specs.getPizzaPalette());

		final ID namespace = Debug.checkNull("namespace", pizza_palette.getNamespace());
		result.setNamespace(namespace);

		final IsoTransform isometry = Debug.checkNull("IsoTransform", pizza_palette.getIsoTransform());

		final int padding = specs.getPadding();

		L.d("palette namespace", namespace);

		// terrain_palette.print();
		output_folder.makeFolder();

		final Mapping<Fabric, com.jfixby.scarabei.api.color.Color> colors = Collections.newMap(specs.getFabricColors());

		final Mapping<FabricsRelation, Float2> centers = pizza_palette.getRelationRelativeCenters();

		// centers.print("centers");
		// Collection<TerrainBlock> all_blocks =
		// terrain_palette.listAllBlocks();
		// for (int i = 0; i < all_blocks.size(); i++) {
		// TerrainBlock block = all_blocks.getElementAt(i);
		// AssetID name = block.getName();
		// L.d("processing", name);
		// }

		final Scene2DPackage structures = new Scene2DPackage();

		// Patch18Palette p18_palette = patch18_palette.getPatch18Palette();
		// TerrainPalette terrain_palette = patch18_palette.getTerrainPalette();
		//
		// RelationsList relations = p18_palette.listRelations();
		// for (int i = 0; i < relations.size(); i++) {
		// FabricsRelation relation = relations.getRelation(i);
		//
		// proc_relation(relation, namespace, result, output_folder, isometry,
		// terrain_palette, padding, colors, centers, structures);
		//
		// }

		final Collection<P18TerrainTypeVariationsList> variations = pizza_palette.getP18TerrainPalette().listVariationsAll();
		final Set<Patch18> unprocessed = Arrays.newSet(Patch18.values());
		for (int i = 0; i < variations.size(); i++) {
			final P18TerrainTypeVariationsList var = variations.getElementAt(i);
			for (int k = 0; k < var.size(); k++) {
				final P18TerrainTypeVariation variation = var.getVariation(k);
				// String name = variation.getName();
				// AssetID id = variation.getID();
				// L.d("loading", variation);

				final Patch18 p18 = variation.getShape();
				final FabricsRelation relation = variation.getRelation();

				final TerrainType element = variation.getProperties();

				final ID tile_id = element.getID();
				L.d("processing block", tile_id);

				final Rectangle wrapping_frame = Geometry.newRectangle();
				final ClosedPolygonalChain tile_shape = Geometry.newClosedPolygonalChain();
				this.setup_shape(tile_shape, isometry, element);

				Geometry.setupWrapingFrame(tile_shape.listVertices(), wrapping_frame);
				// L.d("wrapping_frame", wrapping_frame);

				final ID raster_id = tile_id.child("raster").child("mock");

				result.addDependency(raster_id);
				final double pixels_to_tile_meter = isometry.getPixelsToGameMeter();
				final int color_function_height = (int)(wrapping_frame.getHeight() * pixels_to_tile_meter + padding * 2);
				final int color_function_width = (int)(wrapping_frame.getWidth() * pixels_to_tile_meter + padding * 2);

				final BufferedImage buffer = new BufferedImage(color_function_width, color_function_height,
					BufferedImage.TYPE_INT_ARGB);

				this.fill(buffer, p18, pixels_to_tile_meter, wrapping_frame, padding, colors, relation, isometry, element, centers,
					unprocessed, tile_id, raster_id, structures);

				ImageAWT.writeToFile(buffer, raster_output.child(raster_id + ".png"), "png");
				// break;
			}
			// break;
		}

		final String structures_string = Json.serializeToString(structures).toString();
		final String structures_file_name = namespace.child(Scene2DPackage.SCENE2D_PACKAGE_FILE_EXTENSION).toString();
		final File struct_file = output_folder.child(structures_file_name);
		L.d("writing", struct_file);
		struct_file.writeString(structures_string);

		result.setScene2DPackage(structures);
		result.setSceneStructuresFile(struct_file);
		return result;
	}

	private void setup_shape (final ClosedPolygonalChain tile_shape, final IsoTransform isometry, final TerrainType element) {
		final List<Float3> input = Collections.newList();
		final List<Float2> output = Collections.newList();
		{
			final Float3 vertex = Geometry.newFloat3();
			input.add(vertex);
			final double x = element.getXWidth().toDouble() * 0;
			final double y = element.getYWidth().toDouble() * 0;
			final double z = element.getHeight().toDouble() * 0;
			vertex.setXYZ(x, y, z);
		}
		{
			final Float3 vertex = Geometry.newFloat3();
			input.add(vertex);
			final double x = element.getXWidth().toDouble() * 1;
			final double y = element.getYWidth().toDouble() * 0;
			final double z = element.getHeight().toDouble() * 0;
			vertex.setXYZ(x, y, z);
		}
		{
			final Float3 vertex = Geometry.newFloat3();
			input.add(vertex);
			final double x = element.getXWidth().toDouble() * 1;
			final double y = element.getYWidth().toDouble() * 1;
			final double z = element.getHeight().toDouble() * 0;
			vertex.setXYZ(x, y, z);
		}
		{
			final Float3 vertex = Geometry.newFloat3();
			input.add(vertex);
			final double x = element.getXWidth().toDouble() * 0;
			final double y = element.getYWidth().toDouble() * 1;
			final double z = element.getHeight().toDouble() * 0;
			vertex.setXYZ(x, y, z);
		}
		{
			final Float3 vertex = Geometry.newFloat3();
			input.add(vertex);
			final double x = element.getXWidth().toDouble() * 0;
			final double y = element.getYWidth().toDouble() * 0;
			final double z = element.getHeight().toDouble() * 0;
			vertex.setXYZ(x, y, z);
		}
		{
			final Float3 vertex = Geometry.newFloat3();
			input.add(vertex);
			final double x = element.getXWidth().toDouble() * 0;
			final double y = element.getYWidth().toDouble() * 0;
			final double z = element.getHeight().toDouble() * 1;
			vertex.setXYZ(x, y, z);
		}
		{
			final Float3 vertex = Geometry.newFloat3();
			input.add(vertex);
			final double x = element.getXWidth().toDouble() * 1;
			final double y = element.getYWidth().toDouble() * 0;
			final double z = element.getHeight().toDouble() * 1;
			vertex.setXYZ(x, y, z);
		}
		{
			final Float3 vertex = Geometry.newFloat3();
			input.add(vertex);
			final double x = element.getXWidth().toDouble() * 1;
			final double y = element.getYWidth().toDouble() * 1;
			final double z = element.getHeight().toDouble() * 1;
			vertex.setXYZ(x, y, z);
		}
		{
			final Float3 vertex = Geometry.newFloat3();
			input.add(vertex);
			final double x = element.getXWidth().toDouble() * 0;
			final double y = element.getYWidth().toDouble() * 1;
			final double z = element.getHeight().toDouble() * 1;
			vertex.setXYZ(x, y, z);
		}
		{
			final Float3 vertex = Geometry.newFloat3();
			input.add(vertex);
			final double x = element.getXWidth().toDouble() * 0;
			final double y = element.getYWidth().toDouble() * 0;
			final double z = element.getHeight().toDouble() * 1;
			vertex.setXYZ(x, y, z);
		}
		isometry.project3Dto2D(input, output);
		tile_shape.setupVertices(output);

	}

	private void fill (final BufferedImage img, final Patch18 p18, final double pixels_to_tile_meter,
		final Rectangle wrapping_frame, final int padding, final Mapping<Fabric, com.jfixby.scarabei.api.color.Color> colors,
		final FabricsRelation relation, final IsoTransform isometry, final TerrainType element,
		final Mapping<FabricsRelation, Float2> centers, final Set<Patch18> unprocessed, final ID tile_id, final ID raster_id,
		final Scene2DPackage structures) {

		final Rectangle image_frame = Geometry.newRectangle();
		image_frame.setSize(wrapping_frame.getWidth() * pixels_to_tile_meter, wrapping_frame.getHeight() * pixels_to_tile_meter);
		image_frame.setPosition(padding, padding);

		{
			final SceneStructure structure = new SceneStructure();
			final LayerElementFactory factory = new LayerElementFactory(structure);
			structures.structures.add(structure);

			structure.structure_name = tile_id.toString();

			final LayerElement raster_info = factory.newLayerElement();
			;
			structure.root.children.addElement(raster_info, structure);

			raster_info.is_hidden = false;
			raster_info.name = raster_id.getLastStep();
			raster_info.is_raster = true;

			raster_info.width = img.getWidth();
			raster_info.height = img.getHeight();

			raster_info.position_x = 0;
			raster_info.position_y = 0;

			final Float3 origin3d = Geometry.newFloat3(0, 0, 0);
			final Float2 origin2d = Geometry.newFloat2();
			isometry.project3Dto2D(origin3d, origin2d);
			this.ajust(origin2d, wrapping_frame, image_frame);

			raster_info.origin_relative_x = origin2d.getX() / raster_info.width;
			raster_info.origin_relative_y = origin2d.getY() / raster_info.height;

			// raster_info.origin_relative_x = 0.5;
			// raster_info.origin_relative_y = 0.5;

			raster_info.raster_id = raster_id.toString();
		}

		final Graphics2D g2 = img.createGraphics();

		final double X = element.getXWidth().toDouble();
		final double Y = element.getYWidth().toDouble();
		final double Z = element.getHeight().toDouble();

		// g2.setColor(new Color(0, 0, 255, 8));
		// g2.fillRect(0, 0, img.getWidth(), img.getHeight());
		{
			final Fabric lower_fabric = relation.getLowerFabric();
			final com.jfixby.scarabei.api.color.Color jcolor = colors.get(lower_fabric);

			{
				final EditableCollection<Float2> center_2d = this.getCenter(centers, relation, X, Y, 0, isometry,
					Collections.newList());
				;

				final List<Float3> corners_3d = Collections.newList();
				corners_3d.add(Geometry.newFloat3(0, 0, 0));
				corners_3d.add(Geometry.newFloat3(X, 0, 0));
				corners_3d.add(Geometry.newFloat3(X, Y, 0));
				corners_3d.add(Geometry.newFloat3(0, Y, 0));

				final EditableCollection<Float2> corners_2d = Geometry.newFloat2(Collections.newList(), 4);
				isometry.project3Dto2D(corners_3d, corners_2d);

				this.ajust(corners_2d, wrapping_frame, image_frame);
				this.ajust(center_2d, wrapping_frame, image_frame);

				g2.setColor(new Color(jcolor.red(), jcolor.green(), jcolor.blue(), jcolor.alpha()));

				this.fillTriangle(g2, corners_2d.getElementAt(0), corners_2d.getElementAt(1), corners_2d.getElementAt(2));
				this.fillTriangle(g2, corners_2d.getElementAt(2), corners_2d.getElementAt(3), corners_2d.getElementAt(0));
				g2.setColor(Color.black);
				this.drawPoly(g2, center_2d);

			}
		}
		{
			final Fabric upper_fabric = relation.getUpperFabric();
			final com.jfixby.scarabei.api.color.Color jcolor = colors.get(upper_fabric);
			final List<Float3> upper_ring_3d = Collections.newList();
			final List<Float2> upper_ring_2d = Collections.newList();
			final List<Float2> lower_ring_2d = Collections.newList();

			final EditableCollection<Float2> center_2d = this.getCenter(centers, relation, X, Y, Z, isometry, upper_ring_3d);
			final EditableCollection<Float3> lower_ring_3d = Geometry.newFloat3(Collections.newList(), 4);
			for (int i = 0; i < lower_ring_3d.size(); i++) {
				lower_ring_3d.getElementAt(i).setXYZ(upper_ring_3d.getElementAt(i));
				lower_ring_3d.getElementAt(i).setZ(0);
			}
			{
				final List<Float3> corners_3d = Collections.newList();
				corners_3d.add(Geometry.newFloat3(0, 0, Z));
				corners_3d.add(Geometry.newFloat3(X, 0, Z));
				corners_3d.add(Geometry.newFloat3(X, Y, Z));
				corners_3d.add(Geometry.newFloat3(0, Y, Z));

				final EditableCollection<Float2> corners_2d = Geometry.newFloat2(

					Collections.newList(), 4);

				final Float3 H_3d = Geometry.newFloat3(0, 0, -Z);
				final Float2 H_2d = Geometry.newFloat2();

				isometry.project3Dto2D(H_3d, H_2d);
				isometry.project3Dto2D(corners_3d, corners_2d);
				isometry.project3Dto2D(upper_ring_3d, upper_ring_2d);
				isometry.project3Dto2D(lower_ring_3d, lower_ring_2d);

				// wrapping_frame.toRelative(H_2d);
				// image_frame.toAbsolute(H_2d);
				//
				// H_2d.add(-image_frame.getPosition().getX(), -image_frame
				// .getPosition().getY());F

				H_2d.scaleXY(pixels_to_tile_meter);

				this.ajust(corners_2d, wrapping_frame, image_frame);
				this.ajust(center_2d, wrapping_frame, image_frame);
				this.ajust(upper_ring_2d, wrapping_frame, image_frame);
				this.ajust(lower_ring_2d, wrapping_frame, image_frame);

				final List<Float2> TL = Geometry.newFloat2List(3);
				TL.getElementAt(0).set(corners_2d.getElementAt(0));
				TL.getElementAt(1).set(upper_ring_2d.getElementAt(0));
				TL.getElementAt(2).set(upper_ring_2d.getElementAt(1));

				final List<Float2> TR = Geometry.newFloat2List(3);
				TR.getElementAt(0).set(corners_2d.getElementAt(1));
				TR.getElementAt(1).set(upper_ring_2d.getElementAt(1));
				TR.getElementAt(2).set(upper_ring_2d.getElementAt(2));

				final List<Float2> DR = Geometry.newFloat2List(3);
				DR.getElementAt(0).set(corners_2d.getElementAt(2));
				DR.getElementAt(1).set(upper_ring_2d.getElementAt(2));
				DR.getElementAt(2).set(upper_ring_2d.getElementAt(3));

				final List<Float2> DL = Geometry.newFloat2List(3);
				DL.getElementAt(0).set(corners_2d.getElementAt(3));
				DL.getElementAt(1).set(upper_ring_2d.getElementAt(3));
				DL.getElementAt(2).set(upper_ring_2d.getElementAt(0));
				final float T = 1f;
				// boolean top = true;
				{
					this.drawSurface(!true, g2, jcolor, TL, TR, DL, DR, T, H_2d, p18, unprocessed, lower_ring_2d, corners_2d,
						Collections.newList(upper_ring_2d), Collections.newList(center_2d));
					this.drawSurface(true, g2, jcolor, TL, TR, DL, DR, T, H_2d, p18, unprocessed, lower_ring_2d, corners_2d,
						Collections.newList(upper_ring_2d), Collections.newList(center_2d));

				}
			}
		}

		g2.dispose();

	}

	private void drawSurface (final boolean top, final Graphics2D g2, final com.jfixby.scarabei.api.color.Color jcolor,
		final List<Float2> TL, final List<Float2> TR, final List<Float2> DL, final List<Float2> DR, final float T,
		final Float2 H_2d, final Patch18 p18, final Set<Patch18> unprocessed, final List<Float2> lower_ring_2d,
		final EditableCollection<Float2> corners_2d, final List<Float2> upper_ring_2d, final EditableCollection<Float2> center_2d) {

		g2.setColor(new Color(0, 0, 0, 0.3f));
		// drawPoly(g2, lower_ring_2d);
		g2.setColor(new Color(jcolor.red(), jcolor.green(), jcolor.blue(), jcolor.alpha() * T));

		if (p18.isBlocked()) {
			this.fillPoly(g2, corners_2d, H_2d, top);
			unprocessed.remove(p18);
		}
		if (p18.isFree()) {
			// this.fillPoly(g2, corners_2d);
			unprocessed.remove(p18);
		}

		if (p18.isErr()) {
			g2.setColor(new Color(1, 0, 0, T));

			this.fillPoly(g2, corners_2d, H_2d, top);
			unprocessed.remove(p18);
		}
		if (p18.isIrrelevant()) {
			g2.setColor(new Color(0.5f, 0.5f, 0.5f, T));

			this.fillPoly(g2, corners_2d, H_2d, top);
			unprocessed.remove(p18);
		}

		if (p18.isLookingDownRight()) {
			this.fillPoly(g2, TL, H_2d, top);
			unprocessed.remove(p18);
		}
		if (p18.isLookingDownLeft()) {
			this.fillPoly(g2, TR, H_2d, top);
			unprocessed.remove(p18);
		}
		if (p18.isLookingUpRight()) {
			this.fillPoly(g2, DL, H_2d, top);
			unprocessed.remove(p18);
		}
		if (p18.isLookingUpLeft()) {
			this.fillPoly(g2, DR, H_2d, top);
			unprocessed.remove(p18);
		}
		if (p18.isBottomLeftCorner()) {
			this.fillPoly(g2, DL, H_2d, top);
			this.fillPoly(g2, TL, H_2d, top);
			this.fillPoly(g2, DR, H_2d, top);
			// this.fillPoly(g2, TR);
			this.fillPoly(g2, upper_ring_2d, H_2d, top);
			unprocessed.remove(p18);

		}
		if (p18.isBottomRightCorner()) {
			this.fillPoly(g2, DL, H_2d, top);
			// this.fillPoly(g2, TL);
			this.fillPoly(g2, DR, H_2d, top);
			this.fillPoly(g2, TR, H_2d, top);
			this.fillPoly(g2, upper_ring_2d, H_2d, top);
			unprocessed.remove(p18);
		}
		if (p18.isTopRightCorner()) {
			// this.fillPoly(g2, DL);
			this.fillPoly(g2, TL, H_2d, top);
			this.fillPoly(g2, DR, H_2d, top);
			this.fillPoly(g2, TR, H_2d, top);
			this.fillPoly(g2, upper_ring_2d, H_2d, top);
			unprocessed.remove(p18);
		}
		if (p18.isTopLeftCorner()) {
			this.fillPoly(g2, DL, H_2d, top);
			this.fillPoly(g2, TL, H_2d, top);
			// this.fillPoly(g2, DR);
			this.fillPoly(g2, TR, H_2d, top);
			this.fillPoly(g2, upper_ring_2d, H_2d, top);
			unprocessed.remove(p18);
		}
		if (p18.isLeftBridge()) {
			// this.fillPoly(g2, DL);
			this.fillPoly(g2, TL, H_2d, top);
			this.fillPoly(g2, DR, H_2d, top);
			// this.fillPoly(g2, TR);
			this.fillPoly(g2, upper_ring_2d, H_2d, top);
			unprocessed.remove(p18);
		}
		if (p18.isRightBridge()) {
			this.fillPoly(g2, DL, H_2d, top);
			// this.fillPoly(g2, TL);
			// this.fillPoly(g2, DR);
			this.fillPoly(g2, TR, H_2d, top);
			this.fillPoly(g2, upper_ring_2d, H_2d, top);
			unprocessed.remove(p18);
		}

		if (p18.isLookingUp()) {
			this.fillPoly(g2, DL, H_2d, top);
			// this.fillPoly(g2, TL);
			this.fillPoly(g2, DR, H_2d, top);
			// this.fillPoly(g2, TR);
			// this.fillPoly(g2, upper_ring_2d);
			upper_ring_2d.removeElementAt(1);
			this.fillPoly(g2, upper_ring_2d);

			unprocessed.remove(p18);
		}
		if (p18.isLookingDown()) {
			// this.fillPoly(g2, DL);
			this.fillPoly(g2, TL, H_2d, top);
			// this.fillPoly(g2, DR);
			this.fillPoly(g2, TR, H_2d, top);
			upper_ring_2d.removeElementAt(3);
			this.fillPoly(g2, upper_ring_2d, H_2d, top);
			unprocessed.remove(p18);
		}
		if (p18.isLookingRight()) {
			this.fillPoly(g2, DL, H_2d, top);
			this.fillPoly(g2, TL, H_2d, top);
			// this.fillPoly(g2, DR);
			// this.fillPoly(g2, TR);
			upper_ring_2d.removeElementAt(2);
			this.fillPoly(g2, upper_ring_2d, H_2d, top);
			unprocessed.remove(p18);
		}
		if (p18.isLookingLeft()) {
			// this.fillPoly(g2, DL);
			// this.fillPoly(g2, TL);
			this.fillPoly(g2, DR, H_2d, top);
			this.fillPoly(g2, TR, H_2d, top);
			upper_ring_2d.removeElementAt(0);
			this.fillPoly(g2, upper_ring_2d, H_2d, top);
			unprocessed.remove(p18);
		}

		g2.setColor(new Color(0, 0, 0, 0.3f));
		this.drawPoly(g2, center_2d);
		// drawPoly(g2, upper_ring_2d);

	}

	private void fillPoly (final Graphics2D g2, final Collection<Float2> upper_poly, final Float2 Z, final boolean t) {
		final Color color = g2.getColor();
		final Color darker_color = color.darker();
		for (int i = 0; i < upper_poly.size(); i++) {
			final List<Float2> tmp = Geometry.newFloat2List(4);
			int p = i + 1;
			if (p >= upper_poly.size()) {
				p = 0;
			}
			final Float2 I = upper_poly.getElementAt(i);
			final Float2 P = upper_poly.getElementAt(p);
			final Float2 lI = Geometry.newFloat2(upper_poly.getElementAt(i));
			final Float2 lP = Geometry.newFloat2(upper_poly.getElementAt(p));
			// L.d("Z", Z);
			// L.d("I", I);

			lI.add(Z);
			lP.add(Z);

			tmp.getElementAt(0).set(I);
			tmp.getElementAt(1).set(P);
			tmp.getElementAt(2).set(lP);
			tmp.getElementAt(3).set(lI);

			g2.setColor(darker_color);
			if (!t) {
				this.fillPoly(g2, tmp);
			}

		}
		g2.setColor(color);
		if (t) {
			this.fillPoly(g2, upper_poly);
		}
	}

	private void fillPoly (final Graphics2D g2, final Collection<Float2> vertices) {

		final ClosedPolygonalChain chain = Geometry.newClosedPolygonalChain(vertices);

		final PolyTriangulation triangulation = chain.getTriangulation();

		for (int i = 0; i < triangulation.size(); i++) {
			final Triangle triangle = triangulation.getTriangle(i);
			final ReadOnlyFloat2 A = triangle.A().transformed();
			final ReadOnlyFloat2 B = triangle.B().transformed();
			final ReadOnlyFloat2 C = triangle.C().transformed();
			this.fillTriangle(g2, A, B, C);

		}

	}

	private void drawPoly (final Graphics2D g2, final Collection<Float2> vertices) {
		Float2 A;
		Float2 B;
		for (int i = 0; i < vertices.size(); i++) {
			A = vertices.getElementAt(i);
			final int x1 = (int)(A.getX());
			final int y1 = (int)(A.getY());
			int p = i + 1;
			if (p >= vertices.size()) {
				p = 0;
			}
			B = vertices.getElementAt(p);
			final int x2 = (int)(B.getX());
			final int y2 = (int)(B.getY());

			g2.drawLine(x1, y1, x2, y2);

		}

	}

	private void ajust (final Collection<Float2> points, final Rectangle wrapping_frame, final Rectangle image_frame) {
		for (int i = 0; i < points.size(); i++) {
			final Float2 point = points.getElementAt(i);
			this.ajust(point, wrapping_frame, image_frame);
		}
	}

	private void ajust (final Float2 point, final Rectangle wrapping_frame, final Rectangle image_frame) {
		wrapping_frame.toRelative(point);
		image_frame.toAbsolute(point);
	}

	private EditableCollection<Float2> getCenter (final Mapping<FabricsRelation, Float2> centers, final FabricsRelation relation,
		final double X, final double Y, final double Z, final IsoTransform isometry, final List<Float3> upper_ring) {

		Float2 center = centers.get(relation);
		if (center == null) {
			center = Geometry.newFloat2(0.5, 0.5);
		}

		center.scaleXY(X, Y);
		final double dX = X / 5d;
		final double dY = Y / 5d;
		final EditableCollection<Float3> vertices_3d = Geometry.newFloat3(Collections.newList(), 9);
		vertices_3d.getElementAt(0).//
			setXYZ(center.getX() + (-0) * dX, center.getY() + (-0) * dY, Z);
		vertices_3d.getElementAt(1).//
			setXYZ(center.getX() + (-1) * dX, center.getY() + (-0) * dY, Z);
		vertices_3d.getElementAt(2).//
			setXYZ(center.getX() + (-0) * dX, center.getY() + (-0) * dY, Z);
		vertices_3d.getElementAt(3).//
			setXYZ(center.getX() + (-0) * dX, center.getY() + (-1) * dY, Z);
		vertices_3d.getElementAt(4).//
			setXYZ(center.getX() + (-0) * dX, center.getY() + (-0) * dY, Z);

		vertices_3d.getElementAt(5).//
			setXYZ(center.getX() + (-0) * dX, center.getY() + (-0) * dY, Z);
		vertices_3d.getElementAt(6).//
			setXYZ(center.getX() + (+1) * dX, center.getY() + (-0) * dY, Z);
		vertices_3d.getElementAt(7).//
			setXYZ(center.getX() + (-0) * dX, center.getY() + (-0) * dY, Z);
		vertices_3d.getElementAt(8).//
			setXYZ(center.getX() + (-0) * dX, center.getY() + (+1) * dY, Z);

		Geometry.newFloat3(upper_ring, 4);
		upper_ring.getElementAt(0).setXYZ(center.getX() * 0, center.getY() * 1, Z);
		upper_ring.getElementAt(1).setXYZ(center.getX() * 1, center.getY() * 0, Z);
		upper_ring.getElementAt(2).setXYZ(1, center.getY() * 1, Z);
		upper_ring.getElementAt(3).setXYZ(center.getX() * 1, 1, Z);

		final EditableCollection<Float2> vertices_2d = Geometry.newFloat2(Collections.newList(), 9);
		isometry.project3Dto2D(vertices_3d, vertices_2d);

		return vertices_2d;
	}

	private void fillTriangle (final Graphics2D g2, final ReadOnlyFloat2 A, final ReadOnlyFloat2 B, final ReadOnlyFloat2 C) {

		final int x1 = (int)(A.getX());
		final int y1 = (int)(A.getY());
		final int x2 = (int)(B.getX());
		final int y2 = (int)(B.getY());
		final int x3 = (int)(C.getX());
		final int y3 = (int)(C.getY());

		final int xpoints[] = {x1, x2, x3};
		final int ypoints[] = {y1, y2, y3};
		final int npoints = 3;

		g2.fillPolygon(xpoints, ypoints, npoints);

	}
}

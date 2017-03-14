
package com.jfixby.r3.tools.iso.red;

import java.awt.Graphics;
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
import com.jfixby.scarabei.api.color.Color;
import com.jfixby.scarabei.api.color.Colors;
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
import com.jfixby.scarabei.api.geometry.Vertex;
import com.jfixby.scarabei.api.json.Json;
import com.jfixby.scarabei.api.log.L;
import com.jfixby.scarabei.api.math.FloatMath;
import com.jfixby.util.iso.api.IsoTransform;
import com.jfixby.util.p18t.api.P18TerrainTypeVariation;
import com.jfixby.util.p18t.api.P18TerrainTypeVariationsList;
import com.jfixby.util.terain.test.api.palette.TerrainType;
import com.jfixby.utl.pizza.api.PizzaPalette;

public class RedIsoMockPaletteGenerator2 implements IsoMockPaletteGeneratorComponent {

	@Override
	public GeneratorParams newIsoMockPaletteGeneratorParams () {
		return new RedGeneratorSpecs();
	}

	@SuppressWarnings("unchecked")
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

		final Mapping<Fabric, Color> colors = Collections.newMap(specs.getFabricColors());

		final Mapping<FabricsRelation, Float2> centers = pizza_palette.getRelationRelativeCenters();

		final Scene2DPackage structures = new Scene2DPackage();

		final Collection<P18TerrainTypeVariationsList> variations = pizza_palette.getP18TerrainPalette().listVariationsAll();
		final Set<Patch18> unprocessed = Arrays.newSet(Patch18.values());
		for (int i = 0; i < variations.size(); i++) {
			final P18TerrainTypeVariationsList var = variations.getElementAt(i);
			for (int k = 0; k < var.size(); k++) {
				final P18TerrainTypeVariation variation = var.getVariation(k);

				final Patch18 p18 = variation.getShape();
				final FabricsRelation relation = variation.getRelation();

				final TerrainType element = variation.getProperties();

				final ID tile_id = element.getID();
				L.d("processing block", tile_id);

				final Float2 Cxy = centers.get(relation);
				final double c_x = Cxy.getX();
				final double c_y = Cxy.getY();

				final EditableCollection<Float3> all = Collections.newList();
				final EditableCollection<Float3> ground = Collections.newList();
				final EditableCollection<Float3> floor_bottom = Collections.newList();
				final EditableCollection<Float3> floor_mid = Collections.newList();
				{

					Geometry.fillUpFloat3(ground, 4);
					final double Wx = element.getXWidth().toDouble();
					final double Wy = element.getYWidth().toDouble();
					ground.getElementAt(0).setXYZ(0, 0, 0);
					ground.getElementAt(1).setXYZ(Wx, 0, 0);
					ground.getElementAt(2).setXYZ(Wx, Wy, 0);
					ground.getElementAt(3).setXYZ(0, Wy, 0);
					all.addAll(ground);
				}
				{

					Geometry.fillUpFloat3(floor_bottom, 4);
					final double Wx = element.getXWidth().toDouble();
					final double Wy = element.getYWidth().toDouble();
					final double Z = element.getAltitude().toDouble();
					floor_bottom.getElementAt(0).setXYZ(0, 0, Z);
					floor_bottom.getElementAt(1).setXYZ(Wx, 0, Z);
					floor_bottom.getElementAt(2).setXYZ(Wx, Wy, Z);
					floor_bottom.getElementAt(3).setXYZ(0, Wy, Z);
					all.addAll(floor_bottom);
				}
				final EditableCollection<Float3> floor_top = Collections.newList();
				{

					Geometry.fillUpFloat3(floor_top, 8);
					final double Wx = element.getXWidth().toDouble();
					final double Wy = element.getYWidth().toDouble();
					final double Z = element.getAltitude().toDouble() + element.getHeight().toDouble();
					floor_top.getElementAt(0).setXYZ(0, 0, Z);
					floor_top.getElementAt(1).setXYZ(Wx * c_x, 0, Z);
					floor_top.getElementAt(2).setXYZ(Wx, 0, Z);
					floor_top.getElementAt(3).setXYZ(Wx, Wy * c_y, Z);
					floor_top.getElementAt(4).setXYZ(Wx, Wy, Z);
					floor_top.getElementAt(5).setXYZ(Wx * c_x, Wy, Z);
					floor_top.getElementAt(6).setXYZ(0, Wy, Z);
					floor_top.getElementAt(7).setXYZ(0, Wy * c_y, Z);
					all.addAll(floor_top);
				}
				{

					Geometry.fillUpFloat3(floor_mid, 8);
					final double Wx = element.getXWidth().toDouble();
					final double Wy = element.getYWidth().toDouble();
					final double Z = element.getAltitude().toDouble();
					floor_mid.getElementAt(0).setXYZ(0, 0, Z);
					floor_mid.getElementAt(1).setXYZ(Wx * c_x, 0, Z);
					floor_mid.getElementAt(2).setXYZ(Wx, 0, Z);
					floor_mid.getElementAt(3).setXYZ(Wx, Wy * c_y, Z);
					floor_mid.getElementAt(4).setXYZ(Wx, Wy, Z);
					floor_mid.getElementAt(5).setXYZ(Wx * c_x, Wy, Z);
					floor_mid.getElementAt(6).setXYZ(0, Wy, Z);
					floor_mid.getElementAt(7).setXYZ(0, Wy * c_y, Z);
					all.addAll(floor_mid);
				}
				final Fabric lower_fabric = relation.getLowerFabric();
				Color lower_fabric_color = colors.get(lower_fabric);
				final Fabric upper_fabric = relation.getUpperFabric();
				Color upper_fabric_color = colors.get(upper_fabric);
				{
					if (p18.isBlocked()) {
						unprocessed.remove(p18);
					}
					if (p18.isFree()) {
						floor_top.clear();
						floor_mid.clear();
						unprocessed.remove(p18);
					}
					if (p18.isErr()) {
						upper_fabric_color = Colors.RED();
						lower_fabric_color = Colors.RED().customize().mix(Colors.BLACK(), 0.4f);
						unprocessed.remove(p18);
					}
					if (p18.isIrrelevant()) {
						upper_fabric_color = Colors.LIGHT_GRAY();
						lower_fabric_color = Colors.GRAY();
						// floor_mid.clear();
						unprocessed.remove(p18);
					}
					if (p18.isLeftBridge()) {
						this.removeIndexes(this.index(2, 6), floor_top, floor_mid);
						unprocessed.remove(p18);
					}
					if (p18.isRightBridge()) {
						this.removeIndexes(this.index(0, 4), floor_top, floor_mid);
						unprocessed.remove(p18);
					}
					if (p18.isBottomLeftCorner()) {
						this.removeIndexes(this.index(2), floor_top, floor_mid);
						unprocessed.remove(p18);
					}
					if (p18.isBottomRightCorner()) {
						this.removeIndexes(this.index(0), floor_top, floor_mid);
						unprocessed.remove(p18);
					}
					if (p18.isTopRightCorner()) {
						this.removeIndexes(this.index(6), floor_top, floor_mid);
						unprocessed.remove(p18);
					}
					if (p18.isTopLeftCorner()) {
						this.removeIndexes(this.index(4), floor_top, floor_mid);
						unprocessed.remove(p18);
					}
					if (p18.isLookingDown()) {
						this.removeIndexes(this.index(6, 5, 4), floor_top, floor_mid);
						unprocessed.remove(p18);
					}
					if (p18.isLookingUp()) {
						this.removeIndexes(this.index(0, 1, 2), floor_top, floor_mid);
						unprocessed.remove(p18);
					}
					if (p18.isLookingRight()) {
						this.removeIndexes(this.index(2, 3, 4), floor_top, floor_mid);
						unprocessed.remove(p18);
					}
					if (p18.isLookingLeft()) {
						this.removeIndexes(this.index(0, 7, 6), floor_top, floor_mid);
						unprocessed.remove(p18);
					}
					if (p18.isLookingUpLeft()) {
						this.removeIndexes(this.index(0, 7, 6, 1, 2), floor_top, floor_mid);
						unprocessed.remove(p18);
					}
					if (p18.isLookingUpRight()) {
						this.removeIndexes(this.index(0, 1, 2, 3, 4), floor_top, floor_mid);
						unprocessed.remove(p18);
					}
					if (p18.isLookingDownRight()) {
						this.removeIndexes(this.index(2, 3, 4, 5, 6), floor_top, floor_mid);
						unprocessed.remove(p18);
					}
					if (p18.isLookingDownLeft()) {
						this.removeIndexes(this.index(4, 5, 6, 7, 0), floor_top, floor_mid);
						unprocessed.remove(p18);
					}
				}

				final List<EditableCollection<Float3>> walls = this.build_walls(floor_top, floor_mid);

				final Rectangle wrapping_frame = Geometry.newRectangle();
				final Rectangle image_frame = Geometry.newRectangle();
				final EditableCollection<Float2> tile_shape = Collections.newList();
				isometry.project3Dto2D(all, tile_shape);
				isometry.project2DtoPixels(tile_shape);
				Geometry.setupWrapingFrame(tile_shape, wrapping_frame);
				// L.d("tile_shape", tile_shape);

				final ID raster_id = tile_id.child("raster").child("mock");
				//
				// result.addDependency(raster_id);
				// double pixels_to_tile_meter =
				// isometry.getPixelsToGameMeter();
				image_frame.setSize(wrapping_frame.getWidth(), wrapping_frame.getHeight());
				image_frame.setPosition(padding, padding);

				final int color_function_height = (int)FloatMath.round(wrapping_frame.getHeight() + padding * 2);
				final int color_function_width = (int)FloatMath.round(wrapping_frame.getWidth() + padding * 2);

				// wrapping_frame.setOriginRelative(0, 0);
				// wrapping_frame.setPosition(padding, padding);
				final int img_width = color_function_width + 1;
				final int img_height = color_function_height + 1;
				{
					final BufferedImage buffer = new BufferedImage(img_width, img_height, BufferedImage.TYPE_INT_ARGB);
					wrapping_frame.listVertices().print("wrapping_frame");
					final Graphics g2 = buffer.getGraphics();
					{

						// g2.setColor(java.awt.Color.BLACK);
						// drawPoly(g2, wrapping_frame.listVertices(),
						// wrapping_frame, image_frame);
						// drawPoly(g2, tile_shape, wrapping_frame,
						// image_frame);
					}
					{

						// fillPoly(g2, ground, wrapping_frame, image_frame,
						// isometry, Colors.DARK_GRAY());
					}
					{

						this.fillPoly(g2, floor_bottom, wrapping_frame, image_frame, isometry, lower_fabric_color);
					}
					{

						this.fillPoly(g2, floor_mid, wrapping_frame, image_frame, isometry, upper_fabric_color);
					}
					{

						this.fillPoly(g2, walls, wrapping_frame, image_frame, isometry,
							upper_fabric_color.customize().mix(Colors.BLACK(), 0.4f));

					}
					{

						this.fillPoly(g2, floor_top, wrapping_frame, image_frame, isometry, upper_fabric_color);
					}

					g2.dispose();
					ImageAWT.writeToFile(buffer, raster_output.child(raster_id + ".png"), "png");
				}
				{
					final SceneStructure structure = new SceneStructure();
					final LayerElementFactory factory = new LayerElementFactory(structure);
					structure.root = factory.newLayerElement();
					structures.structures.addElement(structure);

					structure.structure_name = tile_id.toString();
					result.addDependency(raster_id);

					final LayerElement raster_info = factory.newLayerElement();
					structure//
						.root//
						.children//
							.addElement(raster_info, structure);

					raster_info.is_hidden = false;
					raster_info.name = raster_id.getLastStep();
					raster_info.is_raster = true;

					raster_info.width = img_width;
					raster_info.height = img_height;

					raster_info.position_x = 0;
					raster_info.position_y = 0;

					final Float3 origin3d = Geometry.newFloat3(0, 0, 0);
					final Float2 origin2d = Geometry.newFloat2();
					isometry.project3Dto2D(origin3d, origin2d);
					isometry.project2DtoPixels(origin2d);
					// origin2d.add(padding, padding);
					wrapping_frame.toRelative(origin2d);
					image_frame.toAbsolute(origin2d);

					raster_info.origin_relative_x = origin2d.getX() / raster_info.width;
					raster_info.origin_relative_y = origin2d.getY() / raster_info.height;

					// raster_info.origin_relative_x = 0.5;
					// raster_info.origin_relative_y = 0.5;

					raster_info.raster_id = raster_id.toString();
				}
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

		// unprocessed.print("unprocessed");

		return result;
	}

	private List<EditableCollection<Float3>> build_walls (final EditableCollection<Float3> floor_top,
		final EditableCollection<Float3> floor_mid) {
		final List<EditableCollection<Float3>> walls = Collections.newList();
		final int N = floor_mid.size();
		for (int i = 0; i < N; i++) {
			final Float3 t0 = floor_top.getElementAt(i);
			final Float3 d0 = floor_mid.getElementAt(i);
			int j = i + 1;
			if (j >= N) {
				j = 0;
			}
			final Float3 t1 = floor_top.getElementAt(j);
			final Float3 d1 = floor_mid.getElementAt(j);
			final EditableCollection<Float3> wall = Collections.newList(t0, t1, d1, d0);
			walls.add(wall);
		}
		return walls;
	}

	private int[] index (final int... index) {
		return index;
	}

	private void removeIndexes (final int[] indexes, final EditableCollection<Float3>... collections) {
		final List<EditableCollection<Float3>> collections_list = Collections.newList(collections);
		for (int i = 0; i < collections_list.size(); i++) {
			final EditableCollection<Float3> collection = collections_list.getElementAt(i);
			this.removeIndexes(collection, indexes);
		}

	}

	private void removeIndexes (final EditableCollection<Float3> collection, final int[] indexes) {

		final List<Object> to_remove = Collections.newList();
		for (int i = 0; i < indexes.length; i++) {
			final int K = indexes[i];
			final Object element = collection.getElementAt(K);
			to_remove.add(element);
		}
		for (int i = 0; i < to_remove.size(); i++) {
			final Object dot = to_remove.getElementAt(i);
			collection.remove(dot);
		}
	}

	private void fillPoly (final Graphics g2, final EditableCollection<Float3> shape_3d, final Rectangle wrapping_frame,
		final Rectangle image_frame, final IsoTransform isometry, final Color jcolor) {
		final EditableCollection<Float2> shape_2d = Collections.newList();
		isometry.project3Dto2D(shape_3d, shape_2d);
		isometry.project2DtoPixels(shape_2d);
		this.fillPoly(g2, shape_2d, wrapping_frame, image_frame, jcolor);
	}

	private void fillPoly (final Graphics g2, final Collection<EditableCollection<Float3>> shapes_3d,
		final Rectangle wrapping_frame, final Rectangle image_frame, final IsoTransform isometry, final Color jcolor) {
		for (int i = 0; i < shapes_3d.size(); i++) {
			final EditableCollection<Float3> shape_3d = shapes_3d.getElementAt(i);
			this.fillPoly(g2, shape_3d, wrapping_frame, image_frame, isometry, jcolor);
		}
	}

	private void fillPoly (final Graphics g2, final EditableCollection<Float2> shape_2d, final Rectangle wrapping_frame,
		final Rectangle image_frame, final Color jcolor) {
		final ClosedPolygonalChain chain = Geometry.newClosedPolygonalChain(shape_2d);
		final PolyTriangulation triangles = chain.getTriangulation();
		final int N = triangles.size();
		for (int i = 0; i < N; i++) {
			final Triangle triangle = triangles.getTriangle(i);
			final Vertex a = triangle.A();
			final Vertex b = triangle.B();
			final Vertex c = triangle.C();
			final Float2 A = this.ajust(a, wrapping_frame, image_frame);
			final Float2 B = this.ajust(b, wrapping_frame, image_frame);
			final Float2 C = this.ajust(c, wrapping_frame, image_frame);
			final int x1 = (int)FloatMath.round(A.getX());
			final int y1 = (int)FloatMath.round(A.getY());
			final int x2 = (int)FloatMath.round(B.getX());
			final int y2 = (int)FloatMath.round(B.getY());
			final int x3 = (int)FloatMath.round(C.getX());
			final int y3 = (int)FloatMath.round(C.getY());

			final int xpoints[] = {x1, x2, x3};
			final int ypoints[] = {y1, y2, y3};
			final int npoints = 3;
			this.setColor(g2, jcolor);
			g2.fillPolygon(xpoints, ypoints, npoints);

			this.setColor(g2, Colors.DARK_GRAY().customize().setAlpha(0.5f));
			// g2.drawLine(x1, y1, x2, y2);
			// g2.drawLine(x3, y3, x2, y2);
			// g2.drawLine(x3, y3, x1, y1);

		}

	}

	private void setColor (final Graphics g2, final Color jcolor) {
		final java.awt.Color awt_color = new java.awt.Color(jcolor.red(), jcolor.green(), jcolor.blue(), jcolor.alpha());
		g2.setColor(awt_color);
	}

	private void drawPoly (final Graphics g2, final Collection<? extends ReadOnlyFloat2> tile_shape,
		final Rectangle wrapping_frame, final Rectangle image_frame) {
		for (int i = 0; i < tile_shape.size(); i++) {

			int j = i + 1;
			if (j >= tile_shape.size()) {
				j = 0;
			}
			final Float2 A = this.ajust(tile_shape.getElementAt(i), wrapping_frame, image_frame);
			final Float2 B = this.ajust(tile_shape.getElementAt(j), wrapping_frame, image_frame);
			final int x1 = (int)FloatMath.round(A.getX());
			final int y1 = (int)FloatMath.round(A.getY());
			final int x2 = (int)FloatMath.round(B.getX());
			final int y2 = (int)FloatMath.round(B.getY());
			g2.drawLine(x1, y1, x2, y2);

		}
	}

	private Float2 ajust (final ReadOnlyFloat2 elementAt, final Rectangle wrapping_frame, final Rectangle image_frame) {
		final Float2 tmp = Geometry.newFloat2();
		tmp.set(elementAt);
		wrapping_frame.toRelative(tmp);
		image_frame.toAbsolute(tmp);
		return tmp;
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

	// private void fill(BufferedImage img, Patch18 p18,
	// double pixels_to_tile_meter, Rectangle wrapping_frame, int padding,
	// Mapping<Fabric, com.jfixby.cmns.api.color.Color> colors,
	// FabricsRelation relation, IsoTransform isometry,
	// TerrainType element, Mapping<FabricsRelation, Float2> centers,
	// Set<Patch18> unprocessed, AssetID tile_id, AssetID raster_id,
	// Scene2DPackage structures) {
	//
	// Rectangle image_frame = Geometry.newRectangle();
	// image_frame.setSize(wrapping_frame.getWidth() * pixels_to_tile_meter,
	// wrapping_frame.getHeight() * pixels_to_tile_meter);
	// image_frame.setPosition(padding, padding);
	//
	// {
	// SceneStructure structure = new SceneStructure();
	// structures.structures.addElement(structure);
	//
	// structure.structure_name = tile_id.toString();
	//
	// LayerElement raster_info = new LayerElement();
	// structure.root.children.addElement(raster_info);
	//
	// raster_info.is_hidden = false;
	// raster_info.name = raster_id.getLastStep();
	// raster_info.is_raster = true;
	//
	// raster_info.width = img.getWidth();
	// raster_info.height = img.getHeight();
	//
	// raster_info.position_x = 0;
	// raster_info.position_y = 0;
	//
	// Float3 origin3d = Geometry.newFloat3(0, 0, 0);
	// Float2 origin2d = Geometry.newFloat2();
	// isometry.project3Dto2D(origin3d, origin2d);
	// ajust(origin2d, wrapping_frame, image_frame);
	//
	// raster_info.origin_relative_x = origin2d.getX() / raster_info.width;
	// raster_info.origin_relative_y = origin2d.getY()
	// / raster_info.height;
	//
	// // raster_info.origin_relative_x = 0.5;
	// // raster_info.origin_relative_y = 0.5;
	//
	// raster_info.raster_id = raster_id.toString();
	// }
	//
	// Graphics2D g2 = img.createGraphics();
	//
	// double X = element.getXWidth().toDouble();
	// double Y = element.getYWidth().toDouble();
	// double Z = element.getHeight().toDouble();
	//
	// // g2.setColor(new Color(0, 0, 255, 8));
	// // g2.fillRect(0, 0, img.getWidth(), img.getHeight());
	// {
	// Fabric lower_fabric = relation.getLowerFabric();
	// com.jfixby.cmns.api.color.Color jcolor = colors.get(lower_fabric);
	//
	// {
	// EditableCollection<Float2> center_2d = getCenter(centers,
	// relation, X, Y, 0, isometry, JUtils.newList());
	// ;
	//
	// List<Float3> corners_3d = JUtils.newList();
	// corners_3d.add(Geometry.newFloat3(0, 0, 0));
	// corners_3d.add(Geometry.newFloat3(X, 0, 0));
	// corners_3d.add(Geometry.newFloat3(X, Y, 0));
	// corners_3d.add(Geometry.newFloat3(0, Y, 0));
	//
	// EditableCollection<Float2> corners_2d = Geometry.newFloat2(
	// JUtils.newList(), 4);
	// isometry.project3Dto2D(corners_3d, corners_2d);
	//
	// ajust(corners_2d, wrapping_frame, image_frame);
	// ajust(center_2d, wrapping_frame, image_frame);
	//
	// g2.setColor(new Color(jcolor.red(), jcolor.green(), jcolor
	// .blue(), jcolor.alpha()));
	//
	// fillTriangle(g2, corners_2d.getElementAt(0),
	// corners_2d.getElementAt(1), corners_2d.getElementAt(2));
	// fillTriangle(g2, corners_2d.getElementAt(2),
	// corners_2d.getElementAt(3), corners_2d.getElementAt(0));
	// g2.setColor(Color.black);
	// drawPoly(g2, center_2d);
	//
	// }
	// }
	// {
	// Fabric upper_fabric = relation.getUpperFabric();
	// com.jfixby.cmns.api.color.Color jcolor = colors.get(upper_fabric);
	// List<Float3> upper_ring_3d = JUtils.newList();
	// List<Float2> upper_ring_2d = JUtils.newList();
	// List<Float2> lower_ring_2d = JUtils.newList();
	//
	// EditableCollection<Float2> center_2d = getCenter(centers, relation,
	// X, Y, Z, isometry, upper_ring_3d);
	// EditableCollection<Float3> lower_ring_3d = Geometry.newFloat3(
	// JUtils.newList(), 4);
	// for (int i = 0; i < lower_ring_3d.size(); i++) {
	// lower_ring_3d.getElementAt(i).setXYZ(
	// upper_ring_3d.getElementAt(i));
	// lower_ring_3d.getElementAt(i).setZ(0);
	// }
	// {
	// List<Float3> corners_3d = JUtils.newList();
	// corners_3d.add(Geometry.newFloat3(0, 0, Z));
	// corners_3d.add(Geometry.newFloat3(X, 0, Z));
	// corners_3d.add(Geometry.newFloat3(X, Y, Z));
	// corners_3d.add(Geometry.newFloat3(0, Y, Z));
	//
	// EditableCollection<Float2> corners_2d = Geometry.newFloat2(
	//
	// JUtils.newList(), 4);
	//
	// Float3 H_3d = Geometry.newFloat3(0, 0, -Z);
	// Float2 H_2d = Geometry.newFloat2();
	//
	// isometry.project3Dto2D(H_3d, H_2d);
	// isometry.project3Dto2D(corners_3d, corners_2d);
	// isometry.project3Dto2D(upper_ring_3d, upper_ring_2d);
	// isometry.project3Dto2D(lower_ring_3d, lower_ring_2d);
	//
	// // wrapping_frame.toRelative(H_2d);
	// // image_frame.toAbsolute(H_2d);
	// //
	// // H_2d.add(-image_frame.getPosition().getX(), -image_frame
	// // .getPosition().getY());F
	//
	// H_2d.scaleXY(pixels_to_tile_meter);
	//
	// ajust(corners_2d, wrapping_frame, image_frame);
	// ajust(center_2d, wrapping_frame, image_frame);
	// ajust(upper_ring_2d, wrapping_frame, image_frame);
	// ajust(lower_ring_2d, wrapping_frame, image_frame);
	//
	// List<Float2> TL = Geometry.newFloat2List(3);
	// TL.getElementAt(0).set(corners_2d.getElementAt(0));
	// TL.getElementAt(1).set(upper_ring_2d.getElementAt(0));
	// TL.getElementAt(2).set(upper_ring_2d.getElementAt(1));
	//
	// List<Float2> TR = Geometry.newFloat2List(3);
	// TR.getElementAt(0).set(corners_2d.getElementAt(1));
	// TR.getElementAt(1).set(upper_ring_2d.getElementAt(1));
	// TR.getElementAt(2).set(upper_ring_2d.getElementAt(2));
	//
	// List<Float2> DR = Geometry.newFloat2List(3);
	// DR.getElementAt(0).set(corners_2d.getElementAt(2));
	// DR.getElementAt(1).set(upper_ring_2d.getElementAt(2));
	// DR.getElementAt(2).set(upper_ring_2d.getElementAt(3));
	//
	// List<Float2> DL = Geometry.newFloat2List(3);
	// DL.getElementAt(0).set(corners_2d.getElementAt(3));
	// DL.getElementAt(1).set(upper_ring_2d.getElementAt(3));
	// DL.getElementAt(2).set(upper_ring_2d.getElementAt(0));
	// float T = 1f;
	// // boolean top = true;
	// {
	// drawSurface(!true, g2, jcolor, TL, TR, DL, DR, T, H_2d,
	// p18, unprocessed, lower_ring_2d, corners_2d,
	// JUtils.newList(upper_ring_2d),
	// JUtils.newList(center_2d));
	// drawSurface(true, g2, jcolor, TL, TR, DL, DR, T, H_2d, p18,
	// unprocessed, lower_ring_2d, corners_2d,
	// JUtils.newList(upper_ring_2d),
	// JUtils.newList(center_2d));
	//
	// }
	// }
	// }
	//
	// g2.dispose();
	//
	// }

	// private void drawSurface(boolean top, Graphics2D g2,
	// com.jfixby.cmns.api.color.Color jcolor, List<Float2> TL,
	// List<Float2> TR, List<Float2> DL, List<Float2> DR, float T,
	// Float2 H_2d, Patch18 p18, Set<Patch18> unprocessed,
	// List<Float2> lower_ring_2d, EditableCollection<Float2> corners_2d,
	// List<Float2> upper_ring_2d, EditableCollection<Float2> center_2d) {
	//
	// g2.setColor(new Color(0, 0, 0, 0.3f));
	// // drawPoly(g2, lower_ring_2d);
	// g2.setColor(new Color(jcolor.red(), jcolor.green(), jcolor.blue(),
	// jcolor.alpha() * T));
	//
	// if (p18.isBlocked()) {
	// this.fillPoly(g2, corners_2d, H_2d, top);
	// unprocessed.remove(p18);
	// }
	// if (p18.isFree()) {
	// // this.fillPoly(g2, corners_2d);
	// unprocessed.remove(p18);
	// }
	//
	// if (p18.isErr()) {
	// g2.setColor(new Color(1, 0, 0, T));
	//
	// this.fillPoly(g2, corners_2d, H_2d, top);
	// unprocessed.remove(p18);
	// }
	// if (p18.isIrrelevant()) {
	// g2.setColor(new Color(0.5f, 0.5f, 0.5f, T));
	//
	// this.fillPoly(g2, corners_2d, H_2d, top);
	// unprocessed.remove(p18);
	// }
	//
	// if (p18.isLookingDownRight()) {
	// this.fillPoly(g2, TL, H_2d, top);
	// unprocessed.remove(p18);
	// }
	// if (p18.isLookingDownLeft()) {
	// this.fillPoly(g2, TR, H_2d, top);
	// unprocessed.remove(p18);
	// }
	// if (p18.isLookingUpRight()) {
	// this.fillPoly(g2, DL, H_2d, top);
	// unprocessed.remove(p18);
	// }
	// if (p18.isLookingUpLeft()) {
	// this.fillPoly(g2, DR, H_2d, top);
	// unprocessed.remove(p18);
	// }
	// if (p18.isBottomLeftCorner()) {
	// this.fillPoly(g2, DL, H_2d, top);
	// this.fillPoly(g2, TL, H_2d, top);
	// this.fillPoly(g2, DR, H_2d, top);
	// // this.fillPoly(g2, TR);
	// this.fillPoly(g2, upper_ring_2d, H_2d, top);
	// unprocessed.remove(p18);
	//
	// }
	// if (p18.isBottomRightCorner()) {
	// this.fillPoly(g2, DL, H_2d, top);
	// // this.fillPoly(g2, TL);
	// this.fillPoly(g2, DR, H_2d, top);
	// this.fillPoly(g2, TR, H_2d, top);
	// this.fillPoly(g2, upper_ring_2d, H_2d, top);
	// unprocessed.remove(p18);
	// }
	// if (p18.isTopRightCorner()) {
	// // this.fillPoly(g2, DL);
	// this.fillPoly(g2, TL, H_2d, top);
	// this.fillPoly(g2, DR, H_2d, top);
	// this.fillPoly(g2, TR, H_2d, top);
	// this.fillPoly(g2, upper_ring_2d, H_2d, top);
	// unprocessed.remove(p18);
	// }
	// if (p18.isTopLeftCorner()) {
	// this.fillPoly(g2, DL, H_2d, top);
	// this.fillPoly(g2, TL, H_2d, top);
	// // this.fillPoly(g2, DR);
	// this.fillPoly(g2, TR, H_2d, top);
	// this.fillPoly(g2, upper_ring_2d, H_2d, top);
	// unprocessed.remove(p18);
	// }
	// if (p18.isLeftBridge()) {
	// // this.fillPoly(g2, DL);
	// this.fillPoly(g2, TL, H_2d, top);
	// this.fillPoly(g2, DR, H_2d, top);
	// // this.fillPoly(g2, TR);
	// this.fillPoly(g2, upper_ring_2d, H_2d, top);
	// unprocessed.remove(p18);
	// }
	// if (p18.isRightBridge()) {
	// this.fillPoly(g2, DL, H_2d, top);
	// // this.fillPoly(g2, TL);
	// // this.fillPoly(g2, DR);
	// this.fillPoly(g2, TR, H_2d, top);
	// this.fillPoly(g2, upper_ring_2d, H_2d, top);
	// unprocessed.remove(p18);
	// }
	//
	// if (p18.isLookingUp()) {
	// this.fillPoly(g2, DL, H_2d, top);
	// // this.fillPoly(g2, TL);
	// this.fillPoly(g2, DR, H_2d, top);
	// // this.fillPoly(g2, TR);
	// // this.fillPoly(g2, upper_ring_2d);
	// upper_ring_2d.removeElementAt(1);
	// this.fillPoly(g2, upper_ring_2d);
	//
	// unprocessed.remove(p18);
	// }
	// if (p18.isLookingDown()) {
	// // this.fillPoly(g2, DL);
	// this.fillPoly(g2, TL, H_2d, top);
	// // this.fillPoly(g2, DR);
	// this.fillPoly(g2, TR, H_2d, top);
	// upper_ring_2d.removeElementAt(3);
	// this.fillPoly(g2, upper_ring_2d, H_2d, top);
	// unprocessed.remove(p18);
	// }
	// if (p18.isLookingRight()) {
	// this.fillPoly(g2, DL, H_2d, top);
	// this.fillPoly(g2, TL, H_2d, top);
	// // this.fillPoly(g2, DR);
	// // this.fillPoly(g2, TR);
	// upper_ring_2d.removeElementAt(2);
	// this.fillPoly(g2, upper_ring_2d, H_2d, top);
	// unprocessed.remove(p18);
	// }
	// if (p18.isLookingLeft()) {
	// // this.fillPoly(g2, DL);
	// // this.fillPoly(g2, TL);
	// this.fillPoly(g2, DR, H_2d, top);
	// this.fillPoly(g2, TR, H_2d, top);
	// upper_ring_2d.removeElementAt(0);
	// this.fillPoly(g2, upper_ring_2d, H_2d, top);
	// unprocessed.remove(p18);
	// }
	//
	// g2.setColor(new Color(0, 0, 0, 0.3f));
	// drawPoly(g2, center_2d);
	// // drawPoly(g2, upper_ring_2d);
	//
	// }

	// private void fillPoly(Graphics2D g2, Collection<Float2> upper_poly,
	// Float2 Z, boolean t) {
	// Color color = g2.getColor();
	// Color darker_color = color.darker();
	// for (int i = 0; i < upper_poly.size(); i++) {
	// List<Float2> tmp = Geometry.newFloat2List(4);
	// int p = i + 1;
	// if (p >= upper_poly.size()) {
	// p = 0;
	// }
	// Float2 I = upper_poly.getElementAt(i);
	// Float2 P = upper_poly.getElementAt(p);
	// Float2 lI = Geometry.newFloat2(upper_poly.getElementAt(i));
	// Float2 lP = Geometry.newFloat2(upper_poly.getElementAt(p));
	// // L.d("Z", Z);
	// // L.d("I", I);
	//
	// lI.add(Z);
	// lP.add(Z);
	//
	// tmp.getElementAt(0).set(I);
	// tmp.getElementAt(1).set(P);
	// tmp.getElementAt(2).set(lP);
	// tmp.getElementAt(3).set(lI);
	//
	// g2.setColor(darker_color);
	// if (!t) {
	// this.fillPoly(g2, tmp);
	// }
	//
	// }
	// g2.setColor(color);
	// if (t) {
	// this.fillPoly(g2, upper_poly);
	// }
	// }

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

	// private void ajust(Float2 point, Rectangle wrapping_frame,
	// Rectangle image_frame) {
	// wrapping_frame.toRelative(point);
	// image_frame.toAbsolute(point);
	// }

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

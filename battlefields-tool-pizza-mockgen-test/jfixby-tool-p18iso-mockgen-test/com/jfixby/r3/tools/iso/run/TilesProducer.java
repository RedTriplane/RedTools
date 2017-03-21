
package com.jfixby.r3.tools.iso.run;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;

import com.github.wrebecca.bleed.RebeccaTextureBleeder;
import com.jfixby.examples.wdgs.WDGS_P18Terrain_Palette;
import com.jfixby.r3.ext.api.patch18.P18;
import com.jfixby.r3.ext.p18t.red.RedP18Terrain;
import com.jfixby.r3.ext.red.terrain.RedTerrain;
import com.jfixby.r3.tools.api.iso.IsoMockPaletteGenerator;
import com.jfixby.r3.tools.iso.red.RedIsoMockPaletteGenerator2;
import com.jfixby.scarabei.adopted.gdx.GdxSimpleTriangulator;
import com.jfixby.scarabei.adopted.gdx.json.GdxJson;
import com.jfixby.scarabei.api.collections.Collection;
import com.jfixby.scarabei.api.collections.Collections;
import com.jfixby.scarabei.api.collections.EditableCollection;
import com.jfixby.scarabei.api.collections.List;
import com.jfixby.scarabei.api.color.Color;
import com.jfixby.scarabei.api.color.Colors;
import com.jfixby.scarabei.api.desktop.ImageAWT;
import com.jfixby.scarabei.api.desktop.ScarabeiDesktop;
import com.jfixby.scarabei.api.file.File;
import com.jfixby.scarabei.api.file.FilesList;
import com.jfixby.scarabei.api.file.LocalFileSystem;
import com.jfixby.scarabei.api.floatn.Float2;
import com.jfixby.scarabei.api.floatn.Float3;
import com.jfixby.scarabei.api.floatn.ReadOnlyFloat2;
import com.jfixby.scarabei.api.geometry.ClosedPolygonalChain;
import com.jfixby.scarabei.api.geometry.Geometry;
import com.jfixby.scarabei.api.geometry.PolyTriangulation;
import com.jfixby.scarabei.api.geometry.Rectangle;
import com.jfixby.scarabei.api.geometry.Triangle;
import com.jfixby.scarabei.api.geometry.Vertex;
import com.jfixby.scarabei.api.image.ArrayColorMap;
import com.jfixby.scarabei.api.json.Json;
import com.jfixby.scarabei.api.log.L;
import com.jfixby.scarabei.api.math.FloatMath;
import com.jfixby.scarabei.api.math.SimpleTriangulator;
import com.jfixby.scarabei.red.desktop.image.RedImageAWT;
import com.jfixby.texture.slicer.api.TextureSlicer;
import com.jfixby.texture.slicer.red.RedTextureSlicer;
import com.jfixby.tools.bleed.api.TextureBleed;
import com.jfixby.tools.gdx.texturepacker.GdxTexturePacker;
import com.jfixby.tools.gdx.texturepacker.api.TexturePacker;
import com.jfixby.util.iso.api.IsoTransform;
import com.jfixby.util.iso.api.Isometry;
import com.jfixby.util.iso.red.RedIsometry;
import com.jfixby.util.p18t.api.P18Terrain;
import com.jfixby.util.p18t.api.P18TerrainPalette;
import com.jfixby.util.patch18.red.RedP18;
import com.jfixby.util.terain.test.api.palette.Terrain;
import com.jfixby.utl.pizza.api.Pizza;
import com.jfixby.utl.pizza.api.PizzaPalette;
import com.jfixby.utl.pizza.api.PizzaPaletteFactory;
import com.jfixby.utl.pizza.api.PizzaPaletteSpecs;
import com.jfixby.utl.pizza.red.RedPizza;

public class TilesProducer {

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

		final File input = LocalFileSystem.ApplicationHome().child("tiles-input");
		final File output = LocalFileSystem.ApplicationHome().child("tiles-output");

		final FilesList tiles = input.listDirectChildren(file -> file.extensionIs("png"));
		tiles.print("tiles");

		final P18TerrainPalette p18_terrain_palette = WDGS_P18Terrain_Palette.P18_TERRAIN_PALETTE;

		final PizzaPaletteFactory palette_factory = Pizza.invoke().getPizzaPaletteFactory();

		final PizzaPaletteSpecs specs = palette_factory.newPizzaPaletteSpecs();
		specs.setP18TerrainPalette(p18_terrain_palette);
		final IsoTransform fallout_iso = Isometry.getFallout(1);
		specs.setIsoTransform(fallout_iso);

		final PizzaPalette pizza_palette = palette_factory.newPizzaPalette(specs);

		for (final File image_file : tiles) {
			final ArrayColorMap colors = ImageAWT.readAWTColorMap(image_file);
			final Rectangle rect = Geometry.newRectangle(colors.getWidth(), colors.getHeight());
			final IsoTransform isometry = pizza_palette.getIsoTransform();
			{
				final EditableCollection<Float3> ground = Collections.newList();
				Geometry.fillUpFloat3(ground, 4);
				final double Wx = rect.getWidth();
				final double Wy = rect.getHeight();
				ground.getElementAt(0).setXYZ(0, 0, 0);
				ground.getElementAt(1).setXYZ(Wx, 0, 0);
				ground.getElementAt(2).setXYZ(Wx, Wy, 0);
				ground.getElementAt(3).setXYZ(0, Wy, 0);

				final int padding = 64;

				final Rectangle wrapping_frame = Geometry.newRectangle();
				final Rectangle image_frame = Geometry.newRectangle();
				final EditableCollection<Float2> tile_shape = Collections.newList();
				isometry.project3Dto2D(ground, tile_shape);
				Geometry.setupWrapingFrame(tile_shape, wrapping_frame);

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
						g2.setColor(java.awt.Color.blue);
						g2.fillRect(0, 0, img_width, img_height);

					}
					{

						fillPoly(g2, ground, wrapping_frame, image_frame, isometry, Colors.BLACK().customize().setAlpha(0.3f));
					}

					{

						fillImage(g2, colors, wrapping_frame, image_frame, isometry);
					}
					g2.dispose();
					ImageAWT.writeToFile(buffer, output.child(image_file.nameWithoutExtension() + ".png"), "png");
				}
			}
		}
	}

	static private void fillPoly (final Graphics g2, final EditableCollection<Float3> shape_3d, final Rectangle wrapping_frame,
		final Rectangle image_frame, final IsoTransform isometry, final Color jcolor) {
		final EditableCollection<Float2> shape_2d = Collections.newList();
		isometry.project3Dto2D(shape_3d, shape_2d);
		isometry.project2DtoPixels(shape_2d);
		fillPoly(g2, shape_2d, wrapping_frame, image_frame, jcolor);
	}

	static private void fillImage (final Graphics g2, final ArrayColorMap image, final Rectangle wrapping_frame,
		final Rectangle image_frame, final IsoTransform isometry) {

		final List<Float2> shape_2d = Collections.newList();
		final List<Float3> shape_3d = Collections.newList();
		for (int i = 0; i < 3; i++) {
			final Float3 f3 = Geometry.newFloat3();
			shape_3d.add(f3);
		}
		for (int x = 0; x < image.getWidth(); x++) {
			for (int y = 0; y < image.getHeight(); y++) {
				L.d("y", y);
// final EditableCollection<Float2> shape_2d = Collections.newList();
// isometry.project3Dto2D(shape_3d, shape_2d);
// isometry.project2DtoPixels(shape_2d);
// fillPoly(g2, shape_2d, wrapping_frame, image_frame, jcolor);
				shape_3d.getElementAt(0).setXYZ(x + 0, y + 0, 0);
				shape_3d.getElementAt(1).setXYZ(x + 1, y + 0, 0);
				shape_3d.getElementAt(2).setXYZ(x + 1, y + 1, 0);
// shape_3d.getElementAt(3).setXYZ(x + 0, y + 1, 0);

// isometry.unprojectPixelsTo2D(f2);
// isometry.unproject2Dto3D(f2, 0, f3);
				final Color jcolor = image.valueAt((float)shape_3d.getElementAt(0).getX(), (float)shape_3d.getElementAt(0).getY());
// setColor(g2, color);

				isometry.project3Dto2D(shape_3d, shape_2d);
				isometry.project2DtoPixels(shape_2d);
				fillPoly(g2, shape_2d, wrapping_frame, image_frame, jcolor);
			}
		}

	}

	static private void fillPoly (final Graphics g2, final Collection<EditableCollection<Float3>> shapes_3d,
		final Rectangle wrapping_frame, final Rectangle image_frame, final IsoTransform isometry, final Color jcolor) {
		for (int i = 0; i < shapes_3d.size(); i++) {
			final EditableCollection<Float3> shape_3d = shapes_3d.getElementAt(i);
			fillPoly(g2, shape_3d, wrapping_frame, image_frame, isometry, jcolor);
		}
	}

	static private Float2 ajust (final ReadOnlyFloat2 elementAt, final Rectangle wrapping_frame, final Rectangle image_frame) {
		final Float2 tmp = Geometry.newFloat2();
		tmp.set(elementAt);
		wrapping_frame.toRelative(tmp);
		image_frame.toAbsolute(tmp);
		return tmp;
	}

	static private void fillPoly (final Graphics g2, final EditableCollection<Float2> shape_2d, final Rectangle wrapping_frame,
		final Rectangle image_frame, final Color jcolor) {
		final ClosedPolygonalChain chain = Geometry.newClosedPolygonalChain(shape_2d);
		final PolyTriangulation triangles = chain.getTriangulation();
		final int N = triangles.size();
		for (int i = 0; i < N; i++) {
			final Triangle triangle = triangles.getTriangle(i);
			final Vertex a = triangle.A();
			final Vertex b = triangle.B();
			final Vertex c = triangle.C();
			final Float2 A = ajust(a, wrapping_frame, image_frame);
			final Float2 B = ajust(b, wrapping_frame, image_frame);
			final Float2 C = ajust(c, wrapping_frame, image_frame);
			final int x1 = (int)FloatMath.round(A.getX());
			final int y1 = (int)FloatMath.round(A.getY());
			final int x2 = (int)FloatMath.round(B.getX());
			final int y2 = (int)FloatMath.round(B.getY());
			final int x3 = (int)FloatMath.round(C.getX());
			final int y3 = (int)FloatMath.round(C.getY());

			final int xpoints[] = {x1, x2, x3};
			final int ypoints[] = {y1, y2, y3};
			final int npoints = 3;
			setColor(g2, jcolor);
			g2.fillPolygon(xpoints, ypoints, npoints);

			setColor(g2, Colors.DARK_GRAY().customize().setAlpha(0.5f));
			// g2.drawLine(x1, y1, x2, y2);
			// g2.drawLine(x3, y3, x2, y2);
			// g2.drawLine(x3, y3, x1, y1);

		}

	}

	static private void setColor (final Graphics g2, final Color jcolor) {
		final java.awt.Color awt_color = new java.awt.Color(jcolor.red(), jcolor.green(), jcolor.blue(), jcolor.alpha());
		g2.setColor(awt_color);
	}

}

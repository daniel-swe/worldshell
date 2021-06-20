package net.snakefangox.worldshell.collision;


import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.snakefangox.worldshell.entity.WorldShellEntity;
import net.snakefangox.worldshell.math.Matrix3d;
import net.snakefangox.worldshell.math.Quaternion;
import net.snakefangox.worldshell.math.Vector3d;
import net.snakefangox.worldshell.storage.LocalSpace;
import net.snakefangox.worldshell.storage.Microcosm;

import java.util.Arrays;
import java.util.Optional;

/**
 * A custom {@link Box} style implementation that takes a worldshell and handles complex collision.
 */
public class ShellCollisionHull implements LocalSpace {

	private static final double SMOL = 1.0E-7D;
	private final WorldShellEntity entity;
	private HullBoxDelegate dBox;
	private final Vector3d localVector = new Vector3d(), localVector2 = new Vector3d(), localVector3 = new Vector3d();
	private final Matrix3d localMatrix = new Matrix3d();
	private final BlockPos.Mutable localBp = new BlockPos.Mutable();

	public ShellCollisionHull(WorldShellEntity entity) {
		this.entity = entity;
		dBox = new HullBoxDelegate(new Box(BlockPos.ORIGIN), this);
	}

	public void calculateCrudeBounds() {
		EntityBounds bounds = entity.getDimensions();
		double len = bounds.length / 2.0;
		double height = bounds.height / 2.0;
		double width = bounds.width / 2.0;
		localVector.set(-len, -height, -width);
		localVector2.set(len, height, width);
		localVector3.set(entity.getX(), entity.getY() + height, entity.getZ());
		dBox = new HullBoxDelegate(transformBox(localVector, localVector2, getRotation(), localVector3), this);
	}

	public void onWorldshellUpdate() {
	}

	public boolean intersects(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		return true;
	}

	public double calculateMaxDistance(Direction.Axis axis, Box box, double maxDist) {
		double absMax = Math.abs(maxDist);
		if (absMax < SMOL) return 0;
		return maxDist;
	}

	public boolean contains(double x, double y, double z) {
		localVector.set(x, y, z);
		toLocal(localVector);
		localBp.set(localVector.x, localVector.y, localVector.z);
		VoxelShape shape = entity.getMicrocosm().getBlockState(localBp).getCollisionShape(entity.getMicrocosm(), localBp);
		if (shape.isEmpty()) return false;
		return shape.getBoundingBox().contains(localVector.x, localVector.y, localVector.z);
	}

	public Optional<Vec3d> raycast(Vec3d min, Vec3d max) {
		Vec3d nMin = toLocal(min);
		Vec3d nMax = toLocal(max);
		RaycastContext ctx = new RaycastContext(nMin, nMax, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity);
		BlockHitResult hit = entity.getMicrocosm().raycast(ctx);
		return hit.getType() == HitResult.Type.MISS ? Optional.empty() : Optional.of(entity.toGlobal(hit.getPos()));
	}

	private void streamVoxelShapes(Vector3d min, Vector3d max) {
		Microcosm microcosm = entity.getMicrocosm();

		for (int x = (int) min.x; x < Math.ceil(max.x) + 1; ++x) {
			for (int y = (int) min.y; y < Math.ceil(max.y) + 1; ++y) {
				for (int z = (int) min.z; z < Math.ceil(max.z) + 1; ++z) {
					localBp.set(x, y, z);

				}
			}
		}
	}

	public Box transformBox(Vector3d min, Vector3d max, Quaternion rot, Vector3d trans) {
		rot.toRotationMatrix(localMatrix);
		double[] oMin = new double[]{min.x, min.y, min.z};
		double[] oMax = new double[]{max.x, max.y, max.z};
		double[] nMin = new double[]{trans.x, trans.y, trans.z};
		double[] nMax = Arrays.copyOf(nMin, nMin.length);

		for (int i = 0; i < 3; ++i) {
			for (int j = 0; j < 3; ++j) {
				double a = localMatrix.get(i, j) * oMin[j];
				double b = localMatrix.get(i, j) * oMax[j];

				nMin[i] += Math.min(a, b);
				nMax[i] += Math.max(a, b);
			}
		}

		return new Box(nMin[0], nMin[1], nMin[2], nMax[0], nMax[1], nMax[2]);
	}

	public VoxelShape toVoxelShape() {
		return new HullVoxelDelegate(this);
	}

	public double getMin(Direction.Axis axis) {
		return dBox.getMin(axis);
	}

	public double getMax(Direction.Axis axis) {
		return dBox.getMax(axis);
	}

	public Box getDelegateBox() {
		return dBox;
	}

	@Override
	public double getLocalX() {
		return entity.getLocalX();
	}

	@Override
	public double getLocalY() {
		return entity.getLocalY();
	}

	@Override
	public double getLocalZ() {
		return entity.getLocalZ();
	}

	@Override
	public Quaternion getRotation() {
		return entity.getRotation();
	}

	@Override
	public Quaternion getInverseRotation() {
		return entity.getInverseRotation();
	}
}
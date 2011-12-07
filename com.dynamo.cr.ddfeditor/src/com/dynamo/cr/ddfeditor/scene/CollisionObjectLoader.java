package com.dynamo.cr.ddfeditor.scene;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import javax.vecmath.Quat4d;
import javax.vecmath.Vector4d;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.dynamo.cr.sceneed.core.ILoaderContext;
import com.dynamo.cr.sceneed.core.INodeLoader;
import com.dynamo.cr.sceneed.core.Node;
import com.dynamo.cr.sceneed.core.util.LoaderUtil;
import com.dynamo.physics.proto.Physics.CollisionObjectDesc;
import com.dynamo.physics.proto.Physics.CollisionShape;
import com.dynamo.physics.proto.Physics.CollisionShape.Shape;
import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;

public class CollisionObjectLoader implements INodeLoader<CollisionObjectNode> {

    @Override
    public CollisionObjectNode load(ILoaderContext context, InputStream contents)
            throws IOException, CoreException {
        InputStreamReader reader = new InputStreamReader(contents);
        CollisionObjectDesc.Builder builder = CollisionObjectDesc.newBuilder();
        TextFormat.merge(reader, builder);
        CollisionObjectDesc desc = builder.build();
        CollisionObjectNode collisionObject = new CollisionObjectNode();
        collisionObject.setCollisionShape(desc.getCollisionShape());
        collisionObject.setType(desc.getType());
        collisionObject.setMass(desc.getMass());
        collisionObject.setFriction(desc.getFriction());
        collisionObject.setRestitution(desc.getRestitution());
        collisionObject.setGroup(desc.getGroup());
        StringBuffer mask = new StringBuffer();
        int n = desc.getMaskCount();
        for (int i = 0; i < n; ++i) {
            mask.append(desc.getMask(i));
            if (i < n - 1) {
                mask.append(", ");
            }
        }
        collisionObject.setMask(mask.toString());

        CollisionShape collisionShape = desc.getEmbeddedCollisionShape();
        List<Float> dataList = collisionShape.getDataList();
        float[] data = new float[dataList.size()];
        int i = 0;
        for (float f : dataList) {
            data[i++] = f;
        }

        for (Shape shape : collisionShape.getShapesList()) {
            Vector4d pos = LoaderUtil.toVector4(shape.getPosition());
            Quat4d rot = LoaderUtil.toQuat4(shape.getRotation());

            int index = shape.getIndex();

            CollisionShapeNode shapeNode = null;
            switch (shape.getShapeType()) {
            case TYPE_SPHERE:
                shapeNode = new SphereCollisionShapeNode(pos, rot, data, index);
                break;
            case TYPE_BOX:
                shapeNode = new BoxCollisionShapeNode(pos, rot, data, index);
                break;
            case TYPE_CAPSULE:
                shapeNode = new CapsuleCollisionShapeNode(pos, rot, data, index);
                break;

            default:
                throw new RuntimeException("Not implemented: " + shape.getShapeType());
            }

            collisionObject.addShape(shapeNode);
        }

        Node collisionShapeNode = context.loadNode(collisionObject.getCollisionShape());
        collisionObject.setCollisionShapeNode(collisionShapeNode);

        return collisionObject;
    }

    @Override
    public Message buildMessage(ILoaderContext context,
            CollisionObjectNode node, IProgressMonitor monitor)
                    throws IOException, CoreException {
        CollisionObjectDesc.Builder builder = CollisionObjectDesc.newBuilder();
        CollisionObjectNode collisionObject = node;
        builder.setCollisionShape(collisionObject.getCollisionShape());
        builder.setType(collisionObject.getType());
        builder.setMass(collisionObject.getMass());
        builder.setFriction(collisionObject.getFriction());
        builder.setRestitution(collisionObject.getRestitution());
        builder.setGroup(collisionObject.getGroup());
        String[] masks = collisionObject.getMask().split("[ ,]+");
        for (String mask : masks) {
            builder.addMask(mask);
        }

        CollisionShape.Builder collisionShapeBuilder = CollisionShape.newBuilder();
        for (Node n : node.getChildren()) {
            CollisionShapeNode shapeNode = (CollisionShapeNode) n;
            shapeNode.build(collisionShapeBuilder);
        }

        if (collisionShapeBuilder.getShapesCount() > 0) {
            builder.setEmbeddedCollisionShape(collisionShapeBuilder);
        }

        return builder.build();
    }

}

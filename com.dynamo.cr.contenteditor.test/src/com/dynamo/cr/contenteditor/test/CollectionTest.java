package com.dynamo.cr.contenteditor.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Before;
import org.junit.Test;

import com.dynamo.cr.contenteditor.resource.CameraLoader;
import com.dynamo.cr.contenteditor.resource.CollisionLoader;
import com.dynamo.cr.contenteditor.resource.ConvexShapeLoader;
import com.dynamo.cr.contenteditor.resource.LightLoader;
import com.dynamo.cr.contenteditor.resource.SpriteLoader;
import com.dynamo.cr.contenteditor.resource.TextureLoader;
import com.dynamo.cr.contenteditor.scene.AbstractNodeLoaderFactory;
import com.dynamo.cr.contenteditor.scene.CollectionNode;
import com.dynamo.cr.contenteditor.scene.CollectionNodeLoader;
import com.dynamo.cr.contenteditor.scene.ComponentNode;
import com.dynamo.cr.contenteditor.scene.InstanceNode;
import com.dynamo.cr.contenteditor.scene.MeshNode;
import com.dynamo.cr.contenteditor.scene.MeshNodeLoader;
import com.dynamo.cr.contenteditor.scene.ModelNodeLoader;
import com.dynamo.cr.contenteditor.scene.Node;
import com.dynamo.cr.contenteditor.scene.PrototypeNode;
import com.dynamo.cr.contenteditor.scene.PrototypeNodeLoader;
import com.dynamo.cr.contenteditor.scene.Scene;

public class CollectionTest {

    private AbstractNodeLoaderFactory factory;
    private FileResourceLoaderFactory resourceFactory;
    private Scene scene;

    @Before
    public void setup() {
        String root = "test";
        resourceFactory = new FileResourceLoaderFactory(root);
        resourceFactory.addLoader(new TextureLoader(), "png");
        resourceFactory.addLoader(new CameraLoader(), "camera");
        resourceFactory.addLoader(new LightLoader(), "light");
        resourceFactory.addLoader(new SpriteLoader(), "sprite");
        resourceFactory.addLoader(new CollisionLoader(), "collisionobject");
        resourceFactory.addLoader(new ConvexShapeLoader(), "convexshape");
        factory = new FileNodeLoaderFactory(root, resourceFactory);
        factory.addLoader(new CollectionNodeLoader(), "collection");
        factory.addLoader(new PrototypeNodeLoader(), "go");
        factory.addLoader(new ModelNodeLoader(), "model");
        factory.addLoader(new MeshNodeLoader(), "dae");
        scene = new Scene();
    }

    InstanceNode getInstanceNode(Node[] nodes, String resource) {
        for (Node n : nodes) {
            if (n instanceof InstanceNode) {
                InstanceNode in = (InstanceNode) n;
                if (in.getName().equals(resource)) {
                    return in;
                }
            }
        }
        assertTrue(false);
        return null;
    }

    @Test
    public void testCollectionLoader() throws Exception {
        String name = "test.collection";
        Node node = factory.load(new NullProgressMonitor(), scene, name);
        assertThat(node, instanceOf(CollectionNode.class));
        assertThat(node.getChilden().length, is(3));

        CollectionNode coll = (CollectionNode) node;
        Node[] children = coll.getChilden();

        InstanceNode attacker = getInstanceNode(children, "attacker");
        InstanceNode attacker_child1 = getInstanceNode(attacker.getChilden(), "attacker_child1");
        InstanceNode attacker_child2 = getInstanceNode(attacker.getChilden(), "attacker_child2");
        InstanceNode target = getInstanceNode(children, "target");

        assertThat(attacker.getPrototype(), is("attacker.go"));
        assertThat(attacker_child1.getPrototype(), is("attacker_child.go"));
        assertThat(attacker_child2.getPrototype(), is("attacker_child.go"));
        assertThat(target.getPrototype(), is("target.go"));

        assertThat(attacker.getParent(), equalTo((Node) coll));

        // TODO: MAKE WORK
        assertThat(attacker_child1.getParent(), equalTo((Node) attacker));
        assertThat(attacker_child2.getParent(), equalTo((Node) attacker));
        assertThat(target.getParent(), equalTo((Node) coll));

        assertThat(node.contains(attacker), is(true));
        assertThat(node.contains(target), is(true));

        assertThat(attacker.getChilden().length, is(3));
        PrototypeNode attacker_prototype = (PrototypeNode) attacker.getChilden()[0];
        assertThat(attacker_prototype.getChilden().length, is(3));

        assertThat(target.getChilden().length, is(1));
        PrototypeNode target_prototype = (PrototypeNode) target.getChilden()[0];
        assertThat(target_prototype.getChilden().length, is(3));
    }

    ComponentNode getComponentNode(Node[] nodes, String resource) {
        for (Node n : nodes) {
            ComponentNode cn = (ComponentNode) n;
            if (cn.getResource().equals(resource)) {
                return cn;
            }
        }
        assertTrue(false);
        return null;
    }

    @Test
    public void testPrototypeLoader() throws Exception {
        String name = "attacker.go";
        Node node = factory.load(new NullProgressMonitor(), scene, name);
        assertThat(node, instanceOf(PrototypeNode.class));
        assertThat(node.getChilden().length, is(3));

        Node[] children = node.getChilden();
        ComponentNode n1 = getComponentNode(children, "attacker.script");
        ComponentNode n2 = getComponentNode(children, "attacker.collisionobject");
        ComponentNode n3 = getComponentNode(children, "box.model");

        assertThat(n1.getParent(), equalTo((Node) node));
        assertThat(n2.getParent(), equalTo((Node) node));
        assertThat(n3.getParent(), equalTo((Node) node));

        assertThat(node.contains(n1), is(true));
        assertThat(node.contains(n2), is(true));
        assertThat(node.contains(n3), is(true));

        assertThat(n3.getChilden().length, is(1));
        Node mn = n3.getChilden()[0];
        assertThat(mn, instanceOf(MeshNode.class));
    }

    private void testNodeFlags(Node[] nodes, int flags) throws Exception {
        for (Node node : nodes) {
            assertThat((node.getFlags() & flags), is(0));
            testNodeFlags(node.getChilden(), flags);
        }
    }

    private void testNodeFlagsExcludeInstance(Node[] nodes, int flags) throws Exception {
        for (Node node : nodes) {
            if (!(node instanceof InstanceNode))
                assertThat((node.getFlags() & flags), is(0));
            testNodeFlagsExcludeInstance(node.getChilden(), flags);
        }
    }

    @Test
    public void testNodeFlags() throws Exception {
        String name = "test.collection";
        Node node = factory.load(new NullProgressMonitor(), scene, name);
        assertThat(node, instanceOf(CollectionNode.class));
        assertThat(node.getChilden().length, is(3));
        assertThat((node.getFlags() & Node.FLAG_CAN_HAVE_CHILDREN), not(0));
        int flags = Node.FLAG_LABEL_EDITABLE
            & Node.FLAG_SELECTABLE
            & Node.FLAG_TRANSFORMABLE;
        for (Node child : node.getChilden()) {
            assertThat((child.getFlags() & flags), is(flags));
            if (child instanceof InstanceNode)
                testNodeFlagsExcludeInstance(child.getChilden(), flags);
            else
                testNodeFlags(child.getChilden(), flags);
        }
    }
}


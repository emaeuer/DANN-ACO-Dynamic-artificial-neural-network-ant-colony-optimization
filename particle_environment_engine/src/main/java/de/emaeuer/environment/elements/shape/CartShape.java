package de.emaeuer.environment.elements.shape;

import de.emaeuer.environment.balance.onedim.elements.Cart;

import java.util.List;
import java.util.stream.IntStream;

public class CartShape implements Shape<Cart> {

    @Override
    public List<ShapeEntity> getShapesForElement(Cart element) {
        ShapeEntity cartShape = getShapeForCart(element);

        ShapeEntity joint = getShapeForJoint(element);
        ShapeEntity poleOneShape = getShapeForPole(element, element.getPoleData().poleOneLength(), element.getPoleOneAngle());

        if (element.getPoleData().twoPoles()) {
            ShapeEntity poleTwoShape = getShapeForPole(element, element.getPoleData().poleTwoLength(), element.getPoleTwoAngle());
            return List.of(cartShape, poleOneShape, poleTwoShape, joint);
        } else {
            return List.of(cartShape, poleOneShape, joint);
        }

    }

    private ShapeEntity getShapeForCart(Cart element) {
        double width = element.getSize().getX();
        double height = element.getSize().getY();
        double x = element.getPosition().getX() - width / 2;
        double y = element.getPosition().getY() - height / 2;

        double[] xCoords = new double[] {x, x , x + width, x + width};
        double[] yCoords = new double[] {y, y + height, y + height, y};

        return new ShapeEntity(BasicShape.SQUARE, xCoords, yCoords);
    }

    private ShapeEntity getShapeForJoint(Cart element) {
        double x = element.getPosition().getX();
        double y = element.getPosition().getY() - (element.getSize().getY() / 2);

        return new ShapeEntity(BasicShape.CIRCLE, new double[] {x - 5, 10}, new double[] {y - 5, 10});
    }

    private ShapeEntity getShapeForPole(Cart element, double poleLength, double poleAngle) {
        double length = poleLength * 1000;
        double width = 10;
        double x = element.getPosition().getX();
        double y = element.getPosition().getY() - (element.getSize().getY() / 2);

        double rotationSin = Math.sin(poleAngle);
        double rotationCos = Math.cos(poleAngle);

        double[] xPrototype = new double[] {0.5, 0.5, -0.5, -0.5};
        double[] yPrototype = new double[] {0, -1, -1, 0};

        double[] xCoords = IntStream.range(0, xPrototype.length)
                .mapToDouble(i -> x + xPrototype[i] * width * rotationCos - yPrototype[i] * length * rotationSin)
                .toArray();

        double[] yCoords = IntStream.range(0, yPrototype.length)
                .mapToDouble(i -> y + xPrototype[i] * width * rotationSin + yPrototype[i] * length * rotationCos)
                .toArray();

        return new ShapeEntity(BasicShape.SQUARE, xCoords, yCoords);
    }

}

package de.emaeuer.environment.elements.shape;

import de.emaeuer.environment.elements.AbstractElement;

import java.util.List;

public class ImageShape implements Shape<AbstractElement> {

    private final String imageFile;

    public ImageShape(String imageFile) {
        this.imageFile = imageFile;
    }

    @Override
    public List<ShapeEntity> getShapesForElement(AbstractElement element) {
        return element.getShapesOfElement();
    }

    public String getImageFile() {
        return imageFile;
    }
}

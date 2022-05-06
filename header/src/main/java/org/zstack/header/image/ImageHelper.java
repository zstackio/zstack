package org.zstack.header.image;

import java.util.Objects;

public class ImageHelper {
    public static void setImageVirtio(ImageInventory imageInventory) {
        if (imageInventory.getVirtio() != null) {
            return;
        }
        if (Objects.equals(imageInventory.getPlatform(), ImagePlatform.Windows.toString())) {
            imageInventory.setVirtio(false);
            imageInventory.setGuestOsType("Windows");
        } else if (Objects.equals(imageInventory.getPlatform(), ImagePlatform.WindowsVirtio.toString())) {
            imageInventory.setPlatform(ImagePlatform.Windows.toString());
            imageInventory.setVirtio(true);
            imageInventory.setGuestOsType("Windows");
        } else if (Objects.equals(imageInventory.getPlatform(), ImagePlatform.Linux.toString())) {
            imageInventory.setVirtio(true);
            imageInventory.setGuestOsType("Linux");
        } else if (Objects.equals(imageInventory.getPlatform(), ImagePlatform.Other.toString())) {
            imageInventory.setVirtio(false);
            imageInventory.setGuestOsType("Other");
        } else if (Objects.equals(imageInventory.getPlatform(), ImagePlatform.Paravirtualization.toString())) {
            imageInventory.setPlatform(ImagePlatform.Other.toString());
            imageInventory.setVirtio(true);
            imageInventory.setGuestOsType("Other");
        }
    }
}

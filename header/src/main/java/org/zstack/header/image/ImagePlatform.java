package org.zstack.header.image;

/**
 */
public enum ImagePlatform {
    Linux(true),
    Windows(false),
    WindowsVirtio(true),
    Other(false),
    Paravirtualization(true);

    private boolean paraVirtualization;

    private ImagePlatform(boolean para) {
        paraVirtualization = para;
    }

    public boolean isParaVirtualization() {
        return paraVirtualization;
    }
}

package com.aleksandar.threedforgemarket.model.enums.product;

import lombok.Getter;

@Getter
public enum ProductCategory {
    ACCESSORY("Accessory"),
    DECORATION("Decoration"),
    TOY("Toy"),
    FIGURE("Figure");

    private final String displayName;

    ProductCategory(String displayName) {
        this.displayName = displayName;
    }

}
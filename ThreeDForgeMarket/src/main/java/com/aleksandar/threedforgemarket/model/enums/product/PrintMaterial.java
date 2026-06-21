package com.aleksandar.threedforgemarket.model.enums.product;

import lombok.Getter;

@Getter
public enum PrintMaterial {
    PLA("PLA"),
    PETG("PETG"),
    ABS("ABS"),
    TPU("TPU");

    private final String displayName;

    PrintMaterial(String displayName) {
        this.displayName = displayName;
    }
}

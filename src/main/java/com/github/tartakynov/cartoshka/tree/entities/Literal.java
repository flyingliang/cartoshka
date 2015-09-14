package com.github.tartakynov.cartoshka.tree.entities;

public class Literal extends Expression {
    private final Number value;

    public Literal(Number value) {
        this.value = value;
    }
}
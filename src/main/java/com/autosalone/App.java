package com.autosalone;

import jakarta.persistence.Persistence;

public class App {
    public static void main(String[] args) {
        Persistence.createEntityManagerFactory("autosalonePU");
        System.out.println("Fatto!");
    }
}

package org.example.main;

public class Main {
    public static void main(String[] args) throws Exception{
        ExecutionFlow.collectData("BOOKKEEPER");
        ExecutionFlow.collectData("STORM");

    }
}
package ru.ostap;

public class Main {
    public static void main(String[] args) throws Exception {
        DIContainer container = new DIContainer();
        container.scan("ru.ostap");

        container.wireDependencies();

        UserService userService = container.getInstance(UserService.class);
        userService.saveData("username");
    }
}

package ru.ostap;
@Component
public class UserRepository implements Repository{
    @Override
    public void save(String username) {
        System.out.println("Saving user: " + username);
    }
}
